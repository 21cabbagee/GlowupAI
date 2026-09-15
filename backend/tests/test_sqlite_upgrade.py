"""Old local databases must upgrade without reassigning account identities."""

import sqlite3

import pytest

from glowupai.db import Database, SCHEMA


def test_legacy_database_upgrade_preserves_data_and_enforces_uniqueness(tmp_path):
    path = tmp_path / "legacy.sqlite3"
    legacy = (
        SCHEMA.replace("supabase_uid", "firebase_uid")
        .replace("    idempotency_key TEXT,\n", "")
        .replace(
            "CREATE UNIQUE INDEX IF NOT EXISTS idx_captures_user_idempotency\n"
            "ON photo_captures(user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;",
            "",
        )
    )
    with sqlite3.connect(path) as connection:
        connection.executescript(legacy)
        connection.execute(
            "INSERT INTO users(id,firebase_uid) VALUES ('old','firebase-id')"
        )
    for _ in range(2):
        db = Database(path)
        try:
            user = db.fetchone("SELECT * FROM users WHERE id='old'")
            assert user["firebase_uid"] == "firebase-id"
            assert user["supabase_uid"] is None
            assert "idempotency_key" in {
                row["name"] for row in db.fetchall("PRAGMA table_info(photo_captures)")
            }
        finally:
            db.close()
    db = Database(path)
    try:
        db.execute("INSERT INTO users(id,supabase_uid) VALUES ('new','supabase-id')")
        with pytest.raises(sqlite3.IntegrityError):
            db.execute(
                "INSERT INTO users(id,supabase_uid) VALUES ('duplicate','supabase-id')"
            )
    finally:
        db.close()
