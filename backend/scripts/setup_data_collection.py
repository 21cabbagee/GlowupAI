#!/usr/bin/env python3
"""
Setup script for data collection pipeline.

Runs database migrations, verifies tables, and tests the pipeline.
"""

import os
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from glowupai.db import Database
from glowupai.data_collection import DataCollector
from glowupai.feedback import FeedbackCollector
from glowupai.ml_monitoring import ModelMonitor


def run_migration():
    """Run database migration to add new tables."""
    print("=" * 60)
    print("STEP 1: Running Database Migration")
    print("=" * 60)

    db_path = Path(__file__).parent.parent / ".data" / "skinproof.sqlite3"
    migration_path = Path(__file__).parent.parent / "skinproof" / "migrations" / "003_data_collection_feedback.sql"

    if not migration_path.exists():
        print(f"❌ Migration file not found: {migration_path}")
        return False

    print(f"📁 Database: {db_path}")
    print(f"📄 Migration: {migration_path}")

    # Read migration SQL
    with open(migration_path) as f:
        migration_sql = f.read()

    # Execute migration
    db = Database(db_path)
    try:
        db.connection.executescript(migration_sql)
        db.connection.commit()
        print("✅ Migration executed successfully")
        return True
    except Exception as e:
        print(f"❌ Migration failed: {e}")
        return False


def verify_tables():
    """Verify all required tables exist."""
    print("\n" + "=" * 60)
    print("STEP 2: Verifying Tables")
    print("=" * 60)

    db = Database()

    required_tables = [
        "collection_log",
        "capture_feedback",
        "model_predictions",
        "model_health_log"
    ]

    existing_tables = db.fetchall(
        "SELECT name FROM sqlite_master WHERE type='table'"
    )
    existing_table_names = [row["name"] for row in existing_tables]

    print(f"\n📋 Checking {len(required_tables)} required tables...")

    all_exist = True
    for table in required_tables:
        exists = table in existing_table_names
        status = "✅" if exists else "❌"
        print(f"  {status} {table}")
        if not exists:
            all_exist = False

    if all_exist:
        print("\n✅ All required tables exist")
    else:
        print("\n❌ Some tables are missing")

    return all_exist


def test_modules():
    """Test that all modules can be imported and initialized."""
    print("\n" + "=" * 60)
    print("STEP 3: Testing Modules")
    print("=" * 60)

    db = Database()

    try:
        # Test DataCollector
        print("\n📦 Testing DataCollector...")
        collector = DataCollector(db)
        stats = collector.get_collection_stats()
        print(f"  ✅ DataCollector initialized")
        print(f"     Current samples: {stats['total_samples']}")

        # Test FeedbackCollector
        print("\n💬 Testing FeedbackCollector...")
        feedback = FeedbackCollector(db)
        feedback_stats = feedback.get_feedback_stats()
        print(f"  ✅ FeedbackCollector initialized")
        print(f"     Total feedback: {feedback_stats['total_feedback']}")

        # Test ModelMonitor
        print("\n📊 Testing ModelMonitor...")
        monitor = ModelMonitor(db)
        health = monitor.get_health_status()
        print(f"  ✅ ModelMonitor initialized")
        print(f"     Model status: {health['status']}")

        return True

    except Exception as e:
        print(f"\n❌ Module test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def check_environment():
    """Check required environment variables."""
    print("\n" + "=" * 60)
    print("STEP 4: Checking Environment")
    print("=" * 60)

    required_vars = {
        "DATA_COLLECTION_SALT": "Secret salt for anonymization",
    }

    optional_vars = {
        "SMTP_HOST": "Email alerts (optional)",
        "SMTP_PORT": "Email alerts (optional)",
        "SMTP_USER": "Email alerts (optional)",
        "SMTP_PASSWORD": "Email alerts (optional)",
        "ALERT_EMAIL": "Email alerts (optional)",
        "SLACK_WEBHOOK_URL": "Slack alerts (optional)",
    }

    print("\n🔐 Required variables:")
    all_set = True
    for var, desc in required_vars.items():
        value = os.getenv(var)
        if value:
            print(f"  ✅ {var}: {desc}")
        else:
            print(f"  ❌ {var}: {desc} - NOT SET")
            all_set = False

    print("\n🔧 Optional variables:")
    for var, desc in optional_vars.items():
        value = os.getenv(var)
        status = "✅" if value else "⚠️ "
        print(f"  {status} {var}: {desc}")

    if not all_set:
        print("\n⚠️  Add missing variables to .env file")

    return all_set


def create_sample_data():
    """Create sample data for testing (optional)."""
    print("\n" + "=" * 60)
    print("STEP 5: Create Sample Data (Optional)")
    print("=" * 60)

    response = input("\n📝 Create sample test data? (y/N): ")
    if response.lower() != 'y':
        print("⏭️  Skipped")
        return

    db = Database()

    # Create test user with consent
    print("\n👤 Creating test user...")
    db.execute(
        """
        INSERT OR IGNORE INTO users (id, created_at)
        VALUES (?, datetime('now'))
        """,
        ("test_user_123",)
    )

    # Record consent
    db.execute(
        """
        INSERT INTO consent_events (user_id, consent_type, granted, policy_version)
        VALUES (?, 'data_collection', 1, '1.0')
        """,
        ("test_user_123",)
    )

    print("  ✅ Test user created with consent")

    # Create test capture
    print("\n📸 Creating test capture...")
    db.execute(
        """
        INSERT OR IGNORE INTO photo_captures (
            id, user_id, captured_at, raw_ref, capture_quality_json, device_meta_json
        ) VALUES (?, ?, datetime('now'), ?, ?, ?)
        """,
        (
            "test_capture_123",
            "test_user_123",
            "test_image.jpg",
            '{"score": 0.85, "brightness": 0.55, "sharpness": 0.82}',
            '{"os": "Android", "device_model": "Test Device"}'
        )
    )

    # Create test metrics
    db.execute(
        """
        INSERT OR IGNORE INTO metric_snapshots (
            id, photo_id, user_id, model_version,
            blemish_count, redness_score, darkspot_area, texture_score,
            confidence, noise_floor_json
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        (
            "test_metric_123",
            "test_capture_123",
            "test_user_123",
            "v2.0.0",
            12, 0.34, 0.08, 15.2,
            0.92, '{}'
        )
    )

    print("  ✅ Test capture created")

    # Test feedback submission
    print("\n💬 Testing feedback submission...")
    feedback = FeedbackCollector(db)
    feedback_id = feedback.submit_feedback(
        capture_id="test_capture_123",
        user_id="test_user_123",
        feedback_type="inaccurate",
        issues=["blemishes_too_high"],
        corrections={"blemish_count": 8.0},
        comment="Test feedback"
    )
    print(f"  ✅ Feedback submitted: {feedback_id}")

    # Test monitoring tracking
    print("\n📊 Testing monitoring...")
    monitor = ModelMonitor(db)
    monitor.track_prediction(
        capture_id="test_capture_123",
        predictions={
            "blemish_count": 12,
            "redness_score": 0.34,
            "texture_score": 15.2,
            "darkspot_area": 0.08
        },
        processing_time_ms=234.5,
        error=None
    )
    print("  ✅ Prediction tracked")

    print("\n✅ Sample data created successfully")


def main():
    """Run full setup process."""
    print("\n")
    print("╔" + "═" * 58 + "╗")
    print("║" + " " * 10 + "DATA COLLECTION PIPELINE SETUP" + " " * 17 + "║")
    print("╚" + "═" * 58 + "╝")

    # Step 1: Run migration
    if not run_migration():
        print("\n❌ Setup failed at migration step")
        return 1

    # Step 2: Verify tables
    if not verify_tables():
        print("\n❌ Setup failed at table verification")
        return 1

    # Step 3: Test modules
    if not test_modules():
        print("\n❌ Setup failed at module testing")
        return 1

    # Step 4: Check environment
    env_ok = check_environment()
    if not env_ok:
        print("\n⚠️  Some environment variables missing (see above)")
        print("   Add them to .env file for full functionality")

    # Step 5: Create sample data (optional)
    create_sample_data()

    # Success!
    print("\n" + "=" * 60)
    print("✅ SETUP COMPLETE!")
    print("=" * 60)
    print("\n📚 Next steps:")
    print("  1. Review DATA_COLLECTION_README.md for usage examples")
    print("  2. Review DATA_COLLECTION_POLICY.md for privacy info")
    print("  3. Configure environment variables in .env")
    print("  4. Set up cron jobs for automated monitoring")
    print("  5. Deploy Android consent screen")
    print("\n🚀 Ready to collect data and improve the model!")

    return 0


if __name__ == "__main__":
    try:
        exit_code = main()
        sys.exit(exit_code)
    except KeyboardInterrupt:
        print("\n\n⚠️  Setup interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Setup failed with error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
