from __future__ import annotations

import json
import uuid
from typing import Any, Callable, Dict, List, Optional

from .safety import triage
from .service import now_iso, row_dict

PREMIUM_FEATURES = {
    "experiments",
    "ingredient_analysis",
    "long_history",
    "qna",
    "discover",
    "root_cause",
    "budget_optimizer",
    "derm_export",
    "product_prediction",
}


def uid() -> str:
    return str(uuid.uuid4())


def dump(value: Any) -> str:
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


def load(value: Any, default: Any = None) -> Any:
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return default if default is not None else {}


class GuidanceService:
    """Q&A, experiments, shelf scan, and reports."""

    def __init__(self, db: Any, parent_service: Any, insights: Any, vision: Any, jobs: Any) -> None:
        self.db = db
        self.parent = parent_service
        self.insights = insights
        self.vision = vision
        self.jobs = jobs

    # -- experiments (Premium) ----------------------------------------------

    def create_experiment(
        self,
        user_id: str,
        name: str,
        hypothesis: Optional[str],
        product_id: str,
        primary_metric: str = "redness_score",
        target_days: int = 14,
        require_premium_fn: Optional[Callable] = None,
        require_consent_fn: Optional[Callable] = None,
        audit_fn: Optional[Callable] = None,
    ) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Experiments")
        if require_consent_fn:
            require_consent_fn(user_id)
        if primary_metric not in {
            "blemish_count",
            "redness_score",
            "darkspot_area",
            "texture_score",
        }:
            raise ValueError("unsupported primary metric")
        if target_days < 1 or target_days > 180:
            raise ValueError("target_days must be between 1 and 180")
        if not self.db.fetchone("SELECT id FROM products WHERE id=?", (product_id,)):
            raise ValueError("product not found")
        experiment_id = uid()
        start = now_iso()
        self.db.execute(
            "INSERT INTO experiments (id,user_id,name,hypothesis,primary_metric,status,start_at,target_days) VALUES (?,?,?,?,?,?,?,?)",
            (
                experiment_id,
                user_id,
                name.strip(),
                hypothesis,
                primary_metric,
                "running",
                start,
                target_days,
            ),
        )
        self.db.execute(
            "INSERT INTO experiment_products (experiment_id,product_id,role) VALUES (?,?, 'test')",
            (experiment_id, product_id),
        )
        if audit_fn:
            audit_fn(
                "experiment_started",
                "experiment",
                experiment_id,
                user_id,
                {"product_id": product_id},
            )
        return self.experiment(experiment_id, user_id)

    def _early_stop(self, user_id: str, experiment: Dict[str, Any]) -> Dict[str, Any]:
        """Tell the user when evidence is already conclusive, before target_days."""

        if experiment["status"] != "running" or not experiment["products"]:
            return {
                "conclusive": False,
                "message": "Not applicable to a non-running experiment.",
            }
        product_id = experiment["products"][0]["product_id"]
        try:
            result = self.parent.attribution.evaluate_product(user_id, product_id)
        except ValueError:
            return {"conclusive": False, "message": "No product evidence yet."}
        days_stable = result.evidence.get("days_stable", 0)
        target_days = experiment["target_days"]
        if result.label == "likely_useful" and days_stable < target_days:
            return {
                "conclusive": True,
                "recommended_status": "completed",
                "message": f"Evidence is already conclusive after {days_stable} of {target_days} planned days: {result.product_name} is likely useful. You can stop the experiment early.",
            }
        if result.label == "investigate" and days_stable < target_days:
            return {
                "conclusive": True,
                "recommended_status": "investigate",
                "message": f"Evidence already shows a worsening signal after {days_stable} of {target_days} planned days. Stop and investigate before waiting the full window.",
            }
        return {
            "conclusive": False,
            "message": "Not yet conclusive; keep the routine stable until the target day count.",
        }

    def experiment(self, experiment_id: str, user_id: str, history_fn: Optional[Callable] = None) -> Dict[str, Any]:
        row = self.db.fetchone(
            "SELECT * FROM experiments WHERE id=? AND user_id=?",
            (experiment_id, user_id),
        )
        if not row:
            raise ValueError("experiment not found")
        result = row_dict(row)
        result["products"] = [
            row_dict(item)
            for item in self.db.fetchall(
                "SELECT ep.*, p.name, p.category FROM experiment_products ep JOIN products p ON p.id=ep.product_id WHERE ep.experiment_id=?",
                (experiment_id,),
            )
        ]
        result["events"] = [
            row_dict(item)
            for item in self.db.fetchall(
                "SELECT e.*, p.name AS product_name FROM experiment_events x JOIN routine_events e ON e.id=x.routine_event_id JOIN products p ON p.id=e.product_id WHERE x.experiment_id=? ORDER BY e.timestamp",
                (experiment_id,),
            )
        ]
        result["captures"] = history_fn(user_id) if history_fn else []
        result["early_stop"] = self._early_stop(user_id, result)
        return result

    def experiments(self, user_id: str, require_premium_fn: Optional[Callable] = None) -> List[Dict[str, Any]]:
        if require_premium_fn:
            require_premium_fn(user_id, "Experiments")
        return [
            self.experiment(row["id"], user_id)
            for row in self.db.fetchall(
                "SELECT id FROM experiments WHERE user_id=? ORDER BY created_at DESC",
                (user_id,),
            )
        ]

    def set_experiment_status(
        self, user_id: str, experiment_id: str, status: str, require_premium_fn: Optional[Callable] = None
    ) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Experiments")
        if status not in {"planned", "running", "paused", "completed", "cancelled"}:
            raise ValueError("invalid experiment status")
        self.db.execute(
            "UPDATE experiments SET status=?, end_at=? WHERE id=? AND user_id=?",
            (
                status,
                now_iso() if status in {"completed", "cancelled"} else None,
                experiment_id,
                user_id,
            ),
        )
        return self.experiment(experiment_id, user_id)

    # -- grounded Q&A (Premium) ------------------------------------------------

    def _qna_evidence(
        self, user_id: str, history: List[Dict[str, Any]], events: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        verdicts = [
            self.parent._decode_verdict(row)
            for row in self.db.fetchall(
                """SELECT v.*,p.name AS product_name FROM verdicts v
                   JOIN products p ON p.id=v.product_id WHERE v.user_id=?
                   ORDER BY v.generated_at DESC LIMIT 20""",
                (user_id,),
            )
        ]
        return {
            "capture_count": len(history),
            "captures": history[-8:],
            "routine_events": events[-20:],
            "verdicts": verdicts,
        }

    def _deterministic_qna_answer(
        self, user_id: str, question: str, history: List[Dict[str, Any]], events: List[Dict[str, Any]], refresh_verdicts_fn: Optional[Callable] = None, root_cause_fn: Optional[Callable] = None
    ) -> str:
        lowered = question.casefold()
        if "ingredient" in lowered or "inci" in lowered:
            return "Ingredient explanations are grounded in the maintained catalog. Open a product's ingredient analysis to see reviewed purposes and cautions; unknown entries are explicitly marked unknown."
        if "redness" in lowered and len(history) >= 2:
            delta = float(history[-1]["redness_score"]) - float(
                history[0]["redness_score"]
            )
            return f"Your measured redness changed by {delta:+.4f} from the first available capture to the latest. That is a measurement delta, not a diagnosis. I found {len(events)} routine events to compare against it."
        if any(word in lowered for word in ("why", "pattern", "correlat")):
            correlations = root_cause_fn(user_id) if root_cause_fn else []
            if correlations:
                top = correlations[0]
                return f"The strongest pattern in your data: {top['message']}"
            return "I did not find a repeatable pattern between your logged context events and your measurements yet. Log more sleep/travel/weather events or capture more consistently to sharpen this."
        if any(word in lowered for word in ("work", "useful", "product", "keep")):
            verdicts = refresh_verdicts_fn(user_id) if refresh_verdicts_fn else []
            return (
                "Your current evidence states are: "
                + (
                    ", ".join(
                        f"{item['product_name']}: {item['label']}" for item in verdicts
                    )
                    if verdicts
                    else "not enough clean post-stabilization evidence yet"
                )
                + "."
            )
        return f"I can ground this in {len(history)} captures and {len(events)} routine events. Ask about redness, a product, ingredients, cadence, patterns, or a specific date so I can cite the relevant evidence."

    def ask(self, user_id: str, question: str, thread_id: Optional[str] = None, require_premium_fn: Optional[Callable] = None, history_fn: Optional[Callable] = None, record_engagement_fn: Optional[Callable] = None) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Data Q&A")
        scope = triage(question)
        if thread_id:
            thread = self.db.fetchone(
                "SELECT * FROM qna_threads WHERE id=? AND user_id=?",
                (thread_id, user_id),
            )
            if not thread:
                raise ValueError("thread not found")
        else:
            thread_id = uid()
            self.db.execute(
                "INSERT INTO qna_threads (id,user_id,title) VALUES (?,?,?)",
                (thread_id, user_id, question[:120]),
            )
        self.db.execute(
            "INSERT INTO qna_messages (id,thread_id,role,content,scope) VALUES (?,?,?,?,?)",
            (uid(), thread_id, "user", question, scope.scope),
        )
        citations = []
        if scope.scope == "dermatology_review":
            answer = scope.message
        else:
            history = history_fn(user_id) if history_fn else []
            events = [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT e.timestamp,p.name,e.action FROM routine_events e JOIN products p ON p.id=e.product_id WHERE e.user_id=? ORDER BY e.timestamp",
                    (user_id,),
                )
            ]
            for item in history[-3:]:
                citations.append(
                    {"type": "capture", "date": item["captured_at"], "id": item["id"]}
                )
            provider_answer = getattr(self.insights, "answer", None)
            answer = (
                provider_answer(question, self._qna_evidence(user_id, history, events))
                if callable(provider_answer)
                else None
            )
            if not answer:
                answer = self._deterministic_qna_answer(
                    user_id, question, history, events
                )
        self.db.execute(
            "INSERT INTO qna_messages (id,thread_id,role,content,citations_json,scope) VALUES (?,?,?,?,?,?)",
            (uid(), thread_id, "assistant", answer, dump(citations), scope.scope),
        )
        if record_engagement_fn:
            record_engagement_fn(user_id, "qna_answered", thread_id)
        return {
            "thread_id": thread_id,
            "answer": answer,
            "scope": scope.scope,
            "citations": citations,
        }

    def qna_history(self, user_id: str, require_premium_fn: Optional[Callable] = None) -> List[Dict[str, Any]]:
        if require_premium_fn:
            require_premium_fn(user_id, "Data Q&A")
        return [
            row_dict(row) | {"citations": load(row["citations_json"], [])}
            for row in self.db.fetchall(
                "SELECT m.*,t.title FROM qna_messages m JOIN qna_threads t ON t.id=m.thread_id WHERE t.user_id=? ORDER BY m.created_at",
                (user_id,),
            )
        ]

    # -- shelf scan → auto-logging (free) -------------------------------------

    def _run_shelf_scan(self, image_bytes: bytes) -> Dict[str, Any]:
        if not self.vision:
            return {
                "candidates": [],
                "message": "AI shelf-scan is not configured; add products manually.",
            }
        candidates = self.vision.extract_products(image_bytes)
        return {
            "candidates": candidates,
            "message": (
                f"Found {len(candidates)} candidate product(s). Review and confirm before they are added."
                if candidates
                else "No products were recognized in this photo. Try better lighting or a closer shot of the labels."
            ),
        }

    def scan_shelf(self, user_id: str, image_bytes: bytes, record_engagement_fn: Optional[Callable] = None) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        if not image_bytes:
            raise ValueError("image is required")
        job_id = self.jobs.submit(
            "shelf_scan", self._run_shelf_scan, image_bytes, user_id=user_id
        )
        if record_engagement_fn:
            record_engagement_fn(user_id, "shelf_scan_submitted", job_id)
        return {"job_id": job_id, "status": "queued"}

    def shelf_scan_status(self, user_id: str, job_id: str) -> Dict[str, Any]:
        self.parent.require_user(user_id)
        job = self.jobs.get(job_id, user_id=user_id)
        if not job:
            raise ValueError("shelf-scan job not found")
        return job

    def confirm_shelf_scan(
        self, user_id: str, job_id: str, selections: List[Dict[str, Any]], record_engagement_fn: Optional[Callable] = None
    ) -> List[Dict[str, Any]]:
        self.parent.require_user(user_id)
        job = self.jobs.get(job_id, user_id=user_id)
        if not job or job["status"] != "completed":
            raise ValueError("shelf-scan job is not ready")
        created = []
        for selection in selections[:20]:
            name = str(selection.get("name") or "").strip()
            if not name:
                continue
            created.append(
                self.parent.create_product(
                    name=name,
                    barcode=None,
                    category=str(selection.get("category") or "other"),
                    ingredients=selection.get("ingredients"),
                    stabilization_days=int(selection.get("stabilization_days") or 14),
                )
            )
        if record_engagement_fn:
            record_engagement_fn(
                user_id, "shelf_scan_confirmed", job_id, {"created": len(created)}
            )
        return created

    # -- dermatologist export (Premium) ---------------------------------------

    def dermatologist_report(self, user_id: str, profile_fn: Callable, history_fn: Callable, require_premium_fn: Optional[Callable] = None, refresh_verdicts_fn: Optional[Callable] = None) -> Dict[str, Any]:
        if require_premium_fn:
            require_premium_fn(user_id, "Dermatologist export")
        profile = profile_fn(user_id)
        history = history_fn(user_id)
        verdicts = refresh_verdicts_fn(user_id) if refresh_verdicts_fn else []
        model_versions = sorted(
            {item["model_version"] for item in history if item.get("model_version")}
        )
        summary_rows = "".join(
            f"<tr><td>{v['product_name']}</td><td>{v['label']}</td><td>{v['evidence'].get('days_stable', 0)}</td><td>{v['evidence'].get('confidence', '')}</td></tr>"
            for v in verdicts
        )
        printable_html = (
            "<h1>GlowUpAI cosmetic measurement summary</h1>"
            f"<p>Generated {now_iso()} for a consenting user. Cosmetic tracking only; not a diagnosis.</p>"
            f"<p>{len(history)} captures across model version(s): {', '.join(model_versions) or 'n/a'}.</p>"
            "<table border=1 cellpadding=4><tr><th>Product</th><th>Evidence label</th><th>Days stable</th><th>Confidence</th></tr>"
            f"{summary_rows}</table>"
        )
        return {
            "generated_at": now_iso(),
            "capture_count": len(history),
            "model_versions": model_versions,
            "verdicts": verdicts,
            "printable_html": printable_html,
            "disclaimer": "This is a measurement history export for the user to optionally share, not a clinical or diagnostic document. GlowUpAI does not diagnose, treat, or rule out medical conditions.",
        }

    # -- dashboard and misc ---------------------------------------------------

    def dashboard(
        self,
        user_id: str,
        vertical: str = "skin",
        profile_fn: Optional[Callable] = None,
        history_fn: Optional[Callable] = None,
        verdicts_for_user_fn: Optional[Callable] = None,
        experiments_fn: Optional[Callable] = None,
        engagement_fn: Optional[Callable] = None,
        analytics_fn: Optional[Callable] = None,
        weekly_recap_fn: Optional[Callable] = None,
        check_ins_fn: Optional[Callable] = None,
    ) -> Dict[str, Any]:
        import logging
        logger = logging.getLogger(__name__)

        # Safely call each function with error handling
        def safe_call(fn, *args, default=None, label="unknown"):
            try:
                if fn:
                    return fn(*args)
                return default
            except Exception as e:
                logger.error(f"Dashboard {label} failed for user {user_id}: {str(e)}")
                return default

        profile = safe_call(profile_fn, user_id, default={}, label="profile")
        history = safe_call(history_fn, user_id, vertical, default=[], label="history")
        plan = profile.get("entitlement", {}).get("plan", "free")
        verdicts = safe_call(verdicts_for_user_fn, user_id, default=[], label="verdicts")
        features = {feature: int(plan == "premium") for feature in PREMIUM_FEATURES}

        try:
            features["product_verdicts_unlocked"] = int(
                self._free_unlocked_product_id(user_id) is not None or plan == "premium"
            )
        except Exception as e:
            logger.error(f"Dashboard features check failed for user {user_id}: {str(e)}")
            features["product_verdicts_unlocked"] = 0

        # Get routine events with error handling
        try:
            routine_events = [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT e.*,p.name AS product_name FROM routine_events e JOIN products p ON p.id=e.product_id WHERE e.user_id=? ORDER BY e.timestamp DESC",
                    (user_id,),
                )
            ]
        except Exception as e:
            logger.error(f"Dashboard routine_events failed for user {user_id}: {str(e)}")
            routine_events = []

        return {
            "profile": profile,
            "vertical": vertical,
            "history": history,
            "verdicts": verdicts,
            "experiments": safe_call(experiments_fn, user_id, default=[], label="experiments") if experiments_fn and plan == "premium" else [],
            "engagement": safe_call(engagement_fn, user_id, default={}, label="engagement"),
            "analytics": safe_call(analytics_fn, user_id, default={}, label="analytics"),
            "weekly_recap": safe_call(weekly_recap_fn, user_id, vertical, default={}, label="weekly_recap"),
            "check_ins": safe_call(check_ins_fn, user_id, 20, default=[], label="check_ins"),
            "routine_events": routine_events,
            "features": features,
            "disclaimer": "Cosmetic tracking only; GlowUpAI does not diagnose, treat, or rule out medical conditions.",
        }

    def _free_unlocked_product_id(self, user_id: str) -> Optional[str]:
        row = self.db.fetchone(
            "SELECT reference_id FROM engagement_events WHERE user_id=? AND event_type='free_verdict_unlocked' ORDER BY occurred_at LIMIT 1",
            (user_id,),
        )
        return row["reference_id"] if row else None

    def triage_question(self, text: str) -> Dict[str, Any]:
        return triage(text).as_dict()
