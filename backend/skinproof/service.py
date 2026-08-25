from __future__ import annotations

import base64
import json
import uuid
from datetime import datetime, timezone
from typing import Any

from .attribution import AttributionEngine, iso, parse_time
from .capture import merge_quality
from .catalog import explain, parse_ingredients
from .config import Settings
from .db import Database, json_dumps
from .insights import GroundedInsightService, InsightService
from .metrics import MetricResult, analyze
from .photos import MemoryPhotoStore, PhotoStore
from .safety import triage


def now_iso() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def new_id() -> str:
    return str(uuid.uuid4())


def row_dict(row) -> dict | None:
    return None if row is None else dict(row)


class SkinProofService:
    def __init__(self, db: Database, settings: Settings | None = None, photos: PhotoStore | None = None, insights: InsightService | None = None) -> None:
        self.db = db
        self.settings = settings or Settings.from_env()
        self.photos = photos or MemoryPhotoStore()
        self.insights = insights or GroundedInsightService()
        self.attribution = AttributionEngine(db)

    def create_user(self, skin_type: str | None = None) -> dict:
        user_id = new_id()
        self.db.execute("INSERT INTO users (id, skin_type) VALUES (?, ?)", (user_id, skin_type))
        return row_dict(self.db.fetchone("SELECT * FROM users WHERE id = ?", (user_id,)))

    def grant_consent(self, user_id: str, facial_data: bool, policy_version: str | None = None) -> dict:
        self.require_user(user_id)
        state = "active" if facial_data else "declined"
        self.db.execute("UPDATE users SET consent_state = ? WHERE id = ?", (state, user_id))
        self.db.execute(
            "INSERT INTO consent_events (user_id, consent_type, granted, policy_version) VALUES (?, 'facial_data', ?, ?)",
            (user_id, int(facial_data), policy_version or self.settings.policy_version),
        )
        return row_dict(self.db.fetchone("SELECT * FROM users WHERE id = ?", (user_id,)))

    def require_user(self, user_id: str) -> dict:
        user = row_dict(self.db.fetchone("SELECT * FROM users WHERE id = ? AND deleted_at IS NULL", (user_id,)))
        if not user:
            raise ValueError("user not found")
        return user

    def require_consent(self, user_id: str) -> dict:
        user = self.require_user(user_id)
        if user["consent_state"] != "active":
            raise PermissionError("explicit facial-data consent is required before using photo capture")
        return user

    def create_product(self, name: str, barcode: str | None = None, category: str = "other", ingredients=None, stabilization_days: int = 14) -> dict:
        if not name.strip():
            raise ValueError("product name is required")
        if stabilization_days < 0 or stabilization_days > 180:
            raise ValueError("stabilization_days must be between 0 and 180")
        product_id = new_id()
        try:
            self.db.execute(
                "INSERT INTO products (id, barcode, name, category, ingredients_json, stabilization_days) VALUES (?, ?, ?, ?, ?, ?)",
                (product_id, barcode, name.strip(), category.strip() or "other", json_dumps(parse_ingredients(ingredients)), stabilization_days),
            )
        except Exception as exc:
            if "UNIQUE" in str(exc).upper():
                raise ValueError("barcode already exists") from exc
            raise
        return row_dict(self.db.fetchone("SELECT * FROM products WHERE id = ?", (product_id,)))

    def search_products(self, query: str) -> list[dict]:
        pattern = f"%{query.strip()}%"
        return [row_dict(row) for row in self.db.fetchall("SELECT * FROM products WHERE name LIKE ? OR barcode LIKE ? ORDER BY name LIMIT 30", (pattern, pattern))]

    def add_routine_event(self, user_id: str, product_id: str, action: str, timestamp: str | None = None, slot: str = "unspecified", dose: str | None = None, frequency: str | None = None, notes: str | None = None) -> dict:
        self.require_user(user_id)
        if not self.db.fetchone("SELECT id FROM products WHERE id = ?", (product_id,)):
            raise ValueError("product not found")
        if action not in {"start", "stop", "change"}:
            raise ValueError("action must be start, stop, or change")
        event_time = timestamp or now_iso()
        parse_time(event_time)
        event_id = new_id()
        self.db.execute(
            "INSERT INTO routine_events (id, user_id, product_id, action, timestamp, slot, dose, frequency, notes) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (event_id, user_id, product_id, action, event_time, slot, dose, frequency, notes),
        )
        return row_dict(self.db.fetchone("SELECT * FROM routine_events WHERE id = ?", (event_id,)))

    def create_capture(self, user_id: str, image_bytes: bytes, quality_data: dict | None = None, captured_at: str | None = None, device_meta: dict | None = None, is_baseline: bool = False) -> dict:
        user = self.require_consent(user_id)
        if not image_bytes:
            raise ValueError("image is required")
        quality = merge_quality(quality_data, image_bytes)
        if not quality.accepted:
            raise ValueError(json.dumps({"message": "capture quality is below the acceptance threshold", "quality": quality.as_dict()}))
        capture_id = new_id()
        capture_time = captured_at or now_iso()
        parse_time(capture_time)
        existing = self.db.fetchone("SELECT COUNT(*) AS count FROM photo_captures WHERE user_id = ?", (user_id,))["count"]
        baseline = bool(is_baseline or existing == 0)
        raw_ref = self.photos.save(user_id, capture_id, image_bytes)
        self.db.execute(
            """INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json,
               device_meta_json, is_baseline) VALUES (?, ?, ?, ?, ?, ?, ?)""",
            (capture_id, user_id, capture_time, raw_ref, json_dumps(quality.as_dict()), json_dumps(device_meta or {}), int(baseline)),
        )
        job_id = new_id()
        self.db.execute("INSERT INTO analysis_jobs (id, capture_id) VALUES (?, ?)", (job_id, capture_id))
        self.process_analysis_job(job_id)
        capture = row_dict(self.db.fetchone("SELECT * FROM photo_captures WHERE id = ?", (capture_id,)))
        capture["capture_quality"] = json.loads(capture.pop("capture_quality_json"))
        capture["device_meta"] = json.loads(capture.pop("device_meta_json"))
        capture["analysis_job_id"] = job_id
        capture["metric"] = row_dict(self.db.fetchone("SELECT * FROM metric_snapshots WHERE photo_id = ?", (capture_id,)))
        return capture

    def process_analysis_job(self, job_id: str) -> None:
        job = self.db.fetchone("SELECT * FROM analysis_jobs WHERE id = ?", (job_id,))
        if not job:
            raise ValueError("analysis job not found")
        capture = self.db.fetchone("SELECT * FROM photo_captures WHERE id = ?", (job["capture_id"],))
        try:
            self.db.execute("UPDATE analysis_jobs SET status = 'running', started_at = ? WHERE id = ?", (now_iso(), job_id))
            quality = json.loads(capture["capture_quality_json"])
            image_bytes = self.photos.read(capture["raw_ref"])
            baseline_row = self.db.fetchone(
                """SELECT m.* FROM metric_snapshots m JOIN photo_captures c ON c.id = m.photo_id
                   WHERE m.user_id = ? AND c.is_baseline = 1 ORDER BY c.captured_at LIMIT 1""", (capture["user_id"],)
            )
            baseline = None
            if baseline_row:
                baseline = MetricResult(
                    blemish_count=baseline_row["blemish_count"], redness_score=baseline_row["redness_score"], redness_delta=baseline_row["redness_delta"],
                    darkspot_area=baseline_row["darkspot_area"], texture_score=baseline_row["texture_score"], confidence=baseline_row["confidence"],
                    noise_floors=json.loads(baseline_row["noise_floor_json"]), model_version=baseline_row["model_version"],
                )
            result = analyze(image_bytes, float(quality.get("score", 0.0)), None if capture["is_baseline"] else baseline, self.settings.model_version)
            metric_id = new_id()
            self.db.execute(
                """INSERT INTO metric_snapshots (id, photo_id, user_id, model_version, blemish_count,
                   redness_score, redness_delta, darkspot_area, texture_score, confidence, noise_floor_json)
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                (metric_id, capture["id"], capture["user_id"], result.model_version, result.blemish_count, result.redness_score,
                 result.redness_delta, result.darkspot_area, result.texture_score, result.confidence, json_dumps(result.noise_floors)),
            )
            self.db.execute("UPDATE analysis_jobs SET status = 'completed', completed_at = ? WHERE id = ?", (now_iso(), job_id))
        except Exception as exc:
            self.db.execute("UPDATE analysis_jobs SET status = 'failed', error = ?, completed_at = ? WHERE id = ?", (str(exc), now_iso(), job_id))
            raise

    def refresh_verdicts(self, user_id: str) -> list[dict]:
        results = self.attribution.evaluate_user(user_id)
        for result in results:
            text = self.insights.generate(result.as_dict())
            evidence = result.evidence
            evidence_start = evidence.get("evidence_window_start")
            evidence_end = evidence.get("evidence_window_end")
            existing = self.db.fetchone(
                """SELECT id FROM verdicts WHERE user_id = ? AND product_id = ?
                   AND label = ? AND evidence_window_start IS ? AND evidence_window_end IS ?""",
                (user_id, result.product_id, result.label, evidence_start, evidence_end),
            )
            if not existing:
                self.db.execute(
                    "INSERT INTO verdicts (id, user_id, product_id, label, evidence_window_start, evidence_window_end, generated_text, evidence_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    (new_id(), user_id, result.product_id, result.label, evidence_start, evidence_end, text, json_dumps(evidence)),
                )
        latest = self.db.fetchall(
            """SELECT v.*, p.name AS product_name FROM verdicts v JOIN products p ON p.id = v.product_id
               WHERE v.user_id = ? AND v.generated_at = (SELECT MAX(v2.generated_at) FROM verdicts v2 WHERE v2.user_id = v.user_id AND v2.product_id = v.product_id)
               ORDER BY v.generated_at DESC""", (user_id,)
        )
        return [self._decode_verdict(row) for row in latest]

    @staticmethod
    def _decode_verdict(row) -> dict:
        result = row_dict(row)
        result["evidence"] = json.loads(result.pop("evidence_json"))
        return result

    def dashboard(self, user_id: str) -> dict:
        user = self.require_user(user_id)
        verdicts = self.refresh_verdicts(user_id)
        history = self.history(user_id)
        events = [row_dict(row) for row in self.db.fetchall("SELECT e.*, p.name AS product_name FROM routine_events e JOIN products p ON p.id = e.product_id WHERE e.user_id = ? ORDER BY e.timestamp DESC", (user_id,))]
        return {"user": user, "history": history, "routine_events": events, "verdicts": verdicts, "disclaimer": "Cosmetic tracking only; SkinProof does not diagnose or treat medical conditions."}

    def history(self, user_id: str) -> list[dict]:
        self.require_user(user_id)
        rows = self.db.fetchall(
            """SELECT c.id, c.captured_at, c.is_baseline, c.capture_quality_json, m.model_version,
                      m.blemish_count, m.redness_score, m.redness_delta, m.darkspot_area,
                      m.texture_score, m.confidence, m.noise_floor_json
               FROM photo_captures c JOIN metric_snapshots m ON m.photo_id = c.id
               WHERE c.user_id = ? ORDER BY c.captured_at""", (user_id,)
        )
        result = []
        for row in rows:
            item = row_dict(row)
            item["capture_quality"] = json.loads(item.pop("capture_quality_json"))
            item["noise_floor"] = json.loads(item.pop("noise_floor_json"))
            result.append(item)
        return result

    def export_user(self, user_id: str) -> dict:
        user = self.require_user(user_id)
        return {
            "export_version": "1",
            "exported_at": now_iso(),
            "user": user,
            "consent_events": [row_dict(row) for row in self.db.fetchall("SELECT * FROM consent_events WHERE user_id = ? ORDER BY recorded_at", (user_id,))],
            "routine_events": [row_dict(row) for row in self.db.fetchall("SELECT * FROM routine_events WHERE user_id = ? ORDER BY timestamp", (user_id,))],
            "captures_and_metrics": self.history(user_id),
            "verdicts": [self._decode_verdict(row) for row in self.db.fetchall("SELECT v.*, p.name AS product_name FROM verdicts v JOIN products p ON p.id = v.product_id WHERE v.user_id = ? ORDER BY v.generated_at", (user_id,))],
            "note": "Photo bytes are held by the configured photo store and are not embedded in this JSON export.",
        }

    def delete_user(self, user_id: str) -> None:
        self.require_user(user_id)
        self.photos.delete_user(user_id)
        self.db.execute("DELETE FROM users WHERE id = ?", (user_id,))

    def ingredient_explainer(self, product_id: str) -> dict:
        product = self.db.fetchone("SELECT * FROM products WHERE id = ?", (product_id,))
        if not product:
            raise ValueError("product not found")
        return explain(product["name"], product["ingredients_json"])

    def triage_question(self, text: str) -> dict:
        return triage(text).as_dict()
