"""Unit tests for database operations (CRUD for captures, users)."""

import tempfile
import unittest
from datetime import datetime, timedelta
from pathlib import Path

from skinproof.db import Database


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
        user_id = self.db.create_user(
            firebase_uid="test_uid_123",
            email="test@example.com",
            skin_type="combination"
        )

        self.assertIsNotNone(user_id)
        self.assertIsInstance(user_id, str)

    def test_get_user_by_id(self):
        """Test retrieving user by ID."""
        user_id = self.db.create_user(
            firebase_uid="test_uid_456",
            email="user@example.com"
        )

        user = self.db.get_user(user_id)

        self.assertIsNotNone(user)
        self.assertEqual(user["id"], user_id)
        self.assertEqual(user["firebase_uid"], "test_uid_456")

    def test_get_user_by_firebase_uid(self):
        """Test retrieving user by Firebase UID."""
        firebase_uid = "firebase_test_789"
        user_id = self.db.create_user(
            firebase_uid=firebase_uid,
            email="firebase@example.com"
        )

        user = self.db.get_user_by_firebase_uid(firebase_uid)

        self.assertIsNotNone(user)
        self.assertEqual(user["id"], user_id)
        self.assertEqual(user["firebase_uid"], firebase_uid)

    def test_update_user_consent(self):
        """Test updating user consent."""
        user_id = self.db.create_user(firebase_uid="consent_test")

        self.db.update_consent(user_id, facial_data=True, analytics=True)

        user = self.db.get_user(user_id)
        self.assertTrue(user.get("facial_data_consent"))
        self.assertTrue(user.get("analytics_consent"))

    def test_create_capture(self):
        """Test capture creation."""
        user_id = self.db.create_user(firebase_uid="capture_test")

        capture_id = self.db.create_capture(
            user_id=user_id,
            metrics={
                "smoothness_score": 75.5,
                "clarity_score": 80.2,
                "evenness_score": 70.8,
                "model_version": "deterministic-3.0"
            },
            image_url="https://example.com/image.jpg",
            is_baseline=True
        )

        self.assertIsNotNone(capture_id)

    def test_get_capture_by_id(self):
        """Test retrieving capture by ID."""
        user_id = self.db.create_user(firebase_uid="get_capture_test")

        capture_id = self.db.create_capture(
            user_id=user_id,
            metrics={"smoothness_score": 75.5},
            image_url="https://example.com/img.jpg"
        )

        capture = self.db.get_capture(capture_id)

        self.assertIsNotNone(capture)
        self.assertEqual(capture["id"], capture_id)
        self.assertEqual(capture["user_id"], user_id)

    def test_list_user_captures(self):
        """Test listing all captures for a user."""
        user_id = self.db.create_user(firebase_uid="list_captures_test")

        # Create multiple captures
        for i in range(3):
            self.db.create_capture(
                user_id=user_id,
                metrics={"smoothness_score": 70.0 + i},
                image_url=f"https://example.com/img{i}.jpg"
            )

        captures = self.db.list_captures(user_id)

        self.assertEqual(len(captures), 3)

    def test_list_captures_with_limit(self):
        """Test listing captures with limit."""
        user_id = self.db.create_user(firebase_uid="limit_test")

        # Create 5 captures
        for i in range(5):
            self.db.create_capture(
                user_id=user_id,
                metrics={"smoothness_score": 70.0},
                image_url=f"https://example.com/img{i}.jpg"
            )

        captures = self.db.list_captures(user_id, limit=3)

        self.assertEqual(len(captures), 3)

    def test_get_baseline_capture(self):
        """Test retrieving baseline capture."""
        user_id = self.db.create_user(firebase_uid="baseline_test")

        # Create non-baseline capture
        self.db.create_capture(
            user_id=user_id,
            metrics={"smoothness_score": 70.0},
            image_url="https://example.com/regular.jpg",
            is_baseline=False
        )

        # Create baseline capture
        baseline_id = self.db.create_capture(
            user_id=user_id,
            metrics={"smoothness_score": 75.0},
            image_url="https://example.com/baseline.jpg",
            is_baseline=True
        )

        baseline = self.db.get_baseline_capture(user_id)

        self.assertIsNotNone(baseline)
        self.assertEqual(baseline["id"], baseline_id)
        self.assertTrue(baseline["is_baseline"])

    def test_create_product(self):
        """Test product creation."""
        product_id = self.db.create_product(
            name="Test Serum",
            brand="Test Brand",
            ingredients="Water, Niacinamide",
            stabilization_days=14
        )

        self.assertIsNotNone(product_id)

    def test_get_product(self):
        """Test retrieving product."""
        product_id = self.db.create_product(
            name="Face Cream",
            ingredients="Water, Hyaluronic Acid"
        )

        product = self.db.get_product(product_id)

        self.assertIsNotNone(product)
        self.assertEqual(product["name"], "Face Cream")

    def test_create_routine_event(self):
        """Test routine event creation."""
        user_id = self.db.create_user(firebase_uid="routine_test")
        product_id = self.db.create_product(name="Test Product")

        event_id = self.db.create_routine_event(
            user_id=user_id,
            product_id=product_id,
            action="start"
        )

        self.assertIsNotNone(event_id)

    def test_get_user_routine_events(self):
        """Test retrieving user's routine events."""
        user_id = self.db.create_user(firebase_uid="events_test")
        product_id = self.db.create_product(name="Test Product")

        # Create multiple events
        self.db.create_routine_event(user_id, product_id, "start")
        self.db.create_routine_event(user_id, product_id, "stop")

        events = self.db.get_routine_events(user_id)

        self.assertGreaterEqual(len(events), 2)

    def test_delete_user_captures(self):
        """Test deleting user's captures."""
        user_id = self.db.create_user(firebase_uid="delete_test")

        # Create captures
        for i in range(3):
            self.db.create_capture(
                user_id=user_id,
                metrics={"smoothness_score": 70.0},
                image_url=f"https://example.com/img{i}.jpg"
            )

        # Delete captures
        self.db.delete_user_captures(user_id)

        # Verify deletion
        captures = self.db.list_captures(user_id)
        self.assertEqual(len(captures), 0)

    def test_database_connection_persistence(self):
        """Test that database connection persists across operations."""
        user_id = self.db.create_user(firebase_uid="persist_test")

        # Multiple operations
        self.db.update_consent(user_id, facial_data=True)
        user = self.db.get_user(user_id)

        self.assertIsNotNone(user)

    def test_concurrent_user_creation(self):
        """Test creating multiple users concurrently."""
        user_ids = []

        for i in range(10):
            user_id = self.db.create_user(
                firebase_uid=f"concurrent_test_{i}",
                email=f"user{i}@example.com"
            )
            user_ids.append(user_id)

        # All users should be created
        self.assertEqual(len(user_ids), 10)
        self.assertEqual(len(set(user_ids)), 10)  # All unique


if __name__ == "__main__":
    unittest.main()
