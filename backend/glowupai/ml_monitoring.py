"""
ML model monitoring and health checks.

Tracks model performance metrics, detects drift, and triggers alerts
when model quality degrades.
"""

from __future__ import annotations

import json
import logging
import os
import smtplib
from datetime import datetime, timedelta
from email.mime.text import MIMEText
from typing import Any

import numpy as np
import requests

from .db import Database

logger = logging.getLogger(__name__)


class ModelMonitor:
    """Monitors ML model health and performance."""

    def __init__(self, db: Database):
        self.db = db
        self.alert_thresholds = {
            "variance_threshold": 0.05,  # 5% variance
            "error_rate_threshold": 0.01,  # 1% error rate
            "drift_threshold": 0.15,  # 15% distribution change
        }

    def track_prediction(
        self,
        capture_id: str,
        predictions: dict[str, float],
        processing_time_ms: float,
        error: str | None = None,
    ) -> None:
        """
        Track a single prediction for monitoring.

        Args:
            capture_id: Capture ID
            predictions: Model predictions
            processing_time_ms: Processing time in milliseconds
            error: Error message if prediction failed
        """
        self.db.execute(
            """
            INSERT INTO model_predictions (
                id,
                capture_id,
                predictions_json,
                processing_time_ms,
                error,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """,
            (
                f"pred_{capture_id}",
                capture_id,
                json.dumps(predictions),
                processing_time_ms,
                error,
                datetime.now().isoformat(),
            ),
        )

    def calculate_variance(self, time_window_hours: int = 24) -> dict[str, float]:
        """
        Calculate prediction variance for each metric.

        Tests same image multiple times to detect prediction instability.

        Args:
            time_window_hours: Hours to look back

        Returns:
            Variance scores per metric
        """
        cutoff = (datetime.now() - timedelta(hours=time_window_hours)).isoformat()

        # Get recent predictions grouped by capture
        predictions = self.db.fetchall(
            """
            SELECT capture_id, predictions_json
            FROM model_predictions
            WHERE created_at >= ? AND error IS NULL
            """,
            (cutoff,),
        )

        # Group by capture ID
        capture_groups: dict[str, list[dict]] = {}
        for row in predictions:
            capture_id = row["capture_id"]
            try:
                preds = json.loads(row["predictions_json"])
                if capture_id not in capture_groups:
                    capture_groups[capture_id] = []
                capture_groups[capture_id].append(preds)
            except json.JSONDecodeError:
                continue

        # Calculate variance for captures with multiple predictions
        variances = {
            "blemish_count": [],
            "redness_score": [],
            "texture_score": [],
            "darkspot_area": [],
        }

        for capture_id, pred_list in capture_groups.items():
            if len(pred_list) < 2:
                continue

            for metric in variances.keys():
                values = [p.get(metric, 0) for p in pred_list]
                if values:
                    variance = np.var(values)
                    variances[metric].append(variance)

        # Average variance per metric
        avg_variances = {}
        for metric, var_list in variances.items():
            if var_list:
                avg_variances[metric] = float(np.mean(var_list))
            else:
                avg_variances[metric] = 0.0

        return avg_variances

    def detect_distribution_drift(self, time_window_days: int = 7) -> dict[str, float]:
        """
        Detect distribution drift in predictions.

        Compares recent prediction distribution to historical baseline.

        Args:
            time_window_days: Days to compare

        Returns:
            Drift scores per metric (0-1, higher = more drift)
        """
        # Get baseline (30-60 days ago)
        baseline_start = (datetime.now() - timedelta(days=60)).isoformat()
        baseline_end = (datetime.now() - timedelta(days=30)).isoformat()

        baseline_preds = self.db.fetchall(
            """
            SELECT predictions_json
            FROM model_predictions
            WHERE created_at >= ? AND created_at < ? AND error IS NULL
            """,
            (baseline_start, baseline_end),
        )

        # Get recent (last N days)
        recent_start = (datetime.now() - timedelta(days=time_window_days)).isoformat()
        recent_preds = self.db.fetchall(
            """
            SELECT predictions_json
            FROM model_predictions
            WHERE created_at >= ? AND error IS NULL
            """,
            (recent_start,),
        )

        if len(baseline_preds) < 10 or len(recent_preds) < 10:
            logger.warning("Not enough data to detect drift")
            return {}

        # Extract metric distributions
        metrics = ["blemish_count", "redness_score", "texture_score", "darkspot_area"]
        drift_scores = {}

        for metric in metrics:
            baseline_values = []
            recent_values = []

            for row in baseline_preds:
                try:
                    preds = json.loads(row["predictions_json"])
                    baseline_values.append(preds.get(metric, 0))
                except json.JSONDecodeError:
                    continue

            for row in recent_preds:
                try:
                    preds = json.loads(row["predictions_json"])
                    recent_values.append(preds.get(metric, 0))
                except json.JSONDecodeError:
                    continue

            if baseline_values and recent_values:
                # Calculate distribution difference (KL divergence approximation)
                baseline_mean = np.mean(baseline_values)
                recent_mean = np.mean(recent_values)
                baseline_std = np.std(baseline_values)
                recent_std = np.std(recent_values)

                # Simple drift score based on mean and std change
                mean_change = abs(recent_mean - baseline_mean) / (baseline_mean + 1e-6)
                std_change = abs(recent_std - baseline_std) / (baseline_std + 1e-6)

                drift_scores[metric] = float((mean_change + std_change) / 2)

        return drift_scores

    def calculate_error_rate(self, time_window_hours: int = 24) -> float:
        """
        Calculate prediction error rate.

        Args:
            time_window_hours: Hours to look back

        Returns:
            Error rate (0-1)
        """
        cutoff = (datetime.now() - timedelta(hours=time_window_hours)).isoformat()

        result = self.db.fetchone(
            """
            SELECT
                SUM(CASE WHEN error IS NOT NULL THEN 1 ELSE 0 END) as errors,
                COUNT(*) as total
            FROM model_predictions
            WHERE created_at >= ?
            """,
            (cutoff,),
        )

        if not result or result["total"] == 0:
            return 0.0

        return result["errors"] / result["total"]

    def get_processing_time_stats(self, time_window_hours: int = 24) -> dict[str, float]:
        """
        Get processing time statistics.

        Args:
            time_window_hours: Hours to look back

        Returns:
            Processing time stats (p50, p95, p99)
        """
        cutoff = (datetime.now() - timedelta(hours=time_window_hours)).isoformat()

        times = self.db.fetchall(
            """
            SELECT processing_time_ms
            FROM model_predictions
            WHERE created_at >= ? AND error IS NULL
            """,
            (cutoff,),
        )

        if not times:
            return {"p50": 0, "p95": 0, "p99": 0, "mean": 0}

        time_values = [row["processing_time_ms"] for row in times]

        return {
            "p50": float(np.percentile(time_values, 50)),
            "p95": float(np.percentile(time_values, 95)),
            "p99": float(np.percentile(time_values, 99)),
            "mean": float(np.mean(time_values)),
        }

    def get_health_status(self) -> dict[str, Any]:
        """
        Get overall model health status.

        Returns comprehensive health report.
        """
        health = {
            "timestamp": datetime.now().isoformat(),
            "status": "healthy",
            "issues": [],
        }

        # Check variance
        variances = self.calculate_variance(time_window_hours=24)
        max_variance = max(variances.values()) if variances else 0

        health["variance"] = {
            "current": variances,
            "threshold": self.alert_thresholds["variance_threshold"],
            "status": "ok" if max_variance < self.alert_thresholds["variance_threshold"] else "warning",
        }

        if max_variance >= self.alert_thresholds["variance_threshold"]:
            health["issues"].append(f"High variance detected: {max_variance:.3f}")
            health["status"] = "degraded"

        # Check error rate
        error_rate = self.calculate_error_rate(time_window_hours=24)
        health["error_rate"] = {
            "current": error_rate,
            "threshold": self.alert_thresholds["error_rate_threshold"],
            "status": "ok" if error_rate < self.alert_thresholds["error_rate_threshold"] else "critical",
        }

        if error_rate >= self.alert_thresholds["error_rate_threshold"]:
            health["issues"].append(f"High error rate: {error_rate:.1%}")
            health["status"] = "critical"

        # Check drift
        drift_scores = self.detect_distribution_drift(time_window_days=7)
        max_drift = max(drift_scores.values()) if drift_scores else 0

        health["drift"] = {
            "current": drift_scores,
            "threshold": self.alert_thresholds["drift_threshold"],
            "status": "ok" if max_drift < self.alert_thresholds["drift_threshold"] else "warning",
        }

        if max_drift >= self.alert_thresholds["drift_threshold"]:
            health["issues"].append(f"Distribution drift detected: {max_drift:.3f}")
            if health["status"] != "critical":
                health["status"] = "degraded"

        # Processing time
        health["processing_time"] = self.get_processing_time_stats(time_window_hours=24)

        return health

    def send_email_alert(self, subject: str, body: str) -> bool:
        """Send email alert."""
        email_config = {
            "smtp_host": os.getenv("SMTP_HOST", "smtp.gmail.com"),
            "smtp_port": int(os.getenv("SMTP_PORT", "587")),
            "smtp_user": os.getenv("SMTP_USER"),
            "smtp_pass": os.getenv("SMTP_PASSWORD"),
            "alert_email": os.getenv("ALERT_EMAIL"),
        }

        if not all([email_config["smtp_user"], email_config["smtp_pass"], email_config["alert_email"]]):
            logger.warning("Email not configured, skipping alert")
            return False

        try:
            msg = MIMEText(body)
            msg["Subject"] = f"[GlowupAI] {subject}"
            msg["From"] = email_config["smtp_user"]
            msg["To"] = email_config["alert_email"]

            with smtplib.SMTP(email_config["smtp_host"], email_config["smtp_port"]) as server:
                server.starttls()
                server.login(email_config["smtp_user"], email_config["smtp_pass"])
                server.send_message(msg)

            logger.info(f"Email alert sent: {subject}")
            return True

        except Exception as e:
            logger.error(f"Failed to send email alert: {e}")
            return False

    def send_slack_alert(self, message: str) -> bool:
        """Send Slack webhook alert."""
        webhook_url = os.getenv("SLACK_WEBHOOK_URL")

        if not webhook_url:
            logger.warning("Slack webhook not configured, skipping alert")
            return False

        try:
            payload = {
                "text": f":warning: *GlowupAI Model Alert*\n{message}",
            }
            response = requests.post(webhook_url, json=payload, timeout=10)
            response.raise_for_status()

            logger.info("Slack alert sent")
            return True

        except Exception as e:
            logger.error(f"Failed to send Slack alert: {e}")
            return False

    def check_and_alert(self) -> None:
        """
        Check model health and send alerts if thresholds exceeded.

        Should be called periodically (e.g., every hour).
        """
        health = self.get_health_status()

        # Send alerts if status is not healthy
        if health["status"] != "healthy":
            subject = f"Model Health: {health['status'].upper()}"
            body = f"""
Model Health Report
==================

Status: {health['status'].upper()}

Issues:
{chr(10).join(f"- {issue}" for issue in health['issues'])}

Variance: {health['variance']['current']}
Error Rate: {health['error_rate']['current']:.2%}
Drift Scores: {health['drift']['current']}

Processing Time (p95): {health['processing_time']['p95']:.1f}ms

Timestamp: {health['timestamp']}
            """

            # Send email for critical issues
            if health["status"] == "critical":
                self.send_email_alert(subject, body)

            # Send Slack for all issues
            slack_message = f"""
*Status:* {health['status'].upper()}
*Issues:* {len(health['issues'])}

{chr(10).join(f"• {issue}" for issue in health['issues'])}
            """
            self.send_slack_alert(slack_message)

        # Log health status
        self.db.execute(
            """
            INSERT INTO model_health_log (
                id,
                status,
                variance_json,
                error_rate,
                drift_json,
                issues_json,
                created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            (
                f"health_{datetime.now().strftime('%Y%m%d%H%M%S')}",
                health["status"],
                json.dumps(health["variance"]["current"]),
                health["error_rate"]["current"],
                json.dumps(health["drift"]["current"]),
                json.dumps(health["issues"]),
                datetime.now().isoformat(),
            ),
        )

    def generate_daily_report(self) -> dict[str, Any]:
        """
        Generate daily summary report.

        Returns report data.
        """
        report = {
            "date": datetime.now().strftime("%Y-%m-%d"),
            "health": self.get_health_status(),
            "predictions": {},
            "feedback": {},
        }

        # Prediction stats
        day_ago = (datetime.now() - timedelta(days=1)).isoformat()
        pred_result = self.db.fetchone(
            """
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN error IS NOT NULL THEN 1 ELSE 0 END) as errors,
                AVG(processing_time_ms) as avg_time
            FROM model_predictions
            WHERE created_at >= ?
            """,
            (day_ago,),
        )

        if pred_result:
            report["predictions"] = {
                "total": pred_result["total"],
                "errors": pred_result["errors"],
                "error_rate": pred_result["errors"] / pred_result["total"] if pred_result["total"] > 0 else 0,
                "avg_processing_time_ms": pred_result["avg_time"],
            }

        # Feedback stats
        feedback_result = self.db.fetchone(
            """
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN feedback_type = 'accurate' THEN 1 ELSE 0 END) as accurate
            FROM capture_feedback
            WHERE created_at >= ?
            """,
            (day_ago,),
        )

        if feedback_result and feedback_result["total"] > 0:
            report["feedback"] = {
                "total": feedback_result["total"],
                "accurate": feedback_result["accurate"],
                "accuracy_rate": feedback_result["accurate"] / feedback_result["total"],
            }

        return report
