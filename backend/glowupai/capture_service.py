from __future__ import annotations

import json
import uuid
from datetime import datetime, timedelta, timezone
from typing import Any, Callable, Dict, List, Optional

from .attribution import parse_time
from .metrics import analyze
from .service import GlowupAIService, now_iso, row_dict

VERTICALS = ("skin",)
CAPTURE_PROTOCOL_VERSION = "standardized-v1"
FREE_HISTORY_DAYS = 90


def uid() -> str:
    """Generate a unique identifier string.

    Returns:
        A UUID4 string representation.
    """
    return str(uuid.uuid4())


def dump(value) -> str:
    """Serialize a Python value to a compact JSON string.

    Args:
        value: Any JSON-serializable Python object.

    Returns:
        Compact JSON string with sorted keys.
    """
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


def load(value: Any, default: Any = None) -> Any:
    """Deserialize a JSON string to a Python value.

    Args:
        value: JSON string to parse.
        default: Default value to return if parsing fails. Defaults to empty dict.

    Returns:
        Parsed Python object, or the default value if parsing fails.
    """
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return default if default is not None else {}


def as_date(value: str) -> datetime:
    """Parse an ISO timestamp string to a UTC datetime.

    Args:
        value: ISO format timestamp string.

    Returns:
        Datetime object in UTC timezone.
    """
    parsed = parse_time(value)
    return parsed.astimezone(timezone.utc)


def day(value: datetime) -> str:
    """Convert a datetime to an ISO date string.

    Args:
        value: Datetime object to convert.

    Returns:
        ISO format date string (YYYY-MM-DD) in UTC.
    """
    return value.astimezone(timezone.utc).date().isoformat()


class CaptureService:
    """Photo capture, analysis, and history management."""

    def __init__(self, db: Any, parent_service: Any, photos: Any, settings: Any) -> None:
        """Initialize the CaptureService.

        Args:
            db: Database connection instance.
            parent_service: Parent GlowupAIService instance.
            photos: Photo storage service for reading/writing image data.
            settings: Application settings with model version and configuration.
        """
        self.db = db
        self.parent = parent_service
        self.photos = photos
        self.settings = settings

    def _vertical_metrics(self, vertical: str, metric: Dict[str, Any]) -> Dict[str, Any]:
        """Extract vertical-specific metrics from full metric dictionary.

        Args:
            vertical: The appearance vertical (e.g., 'skin').
            metric: Complete metrics dictionary from analysis.

        Returns:
            Dictionary with only vertical-relevant metric keys.
        """
        return {
            key: metric.get(key)
            for key in (
                "blemish_count",
                "redness_score",
                "redness_delta",
                "darkspot_area",
                "texture_score",
            )
        }

    def _get_baseline_metrics(self, user_id: str, vertical: str = "skin") -> Optional[Dict[str, Any]]:
        """Get the baseline capture metrics for calculating relative changes."""
        row = self.db.fetchone(
            """SELECT m.blemish_count, m.redness_score, m.darkspot_area, m.texture_score
               FROM photo_captures c
               JOIN metric_snapshots m ON m.id=(SELECT m2.id FROM metric_snapshots m2
                                                 WHERE m2.photo_id=c.id
                                                 ORDER BY m2.created_at DESC LIMIT 1)
               LEFT JOIN appearance_captures a ON a.photo_id=c.id AND a.vertical=?
               WHERE c.user_id=? AND c.is_baseline=1
               ORDER BY c.captured_at
               LIMIT 1""",
            (vertical, user_id),
        )
        if not row:
            return None
        return row_dict(row)

    def _calculate_relative_change(self, current_value: Optional[float], baseline_value: Optional[float]) -> Optional[float]:
        """Calculate percentage change from baseline, handling division by zero."""
        if current_value is None or baseline_value is None:
            return None
        if baseline_value == 0:
            # Handle division by zero: if current is also 0, no change; otherwise infinite change
            if current_value == 0:
                return 0.0
            # Return None for infinite change cases
            return None
        return round(((current_value - baseline_value) / baseline_value) * 100, 2)

    def _add_baseline_comparison(self, user_id: str, metric: Dict[str, Any], vertical: str = "skin") -> Dict[str, Any]:
        """Add baseline comparison data to metrics."""
        baseline = self._get_baseline_metrics(user_id, vertical)
        if not baseline:
            return {
                "has_baseline": False,
                "redness_change_pct": None,
                "blemish_change_pct": None,
                "darkspot_change_pct": None,
                "texture_change_pct": None,
            }

        return {
            "has_baseline": True,
            "redness_change_pct": self._calculate_relative_change(
                metric.get("redness_score"), baseline.get("redness_score")
            ),
            "blemish_change_pct": self._calculate_relative_change(
                metric.get("blemish_count"), baseline.get("blemish_count")
            ),
            "darkspot_change_pct": self._calculate_relative_change(
                metric.get("darkspot_area"), baseline.get("darkspot_area")
            ),
            "texture_change_pct": self._calculate_relative_change(
                metric.get("texture_score"), baseline.get("texture_score")
            ),
        }

    @staticmethod
    def _measurement_explanation(item: Dict[str, Any]) -> Dict[str, Any]:
        """Generate user-friendly explanation of measurement confidence and quality.

        Provides contextual messaging based on confidence level and capture quality
        to help users understand measurement reliability.

        Args:
            item: Dictionary with 'confidence' and 'capture_quality' keys.

        Returns:
            Dictionary with confidence_label, confidence_message, comparison_ready flag,
            quality_score, quality_issues, and noise floor messaging.
        """
        confidence = float(item.get("confidence") or 0)
        quality = item.get("capture_quality") or {}
        quality_score = float(quality.get("score") or 0)
        failed = quality.get("failed_checks") or []
        if confidence >= 0.75 and quality_score >= 0.75:
            label = "strong comparison frame"
            message = (
                "This frame is well suited for comparing larger changes over time."
            )
        elif confidence >= 0.50 and quality_score >= 0.65:
            label = "directional frame"
            message = "Useful for a broad trend, but small changes may still be capture noise."
        else:
            label = "low-confidence frame"
            message = "Treat this as context rather than proof; use the next guided frame for a stronger comparison."
        return {
            "confidence_label": label,
            "confidence_message": message,
            "comparison_ready": confidence >= 0.50 and quality_score >= 0.65,
            "quality_score": round(quality_score, 3),
            "quality_issues": failed,
            "capture_protocol": CAPTURE_PROTOCOL_VERSION,
            "noise_floor_message": "A change smaller than the stated noise floor may not be a real appearance change.",
        }

    def create_capture(
        self,
        user_id: str,
        image_bytes: bytes,
        quality_data: Optional[Dict[str, Any]] = None,
        captured_at: Optional[str] = None,
        device_meta: Optional[Dict[str, Any]] = None,
        is_baseline: bool = False,
        vertical: str = "skin",
        experiment_id: Optional[str] = None,
        record_engagement_fn: Optional[Callable] = None,
    ) -> Dict[str, Any]:
        """Create a new photo capture with analysis and metrics.

        Processes a new photo capture, runs ML analysis, stores metrics, and optionally
        marks it as a baseline capture. Includes baseline comparison data if applicable.

        Args:
            user_id: ID of the user creating the capture.
            image_bytes: Raw image bytes to analyze.
            quality_data: Pre-computed quality assessment data.
            captured_at: ISO timestamp when photo was taken. Defaults to now.
            device_meta: Device metadata (camera model, OS, etc).
            is_baseline: Whether this is the user's baseline capture.
            vertical: Appearance vertical ('skin' currently).
            experiment_id: Optional experiment this capture is associated with.
            record_engagement_fn: Optional callback to record engagement event.

        Returns:
            Dictionary with capture data, metrics, measurement explanation, and
            baseline comparison data.

        Raises:
            ValueError: If vertical is invalid or experiment_id not found.
        """
        if vertical not in VERTICALS:
            raise ValueError("vertical must be skin")
        if experiment_id and not self.db.fetchone(
            "SELECT id FROM experiments WHERE id=? AND user_id=?",
            (experiment_id, user_id),
        ):
            raise ValueError("experiment not found")
        # Call base service method directly to avoid infinite recursion
        result = GlowupAIService.create_capture(self.parent, 
            user_id, image_bytes, quality_data, captured_at, device_meta, is_baseline
        )
        metric = result["metric"] or {}
        appearance_id = uid()
        self.db.execute(
            "INSERT INTO appearance_captures (id,user_id,photo_id,vertical,metrics_json,model_version,confidence) VALUES (?,?,?,?,?,?,?)",
            (
                appearance_id,
                user_id,
                result["id"],
                vertical,
                dump(self._vertical_metrics(vertical, metric)),
                metric.get("model_version", self.settings.model_version),
                metric.get("confidence", 0),
            ),
        )
        if result["is_baseline"]:
            self.db.execute(
                "UPDATE users SET baseline_date=? WHERE id=?",
                (result["captured_at"], user_id),
            )
            self.db.execute(
                "UPDATE appearance_profiles SET baseline_capture_id=? WHERE user_id=? AND vertical=?",
                (result["id"], user_id, vertical),
            )
        if experiment_id:
            self.db.execute(
                "INSERT INTO engagement_events (id,user_id,event_type,reference_id) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",
                (uid(), user_id, "experiment_capture", experiment_id),
            )
        if record_engagement_fn:
            record_engagement_fn(user_id, "capture_completed", result["id"])
        result["vertical"] = vertical
        result["capture"] = {"id": result["id"], "captured_at": result["captured_at"]}
        result["appearance_metrics"] = self._vertical_metrics(vertical, metric)
        result["measurement"] = self._measurement_explanation(
            {
                "confidence": metric.get("confidence", 0),
                "capture_quality": result.get("capture_quality", {}),
            }
        )
        result["capture_protocol"] = CAPTURE_PROTOCOL_VERSION
        # Add baseline comparison if this is not the baseline itself
        if not is_baseline:
            result["baseline_comparison"] = self._add_baseline_comparison(user_id, metric, vertical)
        else:
            result["baseline_comparison"] = {
                "has_baseline": False,
                "redness_change_pct": None,
                "blemish_change_pct": None,
                "darkspot_change_pct": None,
                "texture_change_pct": None,
            }
        return result

    def history(self, user_id: str, vertical: str = "skin", is_premium: bool = False) -> List[Dict[str, Any]]:
        """Retrieve user's capture history with metrics and baseline comparisons.

        Returns all captures for a user with complete metrics, quality data, and
        baseline comparison. Free users see only last 90 days of history.

        Args:
            user_id: ID of the user.
            vertical: Appearance vertical to retrieve captures for.
            is_premium: Whether user has premium access (full history).

        Returns:
            List of capture dictionaries with metrics, quality, and baseline comparison.

        Raises:
            ValueError: If vertical is invalid.
        """
        self.parent.require_user(user_id)
        if vertical not in VERTICALS:
            raise ValueError("invalid vertical")
        rows = self.db.fetchall(
            """SELECT c.id,c.captured_at,c.is_baseline,c.capture_quality_json,m.model_version,m.blemish_count,m.redness_score,m.redness_delta,m.darkspot_area,m.texture_score,m.confidence,m.noise_floor_json,a.metrics_json FROM photo_captures c JOIN metric_snapshots m ON m.id=(SELECT m2.id FROM metric_snapshots m2 WHERE m2.photo_id=c.id ORDER BY m2.created_at DESC LIMIT 1) LEFT JOIN appearance_captures a ON a.photo_id=c.id AND a.vertical=? WHERE c.user_id=? ORDER BY c.captured_at""",
            (vertical, user_id),
        )
        output = []
        for row in rows:
            item = row_dict(row)
            item["capture_quality"] = load(item.pop("capture_quality_json"))
            item["noise_floor"] = load(item.pop("noise_floor_json"))
            item["appearance_metrics"] = load(item.pop("metrics_json"), {})
            item.update(self._measurement_explanation(item))
            # Add baseline comparison for non-baseline captures
            if not item.get("is_baseline"):
                item["baseline_comparison"] = self._add_baseline_comparison(
                    user_id, item, vertical
                )
            else:
                item["baseline_comparison"] = {
                    "has_baseline": False,
                    "redness_change_pct": None,
                    "blemish_change_pct": None,
                    "darkspot_change_pct": None,
                    "texture_change_pct": None,
                }
            output.append(item)
        if not is_premium:
            cutoff = datetime.now(timezone.utc) - timedelta(days=FREE_HISTORY_DAYS)
            output = [item for item in output if as_date(item["captured_at"]) >= cutoff]
        return output

    def capture_guide(self, user_id: str, vertical: str = "skin", history_fn: Optional[Callable] = None) -> Dict[str, Any]:
        """Provide guidance on when user should take their next capture.

        Calculates optimal capture timing based on user's history. Recommends
        captures every 3-7 days for comparable measurements.

        Args:
            user_id: ID of the user.
            vertical: Appearance vertical.
            history_fn: Optional function to retrieve history (for testing).

        Returns:
            Dictionary with state ('baseline_needed', 'scheduled', 'due', 'overdue'),
            last_capture timestamp, next_window timing, and user-facing message.
        """
        history = history_fn(user_id, vertical) if history_fn else self.history(user_id, vertical)
        captures = [as_date(item["captured_at"]) for item in history]
        latest = max(captures) if captures else None
        now = datetime.now(timezone.utc)
        if not latest:
            return {
                "vertical": vertical,
                "state": "baseline_needed",
                "message": "Take a baseline capture to start your own history.",
                "next_window_start": now.isoformat().replace("+00:00", "Z"),
                "next_window_end": (now + timedelta(days=1))
                .isoformat()
                .replace("+00:00", "Z"),
            }
        start, end = latest + timedelta(days=3), latest + timedelta(days=7)
        state = (
            "due"
            if now >= start and now <= end
            else ("overdue" if now > end else "scheduled")
        )
        return {
            "vertical": vertical,
            "state": state,
            "last_capture": latest.isoformat().replace("+00:00", "Z"),
            "next_window_start": start.isoformat().replace("+00:00", "Z"),
            "next_window_end": end.isoformat().replace("+00:00", "Z"),
            "message": (
                "Capture now for a comparable measurement."
                if state in {"due", "overdue"}
                else "Your next guided window opens soon."
            ),
        }

    def add_measurement_feedback(
        self, user_id: str, capture_id: str, agreement: str, note: Optional[str] = None, record_engagement_fn: Optional[Callable] = None
    ) -> Dict[str, Any]:
        """Record user feedback on measurement accuracy for quality improvement.

        Allows users to rate how well the measurements match their perception,
        providing valuable product quality signals.

        Args:
            user_id: ID of the user providing feedback.
            capture_id: ID of the capture being rated.
            agreement: User's assessment ('fair', 'uncertain', or 'off').
            note: Optional free-text feedback from user.
            record_engagement_fn: Optional callback to record engagement event.

        Returns:
            Feedback record dictionary.

        Raises:
            ValueError: If agreement value invalid or capture not found.
        """
        self.parent.require_user(user_id)
        if agreement not in {"fair", "uncertain", "off"}:
            raise ValueError("agreement must be fair, uncertain, or off")
        if not self.db.fetchone(
            "SELECT id FROM photo_captures WHERE id=? AND user_id=?",
            (capture_id, user_id),
        ):
            raise ValueError("capture not found")
        feedback_id = uid()
        self.db.execute(
            """INSERT INTO measurement_feedback (id,user_id,capture_id,agreement,note)
            VALUES (?,?,?,?,?) ON CONFLICT (user_id,capture_id) DO UPDATE SET agreement=excluded.agreement,note=excluded.note,created_at=datetime('now')""",
            (
                feedback_id,
                user_id,
                capture_id,
                agreement,
                note.strip() if note else None,
            ),
        )
        if record_engagement_fn:
            record_engagement_fn(
                user_id,
                "measurement_feedback_submitted",
                capture_id,
                {"agreement": agreement},
            )
        row = self.db.fetchone(
            "SELECT * FROM measurement_feedback WHERE user_id=? AND capture_id=?",
            (user_id, capture_id),
        )
        return row_dict(row)

    def measurement_feedback_summary(self) -> Dict[str, Any]:
        """Get aggregate summary of all measurement feedback.

        Provides counts of user feedback ratings across all captures for
        monitoring product quality signals.

        Returns:
            Dictionary with counts by agreement type ('fair', 'uncertain', 'off'),
            total count, and explanatory note.
        """
        rows = self.db.fetchall(
            "SELECT agreement, COUNT(*) AS count FROM measurement_feedback GROUP BY agreement"
        )
        counts = {"fair": 0, "uncertain": 0, "off": 0}
        for row in rows:
            counts[row["agreement"]] = int(row["count"])
        return {
            "counts": counts,
            "total": sum(counts.values()),
            "note": "User-reported agreement is a product-quality signal, not a clinical validation study.",
        }

    def labels(self, user_id: str) -> List[Dict[str, Any]]:
        """Retrieve all labels created by a user for ML training and data collection.

        Args:
            user_id: ID of the user.

        Returns:
            List of label dictionaries ordered by creation time (newest first).
        """
        self.parent.require_user(user_id)
        return [
            row_dict(row)
            for row in self.db.fetchall(
                "SELECT * FROM labels WHERE user_id=? ORDER BY created_at DESC",
                (user_id,),
            )
        ]

    def add_label(
        self,
        user_id: str,
        photo_id: str,
        label_type: str,
        value: str,
        confidence: Optional[float] = None,
        notes: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Add a label to a capture for ML training and data collection.

        Allows users to label captures with ground truth data for model improvement
        and training dataset creation.

        Args:
            user_id: ID of the user creating the label.
            photo_id: ID of the capture to label.
            label_type: Type of label (e.g., 'skin_condition', 'quality').
            value: Label value.
            confidence: Optional confidence score for the label (0.0-1.0).
            notes: Optional notes about the label.

        Returns:
            Created label dictionary.

        Raises:
            ValueError: If capture not found.
        """
        self.parent.require_user(user_id)
        if not self.db.fetchone(
            "SELECT id FROM photo_captures WHERE id=? AND user_id=?",
            (photo_id, user_id),
        ):
            raise ValueError("capture not found")
        label_id = uid()
        self.db.execute(
            "INSERT INTO labels (id,user_id,photo_id,label_type,value,confidence,notes) VALUES (?,?,?,?,?,?,?)",
            (label_id, user_id, photo_id, label_type, value, confidence, notes),
        )
        return row_dict(
            self.db.fetchone("SELECT * FROM labels WHERE id=?", (label_id,))
        )

    def _run_reprocess(self, user_id: str, model_version: str) -> Dict[str, Any]:
        """Reprocess all user captures with a new model version (background task).

        Re-runs ML analysis on all accepted captures using specified model version,
        creating new metric snapshots while preserving original data.

        Args:
            user_id: ID of the user whose captures to reprocess.
            model_version: Model version identifier to use for reprocessing.

        Returns:
            Dictionary with processed_count and model_version.
        """
        captures = self.db.fetchall(
            "SELECT * FROM photo_captures WHERE user_id=? AND status='accepted' ORDER BY captured_at",
            (user_id,),
        )
        processed = 0
        for capture in captures:
            quality = load(capture["capture_quality_json"])
            result = analyze(
                self.photos.read(capture["raw_ref"]),
                float(quality.get("score", 0)),
                None,
                model_version,
            )
            self.db.execute(
                "INSERT INTO metric_snapshots (id,photo_id,user_id,model_version,blemish_count,redness_score,redness_delta,darkspot_area,texture_score,confidence,noise_floor_json) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                (
                    uid(),
                    capture["id"],
                    user_id,
                    result.model_version,
                    result.blemish_count,
                    result.redness_score,
                    result.redness_delta,
                    result.darkspot_area,
                    result.texture_score,
                    result.confidence,
                    dump(result.noise_floors),
                ),
            )
            processed += 1
        return {"processed_count": processed, "model_version": model_version}

    def reprocess(self, user_id: str, model_version: str, jobs: Any, audit_fn: Any) -> Dict[str, Any]:
        """Queue a background job to reprocess user captures with new model version.

        Args:
            user_id: ID of the user whose captures to reprocess.
            model_version: Model version identifier to use.
            jobs: Job queue service for background processing.
            audit_fn: Audit logging function.

        Returns:
            Dictionary with job_id and status ('queued').
        """
        job_id = jobs.submit(
            "reprocess", self._run_reprocess, user_id, model_version, user_id=user_id
        )
        audit_fn(
            "reprocess_queued",
            "user",
            user_id,
            user_id,
            {"model_version": model_version},
        )
        return {"job_id": job_id, "status": "queued"}

    def reprocess_status(self, user_id: str, job_id: str, jobs: Any) -> Dict[str, Any]:
        """Check status of a reprocessing background job.

        Args:
            user_id: ID of the user who owns the job.
            job_id: ID of the reprocessing job to check.
            jobs: Job queue service.

        Returns:
            Job status dictionary.

        Raises:
            ValueError: If job not found.
        """
        self.parent.require_user(user_id)
        job = jobs.get(job_id, user_id=user_id)
        if not job:
            raise ValueError("reprocess job not found")
        return job

    # Data Collection, Feedback, and Monitoring Methods

    def submit_capture_feedback(
        self,
        capture_id: str,
        user_id: str,
        feedback_type: str,
        issues: Optional[List[str]] = None,
        corrections: Optional[Dict[str, float]] = None,
        comment: Optional[str] = None
    ) -> Dict[str, Any]:
        """Submit user feedback on a capture for ML improvement.

        Collects structured feedback from users about capture quality and
        measurement accuracy to improve ML models.

        Args:
            capture_id: ID of the capture being rated.
            user_id: ID of the user providing feedback.
            feedback_type: Type of feedback ('quality', 'accuracy', etc).
            issues: List of identified issues with the capture.
            corrections: Dictionary of suggested metric corrections.
            comment: Optional free-text comment from user.

        Returns:
            Dictionary with feedback_id and success message.
        """
        from .feedback import FeedbackCollector

        feedback_collector = FeedbackCollector(self.db)
        feedback_id = feedback_collector.submit_feedback(
            capture_id=capture_id,
            user_id=user_id,
            feedback_type=feedback_type,
            issues=issues,
            corrections=corrections,
            comment=comment
        )

        return {
            "feedback_id": feedback_id,
            "message": "Feedback submitted successfully"
        }

    def get_feedback_stats(self) -> Dict[str, Any]:
        """Get aggregated feedback statistics for admin monitoring.

        Returns:
            Dictionary with feedback counts, averages, and distribution metrics.
        """
        from .feedback import FeedbackCollector

        feedback_collector = FeedbackCollector(self.db)
        return feedback_collector.get_feedback_stats()

    def get_feedback_corrections(self, limit: int = 100) -> List[Dict[str, Any]]:
        """Get user-submitted metric corrections for model retraining.

        Args:
            limit: Maximum number of correction records to retrieve.

        Returns:
            List of correction dictionaries with capture_id and corrected metrics.
        """
        from .feedback import FeedbackCollector

        feedback_collector = FeedbackCollector(self.db)
        return feedback_collector.get_pending_corrections(limit=limit)

    def get_metric_accuracy_analysis(self) -> Dict[str, Any]:
        """Get analysis of metric accuracy based on user feedback.

        Returns:
            Dictionary with accuracy metrics, error rates, and confidence distributions.
        """
        from .feedback import FeedbackCollector

        feedback_collector = FeedbackCollector(self.db)
        return feedback_collector.get_metric_accuracy_analysis()

    def get_model_health_status(self) -> Dict[str, Any]:
        """Get current model health monitoring status and alerts.

        Returns:
            Dictionary with model health metrics, drift indicators, and alert status.
        """
        from .ml_monitoring import ModelMonitor

        monitor = ModelMonitor(self.db)
        return monitor.get_health_status()

    def generate_monitoring_daily_report(self) -> Dict[str, Any]:
        """Generate daily monitoring report with model performance metrics.

        Returns:
            Dictionary with daily metrics, error rates, trends, and recommendations.
        """
        from .ml_monitoring import ModelMonitor

        monitor = ModelMonitor(self.db)
        return monitor.generate_daily_report()

    def get_collection_stats(self) -> Dict[str, Any]:
        """Get data collection statistics for monitoring and reporting.

        Returns:
            Dictionary with collection counts, consent status, and data quality metrics.
        """
        from .data_collection import DataCollector

        collector = DataCollector(self.db)
        return collector.get_collection_stats()

    def export_training_dataset(
        self,
        output_dir: str,
        min_quality: float = 0.75,
        max_samples: Optional[int] = None
    ) -> Dict[str, Any]:
        """Export collected data as training dataset for ML model improvement.

        Exports captures and metrics meeting quality thresholds to a structured
        dataset format for model training and evaluation.

        Args:
            output_dir: Directory path to export dataset to.
            min_quality: Minimum quality score for included captures (0.0-1.0).
            max_samples: Optional maximum number of samples to export.

        Returns:
            Dictionary with export statistics and file paths.
        """
        from .data_collection import DataCollector

        collector = DataCollector(self.db)
        stats = collector.export_training_dataset(
            output_dir=output_dir,
            min_quality=min_quality,
            max_samples=max_samples
        )

        return {
            "message": "Dataset exported successfully",
            "stats": stats
        }

    def cleanup_old_data(self, retention_days: int = 365) -> Dict[str, Any]:
        """Cleanup old collected data beyond retention period.

        Removes captured data older than the specified retention period to
        comply with data retention policies.

        Args:
            retention_days: Number of days to retain data. Defaults to 365.

        Returns:
            Dictionary with message and count of deleted records.
        """
        from .data_collection import DataCollector

        collector = DataCollector(self.db)
        deleted_count = collector.cleanup_old_data(retention_days=retention_days)

        return {
            "message": f"Cleaned up {deleted_count} old data files",
            "deleted_count": deleted_count
        }
