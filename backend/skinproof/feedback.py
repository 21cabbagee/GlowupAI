"""
Feedback collection system for model improvement.

Allows users to provide feedback on analysis accuracy,
helping identify model weaknesses and areas for improvement.
"""

from __future__ import annotations

import json
import logging
from datetime import datetime, timedelta
from typing import Any

from .db import Database

logger = logging.getLogger(__name__)


class FeedbackCollector:
    """Collects and analyzes user feedback on model predictions."""

    def __init__(self, db: Database):
        self.db = db

    def submit_feedback(
        self,
        capture_id: str,
        user_id: str,
        feedback_type: str,  # "accurate", "inaccurate"
        issues: list[str] | None = None,
        corrections: dict[str, float] | None = None,
        comment: str | None = None
    ) -> str:
        """
        Submit feedback for a capture.

        Args:
            capture_id: ID of the capture being rated
            user_id: User providing feedback
            feedback_type: "accurate" or "inaccurate"
            issues: List of perceived issues (e.g., ["blemishes_too_high", "redness_too_low"])
            corrections: Corrected metric values from user
            comment: Optional free-text comment

        Returns:
            Feedback ID
        """
        # Verify capture exists and belongs to user
        capture = self.db.fetchone(
            "SELECT id FROM photo_captures WHERE id = ? AND user_id = ?",
            (capture_id, user_id)
        )

        if not capture:
            raise ValueError(f"Capture {capture_id} not found for user {user_id}")

        # Get current metrics for this capture
        metrics = self.db.fetchone(
            """
            SELECT blemish_count, redness_score, texture_score, darkspot_area
            FROM metric_snapshots
            WHERE photo_id = ?
            ORDER BY created_at DESC
            LIMIT 1
            """,
            (capture_id,)
        )

        # Insert feedback
        feedback_id = self._generate_id()
        self.db.execute(
            """
            INSERT INTO capture_feedback (
                id,
                capture_id,
                user_id,
                feedback_type,
                issues_json,
                corrections_json,
                original_metrics_json,
                comment,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                feedback_id,
                capture_id,
                user_id,
                feedback_type,
                json.dumps(issues or []),
                json.dumps(corrections or {}),
                json.dumps(dict(metrics)) if metrics else "{}",
                comment,
                datetime.now().isoformat(),
            )
        )

        logger.info(f"Feedback submitted: {feedback_id} ({feedback_type})")
        return feedback_id

    def get_feedback_stats(self) -> dict[str, Any]:
        """Get aggregated feedback statistics."""
        stats = {}

        # Total feedback count
        result = self.db.fetchone(
            "SELECT COUNT(*) as count FROM capture_feedback"
        )
        stats["total_feedback"] = result["count"] if result else 0

        # Feedback by type
        type_dist = self.db.fetchall(
            """
            SELECT feedback_type, COUNT(*) as count
            FROM capture_feedback
            GROUP BY feedback_type
            """
        )
        stats["by_type"] = {row["feedback_type"]: row["count"] for row in type_dist}

        # Accuracy rate (last 30 days)
        month_ago = (datetime.now() - timedelta(days=30)).isoformat()
        result = self.db.fetchone(
            """
            SELECT
                SUM(CASE WHEN feedback_type = 'accurate' THEN 1 ELSE 0 END) as accurate,
                COUNT(*) as total
            FROM capture_feedback
            WHERE created_at >= ?
            """,
            (month_ago,)
        )

        if result and result["total"] > 0:
            stats["accuracy_rate_30d"] = round(result["accurate"] / result["total"], 3)
        else:
            stats["accuracy_rate_30d"] = None

        # Most common issues
        issues = self.db.fetchall(
            """
            SELECT issues_json
            FROM capture_feedback
            WHERE feedback_type = 'inaccurate'
            AND issues_json != '[]'
            """
        )

        issue_counts: dict[str, int] = {}
        for row in issues:
            try:
                issue_list = json.loads(row["issues_json"])
                for issue in issue_list:
                    issue_counts[issue] = issue_counts.get(issue, 0) + 1
            except (json.JSONDecodeError, TypeError):
                continue

        # Sort by frequency
        stats["top_issues"] = sorted(
            [{"issue": k, "count": v} for k, v in issue_counts.items()],
            key=lambda x: x["count"],
            reverse=True
        )[:10]

        return stats

    def get_metric_accuracy_analysis(self) -> dict[str, Any]:
        """
        Analyze which metrics are most frequently reported as inaccurate.

        Returns detailed breakdown by metric type.
        """
        # Get all inaccurate feedback with issues
        feedback = self.db.fetchall(
            """
            SELECT issues_json, corrections_json, original_metrics_json
            FROM capture_feedback
            WHERE feedback_type = 'inaccurate'
            """
        )

        metric_issues = {
            "blemish_count": {"too_high": 0, "too_low": 0, "total": 0},
            "redness_score": {"too_high": 0, "too_low": 0, "total": 0},
            "texture_score": {"too_high": 0, "too_low": 0, "total": 0},
            "darkspot_area": {"too_high": 0, "too_low": 0, "total": 0},
        }

        for row in feedback:
            try:
                issues = json.loads(row["issues_json"])
                for issue in issues:
                    # Parse issue format like "blemishes_too_high"
                    if "_too_high" in issue:
                        metric = issue.replace("_too_high", "")
                        if metric == "blemishes":
                            metric = "blemish_count"
                        if metric in metric_issues:
                            metric_issues[metric]["too_high"] += 1
                            metric_issues[metric]["total"] += 1

                    elif "_too_low" in issue:
                        metric = issue.replace("_too_low", "")
                        if metric == "blemishes":
                            metric = "blemish_count"
                        if metric in metric_issues:
                            metric_issues[metric]["too_low"] += 1
                            metric_issues[metric]["total"] += 1

            except (json.JSONDecodeError, TypeError):
                continue

        # Calculate percentages and identify problematic metrics
        analysis = {}
        for metric, counts in metric_issues.items():
            if counts["total"] > 0:
                analysis[metric] = {
                    "total_issues": counts["total"],
                    "too_high_pct": round(counts["too_high"] / counts["total"] * 100, 1),
                    "too_low_pct": round(counts["too_low"] / counts["total"] * 100, 1),
                    "bias": "overestimating" if counts["too_high"] > counts["too_low"] else "underestimating",
                }

        return analysis

    def get_pending_corrections(self, limit: int = 100) -> list[dict]:
        """
        Get feedback entries with user corrections for model retraining.

        Returns list of samples with original and corrected values.
        """
        results = self.db.fetchall(
            """
            SELECT
                f.capture_id,
                f.corrections_json,
                f.original_metrics_json,
                f.created_at,
                pc.raw_ref
            FROM capture_feedback f
            JOIN photo_captures pc ON f.capture_id = pc.id
            WHERE f.feedback_type = 'inaccurate'
            AND f.corrections_json != '{}'
            ORDER BY f.created_at DESC
            LIMIT ?
            """,
            (limit,)
        )

        corrections = []
        for row in results:
            try:
                original = json.loads(row["original_metrics_json"])
                corrected = json.loads(row["corrections_json"])

                corrections.append({
                    "capture_id": row["capture_id"],
                    "image_path": row["raw_ref"],
                    "original_predictions": original,
                    "user_corrections": corrected,
                    "feedback_date": row["created_at"],
                })
            except (json.JSONDecodeError, TypeError):
                continue

        return corrections

    def export_feedback_for_retraining(self, output_path: str) -> int:
        """
        Export feedback corrections as training data.

        Args:
            output_path: JSON file path to export to

        Returns:
            Number of corrections exported
        """
        corrections = self.get_pending_corrections(limit=10000)

        with open(output_path, "w") as f:
            json.dump(corrections, f, indent=2)

        logger.info(f"Exported {len(corrections)} corrections to {output_path}")
        return len(corrections)

    def should_trigger_retraining(self) -> tuple[bool, str]:
        """
        Check if enough feedback has been collected to trigger retraining.

        Returns:
            (should_retrain, reason)
        """
        # Check 1: Enough total feedback samples
        stats = self.get_feedback_stats()
        total_feedback = stats.get("total_feedback", 0)

        if total_feedback >= 1000:
            return True, f"Collected {total_feedback} feedback samples (threshold: 1000)"

        # Check 2: Low accuracy rate
        accuracy = stats.get("accuracy_rate_30d")
        if accuracy is not None and accuracy < 0.7:
            return True, f"Accuracy rate dropped to {accuracy:.1%} (threshold: 70%)"

        # Check 3: High correction rate with specific metric issues
        corrections = self.get_pending_corrections(limit=1)
        if len(corrections) >= 500:
            return True, f"Collected {len(corrections)} user corrections (threshold: 500)"

        return False, "Not enough feedback data yet"

    def _generate_id(self) -> str:
        """Generate unique feedback ID."""
        import secrets
        return f"fb_{secrets.token_hex(8)}"
