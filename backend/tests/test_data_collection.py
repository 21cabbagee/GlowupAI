"""Unit tests for data collection pipeline."""

from __future__ import annotations

import hashlib
import json
import os
import tempfile
import unittest
from datetime import datetime, timedelta
from pathlib import Path
from unittest.mock import patch

from glowupai.data_collection import DataCollector
from glowupai.db import Database


class TestDataCollector(unittest.TestCase):
    """Test DataCollector class."""

    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.TemporaryDirectory()
        self.db_path = Path(self.temp_dir.name) / "test.sqlite3"
        self.storage_path = Path(self.temp_dir.name) / "training_data"

        self.db = Database(self.db_path)
        self.collector = DataCollector(self.db, storage_path=self.storage_path)

        # Create test user
        self.user_id = "test_user_123"
        self.db.execute(
            "INSERT INTO users (id, firebase_uid, consent_state) VALUES (?, ?, ?)",
            (self.user_id, "firebase_123", "granted"),
        )

        # Grant data collection consent
        self.db.execute(
            """
            INSERT INTO consent_events (
                user_id, consent_type, granted, policy_version
            ) VALUES (?, ?, ?, ?)
            """,
            (self.user_id, "data_collection", 1, "1.0"),
        )

        # Create test image file
        self.test_image_path = Path(self.temp_dir.name) / "test_image.jpg"
        self.test_image_path.write_bytes(b"fake image data")

    def tearDown(self):
        """Clean up test fixtures."""
        self.db.close()
        self.temp_dir.cleanup()

    def test_initialization(self):
        """Test DataCollector initialization creates directories."""
        self.assertTrue(self.storage_path.exists())
        self.assertTrue((self.storage_path / "images").exists())
        self.assertTrue((self.storage_path / "labels").exists())
        self.assertTrue((self.storage_path / "metadata").exists())

    def test_check_consent_granted(self):
        """Test consent check for user with consent."""
        has_consent = self.collector.check_consent(self.user_id)
        self.assertTrue(has_consent)

    def test_check_consent_not_granted(self):
        """Test consent check for user without consent."""
        # Create user without consent
        user_id_no_consent = "user_no_consent"
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id_no_consent, "firebase_456"),
        )

        has_consent = self.collector.check_consent(user_id_no_consent)
        self.assertFalse(has_consent)

    def test_check_consent_revoked(self):
        """Test consent check for user who revoked consent."""
        user_id_revoked = "user_revoked"
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id_revoked, "firebase_789"),
        )

        # Grant then revoke consent
        self.db.execute(
            """
            INSERT INTO consent_events (
                user_id, consent_type, granted, policy_version, recorded_at
            ) VALUES (?, ?, ?, ?, ?)
            """,
            (user_id_revoked, "data_collection", 1, "1.0",
             (datetime.now() - timedelta(days=2)).isoformat()),
        )

        self.db.execute(
            """
            INSERT INTO consent_events (
                user_id, consent_type, granted, policy_version, recorded_at
            ) VALUES (?, ?, ?, ?, ?)
            """,
            (user_id_revoked, "data_collection", 0, "1.0",
             (datetime.now() - timedelta(days=1)).isoformat()),
        )

        has_consent = self.collector.check_consent(user_id_revoked)
        self.assertFalse(has_consent)

    def test_check_consent_nonexistent_user(self):
        """Test consent check for nonexistent user."""
        has_consent = self.collector.check_consent("nonexistent_user")
        self.assertFalse(has_consent)

    def test_anonymize_user_id_format(self):
        """Test user ID anonymization produces expected format."""
        face_id = self.collector.anonymize_user_id(self.user_id)

        self.assertIsInstance(face_id, str)
        self.assertEqual(len(face_id), 16)
        # Should be hex characters
        self.assertTrue(all(c in "0123456789abcdef" for c in face_id))

    def test_anonymize_user_id_deterministic(self):
        """Test anonymization is deterministic (same input -> same output)."""
        face_id1 = self.collector.anonymize_user_id(self.user_id)
        face_id2 = self.collector.anonymize_user_id(self.user_id)

        self.assertEqual(face_id1, face_id2)

    def test_anonymize_user_id_different_users(self):
        """Test different users get different face IDs."""
        face_id1 = self.collector.anonymize_user_id("user_1")
        face_id2 = self.collector.anonymize_user_id("user_2")

        self.assertNotEqual(face_id1, face_id2)

    def test_anonymize_user_id_uses_sha256(self):
        """Test anonymization uses SHA-256 hashing."""
        with patch.dict("os.environ", {"DATA_COLLECTION_SALT": "test_salt"}):
            face_id = self.collector.anonymize_user_id("test_user")

            # Manually compute expected hash
            hash_input = f"test_user:test_salt".encode()
            expected = hashlib.sha256(hash_input).hexdigest()[:16]

            self.assertEqual(face_id, expected)

    def test_collect_capture_success(self):
        """Test successful data collection."""
        capture_id = "test_capture_123"
        metrics = {
            "blemish_count": 10.5,
            "redness_score": 0.35,
            "darkspot_area": 0.18,
            "texture_score": 8.2,
            "confidence": 0.92,
            "model_version": "ml-v2.0",
        }
        quality = {
            "brightness": 0.65,
            "sharpness": 0.88,
            "face_present": True,
            "yaw_degrees": 5.0,
            "pitch_degrees": -3.0,
            "distance_cm": 45.0,
            "score": 0.90,
        }
        device_meta = {
            "os": "iOS",
            "os_version": "17.0",
            "device_model": "iPhone 15 Pro",
            "camera_resolution": "4032x3024",
        }

        success = self.collector.collect_capture(
            capture_id=capture_id,
            user_id=self.user_id,
            image_path=str(self.test_image_path),
            metrics=metrics,
            quality=quality,
            device_meta=device_meta,
        )

        self.assertTrue(success)

        # Verify image was copied
        face_id = self.collector.anonymize_user_id(self.user_id)
        image_files = list(self.storage_path.glob(f"images/{face_id}_*.jpg"))
        self.assertEqual(len(image_files), 1)

        # Verify labels were saved
        label_files = list(self.storage_path.glob(f"labels/{face_id}_*.json"))
        self.assertEqual(len(label_files), 1)

        with open(label_files[0], "r") as f:
            labels = json.load(f)

        self.assertEqual(labels["blemish_count"], 10.5)
        self.assertEqual(labels["redness_score"], 0.35)
        self.assertEqual(labels["model_version"], "ml-v2.0")

        # Verify metadata was saved
        metadata_files = list(self.storage_path.glob(f"metadata/{face_id}_*.json"))
        self.assertEqual(len(metadata_files), 1)

        with open(metadata_files[0], "r") as f:
            metadata = json.load(f)

        self.assertEqual(metadata["face_id"], face_id)
        self.assertEqual(metadata["device"]["os"], "iOS")
        self.assertEqual(metadata["capture_quality"]["score"], 0.90)
        self.assertTrue(metadata["privacy"]["anonymized"])

        # Verify database log entry
        log_entry = self.db.fetchone(
            "SELECT * FROM collection_log WHERE face_id = ?",
            (face_id,),
        )
        self.assertIsNotNone(log_entry)
        self.assertEqual(log_entry["quality_score"], 0.90)

    def test_collect_capture_without_consent(self):
        """Test data collection skips users without consent."""
        user_no_consent = "user_no_consent"
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_no_consent, "firebase_999"),
        )

        success = self.collector.collect_capture(
            capture_id="capture_123",
            user_id=user_no_consent,
            image_path=str(self.test_image_path),
            metrics={},
            quality={},
            device_meta={},
        )

        self.assertFalse(success)

        # Verify no files were created
        image_files = list(self.storage_path.glob("images/*.jpg"))
        self.assertEqual(len(image_files), 0)

    def test_collect_capture_missing_image(self):
        """Test data collection fails gracefully with missing image."""
        success = self.collector.collect_capture(
            capture_id="capture_123",
            user_id=self.user_id,
            image_path="/nonexistent/image.jpg",
            metrics={},
            quality={},
            device_meta={},
        )

        self.assertFalse(success)

    def test_collect_capture_error_handling(self):
        """Test data collection handles errors gracefully."""
        # Corrupt storage path to trigger exception
        with patch("shutil.copy2", side_effect=Exception("Disk full")):
            success = self.collector.collect_capture(
                capture_id="capture_123",
                user_id=self.user_id,
                image_path=str(self.test_image_path),
                metrics={},
                quality={},
                device_meta={},
            )

            self.assertFalse(success)

    def test_classify_lighting(self):
        """Test lighting classification."""
        self.assertEqual(self.collector._classify_lighting(0.2), "low_light")
        self.assertEqual(self.collector._classify_lighting(0.4), "normal")
        self.assertEqual(self.collector._classify_lighting(0.7), "bright")
        self.assertEqual(self.collector._classify_lighting(0.9), "overexposed")

    def test_classify_lighting_boundaries(self):
        """Test lighting classification boundary conditions."""
        self.assertEqual(self.collector._classify_lighting(0.0), "low_light")
        self.assertEqual(self.collector._classify_lighting(0.3), "normal")
        self.assertEqual(self.collector._classify_lighting(0.6), "bright")
        self.assertEqual(self.collector._classify_lighting(0.8), "overexposed")
        self.assertEqual(self.collector._classify_lighting(1.0), "overexposed")

    def test_export_training_dataset(self):
        """Test exporting collected data as training dataset."""
        # Collect some data first
        for i in range(5):
            self.collector.collect_capture(
                capture_id=f"capture_{i}",
                user_id=self.user_id,
                image_path=str(self.test_image_path),
                metrics={"blemish_count": float(i)},
                quality={"score": 0.9},
                device_meta={},
            )

        # Export dataset
        export_dir = Path(self.temp_dir.name) / "exported_dataset"
        stats = self.collector.export_training_dataset(
            output_dir=export_dir,
            min_quality=0.75,
        )

        self.assertEqual(stats["total_samples"], 5)
        self.assertEqual(stats["failed_samples"], 0)

        # Verify export structure
        self.assertTrue((export_dir / "images").exists())
        self.assertTrue((export_dir / "labels").exists())
        self.assertTrue((export_dir / "split.json").exists())
        self.assertTrue((export_dir / "export_stats.json").exists())

        # Verify split.json
        with open(export_dir / "split.json", "r") as f:
            split_data = json.load(f)

        self.assertIn("train", split_data)
        self.assertIn("val", split_data)
        self.assertEqual(len(split_data["train"]), 4)  # 80% of 5
        self.assertEqual(len(split_data["val"]), 1)    # 20% of 5

    def test_export_training_dataset_quality_filter(self):
        """Test export filters by quality threshold."""
        # Collect data with varying quality
        for i in range(5):
            quality_score = 0.5 + i * 0.1  # 0.5, 0.6, 0.7, 0.8, 0.9
            self.collector.collect_capture(
                capture_id=f"capture_{i}",
                user_id=self.user_id,
                image_path=str(self.test_image_path),
                metrics={"blemish_count": float(i)},
                quality={"score": quality_score},
                device_meta={},
            )

        # Export with min_quality=0.75
        export_dir = Path(self.temp_dir.name) / "filtered_dataset"
        stats = self.collector.export_training_dataset(
            output_dir=export_dir,
            min_quality=0.75,
        )

        # Should only export samples with quality >= 0.75 (3 samples: 0.7, 0.8, 0.9)
        # Wait, 0.7 < 0.75, so should be 2 samples: 0.8, 0.9
        self.assertEqual(stats["total_samples"], 2)

    def test_export_training_dataset_max_samples(self):
        """Test export respects max_samples limit."""
        # Collect 10 samples
        for i in range(10):
            self.collector.collect_capture(
                capture_id=f"capture_{i}",
                user_id=self.user_id,
                image_path=str(self.test_image_path),
                metrics={"blemish_count": float(i)},
                quality={"score": 0.9},
                device_meta={},
            )

        # Export with max_samples=5
        export_dir = Path(self.temp_dir.name) / "limited_dataset"
        stats = self.collector.export_training_dataset(
            output_dir=export_dir,
            min_quality=0.5,
            max_samples=5,
        )

        self.assertEqual(stats["total_samples"], 5)

    def test_cleanup_old_data(self):
        """Test cleanup of old data."""
        # Insert old data
        old_date = (datetime.now() - timedelta(days=400)).isoformat()
        face_id = self.collector.anonymize_user_id(self.user_id)

        self.db.execute(
            """
            INSERT INTO collection_log (
                face_id, anonymous_capture_id, collected_at,
                quality_score, model_version
            ) VALUES (?, ?, ?, ?, ?)
            """,
            (face_id, "old_capture", old_date, 0.9, "ml-v1.0"),
        )

        # Create corresponding files
        (self.storage_path / "images" / f"{face_id}_old_old_capture.jpg").write_bytes(b"data")
        (self.storage_path / "labels" / f"{face_id}_old_old_capture.json").write_text("{}")
        (self.storage_path / "metadata" / f"{face_id}_old_old_capture.json").write_text("{}")

        # Run cleanup with 365-day retention
        deleted_count = self.collector.cleanup_old_data(retention_days=365)

        # Should delete 3 files
        self.assertEqual(deleted_count, 3)

        # Verify database record was deleted
        result = self.db.fetchone(
            "SELECT * FROM collection_log WHERE anonymous_capture_id = ?",
            ("old_capture",),
        )
        self.assertIsNone(result)

    def test_cleanup_old_data_preserves_recent(self):
        """Test cleanup preserves recent data."""
        # Insert recent data
        recent_date = (datetime.now() - timedelta(days=30)).isoformat()
        face_id = self.collector.anonymize_user_id(self.user_id)

        self.db.execute(
            """
            INSERT INTO collection_log (
                face_id, anonymous_capture_id, collected_at,
                quality_score, model_version
            ) VALUES (?, ?, ?, ?, ?)
            """,
            (face_id, "recent_capture", recent_date, 0.9, "ml-v2.0"),
        )

        # Run cleanup
        deleted_count = self.collector.cleanup_old_data(retention_days=365)

        # Should not delete anything
        self.assertEqual(deleted_count, 0)

        # Verify database record still exists
        result = self.db.fetchone(
            "SELECT * FROM collection_log WHERE anonymous_capture_id = ?",
            ("recent_capture",),
        )
        self.assertIsNotNone(result)

    def test_get_collection_stats_no_data(self):
        """Test collection stats with no data."""
        stats = self.collector.get_collection_stats()

        self.assertEqual(stats["total_samples"], 0)
        self.assertEqual(stats["unique_faces"], 0)
        self.assertEqual(stats["samples_last_7_days"], 0)

    def test_get_collection_stats_with_data(self):
        """Test collection stats calculation."""
        # Collect data
        for i in range(10):
            self.collector.collect_capture(
                capture_id=f"capture_{i}",
                user_id=self.user_id,
                image_path=str(self.test_image_path),
                metrics={"blemish_count": float(i)},
                quality={"score": 0.85 if i < 5 else 0.95},
                device_meta={},
            )

        stats = self.collector.get_collection_stats()

        self.assertEqual(stats["total_samples"], 10)
        self.assertEqual(stats["unique_faces"], 1)
        self.assertEqual(stats["samples_last_7_days"], 10)

        # Check quality distribution
        self.assertIn("quality_distribution", stats)
        self.assertIn("good", stats["quality_distribution"])
        self.assertIn("excellent", stats["quality_distribution"])

    def test_get_collection_stats_multiple_users(self):
        """Test collection stats with multiple users."""
        # Create another user
        user_id_2 = "user_2"
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id_2, "firebase_222"),
        )
        self.db.execute(
            """
            INSERT INTO consent_events (
                user_id, consent_type, granted, policy_version
            ) VALUES (?, ?, ?, ?)
            """,
            (user_id_2, "data_collection", 1, "1.0"),
        )

        # Create test image for second user
        test_image_2 = Path(self.temp_dir.name) / "test_image_2.jpg"
        test_image_2.write_bytes(b"fake image 2")

        # Collect from both users
        self.collector.collect_capture(
            capture_id="capture_1",
            user_id=self.user_id,
            image_path=str(self.test_image_path),
            metrics={},
            quality={"score": 0.9},
            device_meta={},
        )

        self.collector.collect_capture(
            capture_id="capture_2",
            user_id=user_id_2,
            image_path=str(test_image_2),
            metrics={},
            quality={"score": 0.9},
            device_meta={},
        )

        stats = self.collector.get_collection_stats()

        self.assertEqual(stats["total_samples"], 2)
        self.assertEqual(stats["unique_faces"], 2)

    def test_collect_capture_anonymity(self):
        """Test that collected data cannot be traced back to user."""
        capture_id = "secret_capture"

        success = self.collector.collect_capture(
            capture_id=capture_id,
            user_id=self.user_id,
            image_path=str(self.test_image_path),
            metrics={},
            quality={"score": 0.9},
            device_meta={"os": "iOS"},
        )

        self.assertTrue(success)

        # Check that no files contain the original user_id or capture_id
        face_id = self.collector.anonymize_user_id(self.user_id)

        # Check metadata file
        metadata_files = list(self.storage_path.glob(f"metadata/{face_id}_*.json"))
        self.assertEqual(len(metadata_files), 1)

        with open(metadata_files[0], "r") as f:
            content = f.read()

        # Original IDs should not appear in metadata
        self.assertNotIn(self.user_id, content)
        self.assertNotIn(capture_id, content)

        # But anonymized versions should be present
        self.assertIn(face_id, content)


if __name__ == "__main__":
    unittest.main()
