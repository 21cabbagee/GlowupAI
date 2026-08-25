from __future__ import annotations

import statistics
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from .db import Database


LABELS = {"keep", "likely_useful", "evidence_unclear", "investigate"}
METRICS = ("blemish_count", "redness_score", "darkspot_area", "texture_score")


def parse_time(value: str) -> datetime:
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)


def iso(value: datetime) -> str:
    return value.astimezone(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


@dataclass
class AttributionResult:
    product_id: str
    product_name: str
    label: str
    evidence: dict

    def as_dict(self) -> dict:
        return {"product_id": self.product_id, "product_name": self.product_name, "label": self.label, **self.evidence}


class AttributionEngine:
    def __init__(self, db: Database) -> None:
        self.db = db

    def evaluate_user(self, user_id: str) -> list[AttributionResult]:
        products = self.db.fetchall(
            """SELECT DISTINCT p.id, p.name, p.stabilization_days FROM products p
               JOIN routine_events e ON e.product_id = p.id WHERE e.user_id = ?""", (user_id,)
        )
        return [self.evaluate_product(user_id, row["id"]) for row in products]

    def evaluate_product(self, user_id: str, product_id: str) -> AttributionResult:
        product = self.db.fetchone("SELECT * FROM products WHERE id = ?", (product_id,))
        if not product:
            raise ValueError("product not found")
        events = self.db.fetchall(
            "SELECT * FROM routine_events WHERE user_id = ? AND product_id = ? ORDER BY timestamp", (user_id, product_id)
        )
        captures = self.db.fetchall(
            """SELECT c.captured_at, m.blemish_count, m.redness_score, m.darkspot_area,
                      m.texture_score, m.confidence, m.noise_floor_json
               FROM photo_captures c JOIN metric_snapshots m ON m.photo_id = c.id
               WHERE c.user_id = ? AND c.status = 'accepted' ORDER BY c.captured_at""", (user_id,)
        )
        if not events:
            return self._result(product, "evidence_unclear", {"reason": "no routine event recorded"})
        start_event = next((event for event in reversed(events) if event["action"] in ("start", "change")), events[0])
        start = parse_time(start_event["timestamp"])
        stabilization = int(product["stabilization_days"])
        stable_at = start + timedelta(days=stabilization)
        end_event = next((event for event in events if event["action"] == "stop" and parse_time(event["timestamp"]) > start), None)
        end = parse_time(end_event["timestamp"]) if end_event else None
        typed_captures = [(parse_time(row["captured_at"]), row) for row in captures]
        before = [row for captured, row in typed_captures if captured < start and (start - captured).days <= 45]
        after = [row for captured, row in typed_captures if captured >= stable_at and (end is None or captured <= end)]
        if end:
            days_stable = max(0, (end - start).days)
        elif typed_captures:
            days_stable = max(0, (typed_captures[-1][0] - start).days)
        else:
            days_stable = 0

        other_events = self.db.fetchall(
            """SELECT e.timestamp, p.name FROM routine_events e JOIN products p ON p.id = e.product_id
               WHERE e.user_id = ? AND e.product_id <> ? AND e.action IN ('start','change')""", (user_id, product_id)
        )
        confounded_by = [row["name"] for row in other_events if start <= parse_time(row["timestamp"]) <= stable_at]
        days_to_wait = max(0, (stable_at - (typed_captures[-1][0] if typed_captures else start)).days)
        base_evidence = {
            "product_id": product_id,
            "product_name": product["name"],
            "start_at": iso(start),
            "stable_at": iso(stable_at),
            "days_stable": days_stable,
            "stabilization_days": stabilization,
            "n_before": len(before),
            "n_after": len(after),
            "days_to_wait": days_to_wait,
            "confounded_by": confounded_by,
        }
        if confounded_by:
            return self._result(product, "evidence_unclear", {**base_evidence, "reason": "another routine variable changed during the stabilization window"})
        if not after:
            return self._result(product, "evidence_unclear", {**base_evidence, "reason": "not enough post-stabilization captures"})
        if not before:
            return self._result(product, "evidence_unclear", {**base_evidence, "reason": "no comparable pre-product capture"})

        differences: dict[str, float] = {}
        normalized: dict[str, float] = {}
        confidence = statistics.mean(float(row["confidence"]) for row in before + after)
        for metric in METRICS:
            before_value = statistics.median(float(row[metric]) for row in before)
            after_value = statistics.median(float(row[metric]) for row in after)
            difference = after_value - before_value
            # Noise floors are model-versioned with each snapshot. Use the
            # post-event snapshot's floor for the paired comparison.
            floor = float(json_load(after[0]["noise_floor_json"]).get(metric, 0.0))
            differences[metric] = round(difference, 5)
            normalized[metric] = round(-difference / floor, 3) if floor else 0.0
        improvements = {metric: value for metric, value in normalized.items() if value >= 1.5}
        worsening = {metric: value for metric, value in normalized.items() if value <= -1.5}
        composite = sum(normalized.values()) / len(normalized)
        confidence *= min(1.0, len(before) / 3) * min(1.0, len(after) / 3)
        evidence = {
            **base_evidence,
            "differences": differences,
            "noise_normalized_effects": normalized,
            "improvements": improvements,
            "worsening": worsening,
            "composite_effect": round(composite, 3),
            "confidence": round(confidence, 3),
            "evidence_window_start": iso(min(parse_time(row["captured_at"]) for row in captures)),
            "evidence_window_end": iso(max(parse_time(row["captured_at"]) for row in captures)),
        }
        if worsening and not improvements:
            label = "investigate"
            evidence["worst_metric"] = min(worsening, key=worsening.get)
        elif improvements and composite >= 1.5 and confidence >= 0.55:
            label = "likely_useful"
            evidence["best_improvement_metric"] = max(improvements, key=improvements.get)
        elif confidence >= 0.55:
            label = "keep"
            evidence["reason"] = "all observed movement remains within the capture noise floor"
        else:
            label = "evidence_unclear"
            evidence["reason"] = "capture confidence or sample size is too low for a reliable comparison"
        return self._result(product, label, evidence)

    @staticmethod
    def _result(product, label: str, evidence: dict) -> AttributionResult:
        return AttributionResult(product_id=product["id"], product_name=product["name"], label=label, evidence=evidence)


def json_load(value: str) -> dict:
    import json
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return {}
