"""
Data collection pipeline for future model improvements.

Collects anonymized capture data from users who have opted in,
for training future versions of the ML model.
"""

from __future__ import annotations

import hashlib
import json
import logging
import os
import shutil
from datetime import UTC, datetime, timedelta
from pathlib import Path
from typing import Any

from .db import Database

logger = logging.getLogger(__name__)


class DataCollector:
    """Manages anonymized data collection for model training."""

    def __init__(
        self, db: Database, storage_path: str | Path = ".data/training_collection"
    ):
        self.db = db
        self.storage_path = Path(storage_path)
        self.storage_path.mkdir(parents=True, exist_ok=True)

        # Subdirectories for organization
        (self.storage_path / "images").mkdir(exist_ok=True)
        (self.storage_path / "labels").mkdir(exist_ok=True)
        (self.storage_path / "metadata").mkdir(exist_ok=True)

    def check_consent(self, user_id: str) -> bool:
        """Check if user has consented to data collection."""
        user = self.db.fetchone(
            "SELECT consent_state FROM users WHERE id = ?",
            (user_id,),
        )
        if not user:
            return False

        # Check for explicit data collection consent
        consent = self.db.fetchone(
            """
            SELECT granted FROM consent_events
            WHERE user_id = ? AND consent_type = 'data_collection'
            ORDER BY recorded_at DESC
            LIMIT 1
            """,
            (user_id,),
        )

        return consent is not None and consent["granted"] == 1

    def anonymize_user_id(self, user_id: str) -> str:
        """
        Create anonymized face_id hash from user_id.

        This one-way hash ensures we can't trace data back to users,
        but can still group captures from the same face.
        """
        # Use SHA-256 for strong one-way hashing
        salt = os.getenv("DATA_COLLECTION_SALT", "glowup-training-2026")
        hash_input = f"{user_id}:{salt}".encode()
        return hashlib.sha256(hash_input).hexdigest()[:16]

    def collect_capture(
        self,
        capture_id: str,
        user_id: str,
        image_path: str,
        metrics: dict[str, Any],
        quality: dict[str, Any],
        device_meta: dict[str, Any],
    ) -> bool:
        """
        Collect anonymized capture data for training.

        Args:
            capture_id: Original capture ID
            user_id: User ID (will be anonymized)
            image_path: Path to captured image
            metrics: Ground truth metrics from analysis
            quality: Capture quality metrics
            device_meta: Device metadata

        Returns:
            True if collection successful, False otherwise
        """
        # Check consent
        if not self.check_consent(user_id):
            logger.info(f"Skipping collection for {user_id} - no consent")
            return False

        try:
            # Anonymize user ID
            face_id = self.anonymize_user_id(user_id)

            # Generate anonymous filename
            timestamp = datetime.now(UTC).strftime("%Y%m%d_%H%M%S")
            anonymous_id = hashlib.sha256(capture_id.encode()).hexdigest()[:12]
            filename = f"{face_id}_{timestamp}_{anonymous_id}"

            # Copy image to collection storage
            image_dest = self.storage_path / "images" / f"{filename}.jpg"
            if os.path.exists(image_path):
                shutil.copy2(image_path, image_dest)
            else:
                logger.warning(f"Image not found: {image_path}")
                return False

            # Save ground truth labels
            labels = {
                "blemish_count": float(metrics.get("blemish_count", 0)),
                "redness_score": float(metrics.get("redness_score", 0)),
                "darkspot_area": float(metrics.get("darkspot_area", 0)),
                "texture_score": float(metrics.get("texture_score", 0)),
                "confidence": float(metrics.get("confidence", 0)),
                "model_version": metrics.get("model_version", "unknown"),
            }

            labels_path = self.storage_path / "labels" / f"{filename}.json"
            with open(labels_path, "w") as f:
                json.dump(labels, f, indent=2)

            # Save metadata (lighting, device, quality checks)
            metadata = {
                "face_id": face_id,
                "capture_id": anonymous_id,
                "collected_at": datetime.now(UTC).isoformat(),
                "lighting": {
                    "brightness": quality.get("brightness", 0),
                    "conditions": self._classify_lighting(quality.get("brightness", 0)),
                },
                "capture_quality": {
                    "sharpness": quality.get("sharpness", 0),
                    "face_present": quality.get("face_present", True),
                    "yaw_degrees": quality.get("yaw_degrees", 0),
                    "pitch_degrees": quality.get("pitch_degrees", 0),
                    "distance_cm": quality.get("distance_cm", 45),
                    "score": quality.get("score", 0),
                },
                "device": {
                    "os": device_meta.get("os", "unknown"),
                    "os_version": device_meta.get("os_version", "unknown"),
                    "device_model": device_meta.get("device_model", "unknown"),
                    "camera_resolution": device_meta.get(
                        "camera_resolution", "unknown"
                    ),
                },
                "privacy": {
                    "anonymized": True,
                    "user_id_hash": face_id,
                    "collection_date": datetime.now(UTC).strftime("%Y-%m-%d"),
                },
            }

            metadata_path = self.storage_path / "metadata" / f"{filename}.json"
            with open(metadata_path, "w") as f:
                json.dump(metadata, f, indent=2)

            # Log collection in database
            self.db.execute(
                """
                INSERT INTO collection_log (
                    face_id,
                    anonymous_capture_id,
                    collected_at,
                    quality_score,
                    model_version
                ) VALUES (?, ?, ?, ?, ?)
                """,
                (
                    face_id,
                    anonymous_id,
                    datetime.now(UTC).isoformat(),
                    quality.get("score", 0),
                    metrics.get("model_version", "unknown"),
                ),
            )

            logger.info(f"Collected anonymized data: {filename}")
            return True

        except Exception as e:
            logger.error(f"Failed to collect data: {e}")
            return False

    def _classify_lighting(self, brightness: float) -> str:
        """Classify lighting conditions from brightness value."""
        if brightness < 0.3:
            return "low_light"
        elif brightness < 0.6:
            return "normal"
        elif brightness < 0.8:
            return "bright"
        else:
            return "overexposed"

    def export_training_dataset(
        self,
        output_dir: str | Path,
        min_quality: float = 0.75,
        max_samples: int | None = None,
    ) -> dict[str, Any]:
        """
        Export collected data as a training dataset.

        Args:
            output_dir: Directory to export dataset to
            min_quality: Minimum quality score to include
            max_samples: Maximum number of samples to export (None = all)

        Returns:
            Export statistics
        """
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)
        (output_dir / "images").mkdir(exist_ok=True)
        (output_dir / "labels").mkdir(exist_ok=True)

        # Get all collected samples that meet quality threshold
        samples = self.db.fetchall(
            """
            SELECT face_id, anonymous_capture_id, quality_score
            FROM collection_log
            WHERE quality_score >= ?
            ORDER BY collected_at DESC
            """,
            (min_quality,),
        )

        if max_samples:
            samples = samples[:max_samples]

        exported_count = 0
        failed_count = 0

        for sample in samples:
            face_id = sample["face_id"]
            capture_id = sample["anonymous_capture_id"]

            # Find matching files
            image_files = list(
                self.storage_path.glob(f"images/{face_id}_*_{capture_id}.jpg")
            )
            label_files = list(
                self.storage_path.glob(f"labels/{face_id}_*_{capture_id}.json")
            )

            if image_files and label_files:
                # Copy to export directory
                shutil.copy2(
                    image_files[0], output_dir / "images" / image_files[0].name
                )
                shutil.copy2(
                    label_files[0], output_dir / "labels" / label_files[0].name
                )
                exported_count += 1
            else:
                failed_count += 1

        # Create train/val split
        all_samples = list((output_dir / "images").glob("*.jpg"))
        import random

        random.shuffle(all_samples)

        split_idx = int(len(all_samples) * 0.8)
        train_files = all_samples[:split_idx]
        val_files = all_samples[split_idx:]

        split_info = {
            "train": [f.name for f in train_files],
            "val": [f.name for f in val_files],
        }

        with open(output_dir / "split.json", "w") as f:
            json.dump(split_info, f, indent=2)

        # Export statistics
        stats = {
            "total_samples": exported_count,
            "failed_samples": failed_count,
            "train_samples": len(train_files),
            "val_samples": len(val_files),
            "min_quality_threshold": min_quality,
            "export_date": datetime.now(UTC).isoformat(),
        }

        with open(output_dir / "export_stats.json", "w") as f:
            json.dump(stats, f, indent=2)

        logger.info(f"Exported {exported_count} samples to {output_dir}")
        return stats

    def cleanup_old_data(self, retention_days: int = 365) -> int:
        """
        Delete data older than retention period (GDPR/CCPA compliance).

        Args:
            retention_days: Number of days to retain data

        Returns:
            Number of records deleted
        """
        cutoff_date = datetime.now(UTC) - timedelta(days=retention_days)

        # Get old records
        old_records = self.db.fetchall(
            """
            SELECT face_id, anonymous_capture_id
            FROM collection_log
            WHERE collected_at < ?
            """,
            (cutoff_date.isoformat(),),
        )

        deleted_count = 0

        for record in old_records:
            face_id = record["face_id"]
            capture_id = record["anonymous_capture_id"]

            # Delete files
            for pattern in [
                f"images/{face_id}_*_{capture_id}.jpg",
                f"labels/{face_id}_*_{capture_id}.json",
                f"metadata/{face_id}_*_{capture_id}.json",
            ]:
                for file_path in self.storage_path.glob(pattern):
                    file_path.unlink()
                    deleted_count += 1

        # Delete database records
        self.db.execute(
            "DELETE FROM collection_log WHERE collected_at < ?",
            (cutoff_date.isoformat(),),
        )

        logger.info(f"Cleaned up {deleted_count} old data files")
        return deleted_count

    def get_collection_stats(self) -> dict[str, Any]:
        """Get statistics about collected data."""
        stats = {}

        # Total collected samples
        result = self.db.fetchone(
            "SELECT COUNT(*) as count FROM collection_log",
        )
        stats["total_samples"] = result["count"] if result else 0

        # Samples by quality
        quality_dist = self.db.fetchall(
            """
            SELECT
                CASE
                    WHEN quality_score >= 0.9 THEN 'excellent'
                    WHEN quality_score >= 0.75 THEN 'good'
                    WHEN quality_score >= 0.5 THEN 'fair'
                    ELSE 'poor'
                END as quality_tier,
                COUNT(*) as count
            FROM collection_log
            GROUP BY quality_tier
            """,
        )
        stats["quality_distribution"] = {
            row["quality_tier"]: row["count"] for row in quality_dist
        }

        # Unique faces
        result = self.db.fetchone(
            "SELECT COUNT(DISTINCT face_id) as count FROM collection_log",
        )
        stats["unique_faces"] = result["count"] if result else 0

        # Collection rate (last 7 days)
        week_ago = (datetime.now(UTC) - timedelta(days=7)).isoformat()
        result = self.db.fetchone(
            "SELECT COUNT(*) as count FROM collection_log WHERE collected_at >= ?",
            (week_ago,),
        )
        stats["samples_last_7_days"] = result["count"] if result else 0

        return stats
