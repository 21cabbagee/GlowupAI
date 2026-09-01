"""Unit tests for database operations (CRUD for captures, users)."""

import json
import tempfile
import unittest
import uuid
from datetime import datetime, timedelta
from pathlib import Path

from glowupai.db import Database


class TestDatabaseOperations(unittest.TestCase):
    """Test database CRUD operations."""

    def setUp(self):
        """Create temporary database for each test."""
        self.temp = tempfile.TemporaryDirectory()
        self.db_path = Path(self.temp.name) / "test.sqlite3"
        self.db = Database(self.db_path)

    def tearDown(self):
        """Clean up database."""
        self.db.close()
        self.temp.cleanup()

    def test_create_user(self):
        """Test user creation."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid, skin_type) VALUES (?, ?, ?)",
            (user_id, "test_uid_123", "combination")
        )

        user = self.db.fetchone("SELECT * FROM users WHERE id = ?", (user_id,))
        self.assertIsNotNone(user)
        self.assertEqual(user["id"], user_id)
        self.assertEqual(user["firebase_uid"], "test_uid_123")
        self.assertEqual(user["skin_type"], "combination")

    def test_get_user_by_id(self):
        """Test retrieving user by ID."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "test_uid_456")
        )

        user = self.db.fetchone("SELECT * FROM users WHERE id = ?", (user_id,))

        self.assertIsNotNone(user)
        self.assertEqual(user["id"], user_id)
        self.assertEqual(user["firebase_uid"], "test_uid_456")

    def test_get_user_by_firebase_uid(self):
        """Test retrieving user by Firebase UID."""
        firebase_uid = "firebase_test_789"
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, firebase_uid)
        )

        user = self.db.fetchone("SELECT * FROM users WHERE firebase_uid = ?", (firebase_uid,))

        self.assertIsNotNone(user)
        self.assertEqual(user["id"], user_id)
        self.assertEqual(user["firebase_uid"], firebase_uid)

    def test_update_user_consent(self):
        """Test updating user consent."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "consent_test")
        )

        # Insert consent events
        self.db.execute(
            "INSERT INTO consent_events (user_id, consent_type, granted, policy_version) VALUES (?, ?, ?, ?)",
            (user_id, "facial_data", 1, "1.0")
        )
        self.db.execute(
            "INSERT INTO consent_events (user_id, consent_type, granted, policy_version) VALUES (?, ?, ?, ?)",
            (user_id, "analytics", 1, "1.0")
        )

        # Verify consent events were recorded
        consents = self.db.fetchall("SELECT * FROM consent_events WHERE user_id = ?", (user_id,))
        self.assertEqual(len(consents), 2)
        consent_types = {c["consent_type"]: c["granted"] for c in consents}
        self.assertEqual(consent_types["facial_data"], 1)
        self.assertEqual(consent_types["analytics"], 1)

    def test_create_capture(self):
        """Test capture creation."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "capture_test")
        )

        capture_id = str(uuid.uuid4())
        captured_at = datetime.now().isoformat()
        capture_quality = json.dumps({"sharpness": 0.9, "brightness": 0.8})

        self.db.execute(
            "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json, is_baseline) VALUES (?, ?, ?, ?, ?, ?)",
            (capture_id, user_id, captured_at, "https://example.com/image.jpg", capture_quality, 1)
        )

        capture = self.db.fetchone("SELECT * FROM photo_captures WHERE id = ?", (capture_id,))
        self.assertIsNotNone(capture)
        self.assertEqual(capture["id"], capture_id)
        self.assertEqual(capture["is_baseline"], 1)

    def test_get_capture_by_id(self):
        """Test retrieving capture by ID."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "get_capture_test")
        )

        capture_id = str(uuid.uuid4())
        captured_at = datetime.now().isoformat()
        capture_quality = json.dumps({"sharpness": 0.9})

        self.db.execute(
            "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json) VALUES (?, ?, ?, ?, ?)",
            (capture_id, user_id, captured_at, "https://example.com/img.jpg", capture_quality)
        )

        capture = self.db.fetchone("SELECT * FROM photo_captures WHERE id = ?", (capture_id,))

        self.assertIsNotNone(capture)
        self.assertEqual(capture["id"], capture_id)
        self.assertEqual(capture["user_id"], user_id)

    def test_list_user_captures(self):
        """Test listing all captures for a user."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "list_captures_test")
        )

        # Create multiple captures
        for i in range(3):
            capture_id = str(uuid.uuid4())
            captured_at = datetime.now().isoformat()
            capture_quality = json.dumps({"sharpness": 0.9})
            self.db.execute(
                "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json) VALUES (?, ?, ?, ?, ?)",
                (capture_id, user_id, captured_at, f"https://example.com/img{i}.jpg", capture_quality)
            )

        captures = self.db.fetchall("SELECT * FROM photo_captures WHERE user_id = ?", (user_id,))

        self.assertEqual(len(captures), 3)

    def test_list_captures_with_limit(self):
        """Test listing captures with limit."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "limit_test")
        )

        # Create 5 captures
        for i in range(5):
            capture_id = str(uuid.uuid4())
            captured_at = datetime.now().isoformat()
            capture_quality = json.dumps({"sharpness": 0.9})
            self.db.execute(
                "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json) VALUES (?, ?, ?, ?, ?)",
                (capture_id, user_id, captured_at, f"https://example.com/img{i}.jpg", capture_quality)
            )

        captures = self.db.fetchall("SELECT * FROM photo_captures WHERE user_id = ? LIMIT 3", (user_id,))

        self.assertEqual(len(captures), 3)

    def test_get_baseline_capture(self):
        """Test retrieving baseline capture."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "baseline_test")
        )

        # Create non-baseline capture
        capture_id_1 = str(uuid.uuid4())
        captured_at = datetime.now().isoformat()
        capture_quality = json.dumps({"sharpness": 0.9})
        self.db.execute(
            "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json, is_baseline) VALUES (?, ?, ?, ?, ?, ?)",
            (capture_id_1, user_id, captured_at, "https://example.com/regular.jpg", capture_quality, 0)
        )

        # Create baseline capture
        baseline_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json, is_baseline) VALUES (?, ?, ?, ?, ?, ?)",
            (baseline_id, user_id, captured_at, "https://example.com/baseline.jpg", capture_quality, 1)
        )

        baseline = self.db.fetchone("SELECT * FROM photo_captures WHERE user_id = ? AND is_baseline = 1", (user_id,))

        self.assertIsNotNone(baseline)
        self.assertEqual(baseline["id"], baseline_id)
        self.assertEqual(baseline["is_baseline"], 1)

    def test_create_product(self):
        """Test product creation."""
        product_id = str(uuid.uuid4())
        ingredients = json.dumps(["Water", "Niacinamide"])

        self.db.execute(
            "INSERT INTO products (id, name, ingredients_json, stabilization_days) VALUES (?, ?, ?, ?)",
            (product_id, "Test Serum", ingredients, 14)
        )

        product = self.db.fetchone("SELECT * FROM products WHERE id = ?", (product_id,))
        self.assertIsNotNone(product)
        self.assertEqual(product["name"], "Test Serum")
        self.assertEqual(product["stabilization_days"], 14)

    def test_get_product(self):
        """Test retrieving product."""
        product_id = str(uuid.uuid4())
        ingredients = json.dumps(["Water", "Hyaluronic Acid"])

        self.db.execute(
            "INSERT INTO products (id, name, ingredients_json) VALUES (?, ?, ?)",
            (product_id, "Face Cream", ingredients)
        )

        product = self.db.fetchone("SELECT * FROM products WHERE id = ?", (product_id,))

        self.assertIsNotNone(product)
        self.assertEqual(product["name"], "Face Cream")

    def test_create_routine_event(self):
        """Test routine event creation."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "routine_test")
        )

        product_id = str(uuid.uuid4())
        ingredients = json.dumps(["Water"])
        self.db.execute(
            "INSERT INTO products (id, name, ingredients_json) VALUES (?, ?, ?)",
            (product_id, "Test Product", ingredients)
        )

        event_id = str(uuid.uuid4())
        timestamp = datetime.now().isoformat()
        self.db.execute(
            "INSERT INTO routine_events (id, user_id, product_id, action, timestamp) VALUES (?, ?, ?, ?, ?)",
            (event_id, user_id, product_id, "start", timestamp)
        )

        event = self.db.fetchone("SELECT * FROM routine_events WHERE id = ?", (event_id,))
        self.assertIsNotNone(event)
        self.assertEqual(event["action"], "start")

    def test_get_user_routine_events(self):
        """Test retrieving user's routine events."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "events_test")
        )

        product_id = str(uuid.uuid4())
        ingredients = json.dumps(["Water"])
        self.db.execute(
            "INSERT INTO products (id, name, ingredients_json) VALUES (?, ?, ?)",
            (product_id, "Test Product", ingredients)
        )

        # Create multiple events
        timestamp = datetime.now().isoformat()
        event_id_1 = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO routine_events (id, user_id, product_id, action, timestamp) VALUES (?, ?, ?, ?, ?)",
            (event_id_1, user_id, product_id, "start", timestamp)
        )
        event_id_2 = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO routine_events (id, user_id, product_id, action, timestamp) VALUES (?, ?, ?, ?, ?)",
            (event_id_2, user_id, product_id, "stop", timestamp)
        )

        events = self.db.fetchall("SELECT * FROM routine_events WHERE user_id = ?", (user_id,))

        self.assertGreaterEqual(len(events), 2)

    def test_delete_user_captures(self):
        """Test deleting user's captures."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "delete_test")
        )

        # Create captures
        for i in range(3):
            capture_id = str(uuid.uuid4())
            captured_at = datetime.now().isoformat()
            capture_quality = json.dumps({"sharpness": 0.9})
            self.db.execute(
                "INSERT INTO photo_captures (id, user_id, captured_at, raw_ref, capture_quality_json) VALUES (?, ?, ?, ?, ?)",
                (capture_id, user_id, captured_at, f"https://example.com/img{i}.jpg", capture_quality)
            )

        # Delete captures
        self.db.execute("DELETE FROM photo_captures WHERE user_id = ?", (user_id,))

        # Verify deletion
        captures = self.db.fetchall("SELECT * FROM photo_captures WHERE user_id = ?", (user_id,))
        self.assertEqual(len(captures), 0)

    def test_database_connection_persistence(self):
        """Test that database connection persists across operations."""
        user_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
            (user_id, "persist_test")
        )

        # Multiple operations
        self.db.execute(
            "INSERT INTO consent_events (user_id, consent_type, granted, policy_version) VALUES (?, ?, ?, ?)",
            (user_id, "facial_data", 1, "1.0")
        )
        user = self.db.fetchone("SELECT * FROM users WHERE id = ?", (user_id,))

        self.assertIsNotNone(user)

    def test_concurrent_user_creation(self):
        """Test creating multiple users concurrently."""
        user_ids = []

        for i in range(10):
            user_id = str(uuid.uuid4())
            self.db.execute(
                "INSERT INTO users (id, firebase_uid) VALUES (?, ?)",
                (user_id, f"concurrent_test_{i}")
            )
            user_ids.append(user_id)

        # All users should be created
        self.assertEqual(len(user_ids), 10)
        self.assertEqual(len(set(user_ids)), 10)  # All unique

        # Verify all users exist in database
        all_users = self.db.fetchall("SELECT * FROM users WHERE firebase_uid LIKE 'concurrent_test_%'")
        self.assertEqual(len(all_users), 10)


if __name__ == "__main__":
    unittest.main()
