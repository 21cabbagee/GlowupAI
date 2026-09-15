"""Retryable account cleanup. The journal survives deletion of the user row."""

from __future__ import annotations

import time
from datetime import UTC, datetime


class AccountDeletion:
    def __init__(self, db, photos, auth_admin=None):
        self.db, self.photos, self.auth_admin = db, photos, auth_admin

    def request(self, user_id):
        user = self.db.fetchone(
            "SELECT id, supabase_uid FROM users WHERE id=?",
            (user_id,),
        )
        if not user:
            return
        # Commit intent before disabling access; either crash point is recoverable.
        self.db.execute(
            "INSERT INTO deletion_requests (user_id,requested_at) VALUES (?,?) "
            "ON CONFLICT (user_id) DO NOTHING",
            (user_id, datetime.now(UTC).isoformat()),
        )
        self.process(user_id)

    def process(self, user_id):
        running = self.db.fetchone(
            "SELECT j.id FROM jobs j JOIN job_leases l ON l.job_id=j.id "
            "WHERE j.user_id=? AND j.status='running' AND l.expires_at > ? LIMIT 1",
            (user_id, time.time()),
        )
        if running:
            raise RuntimeError("Account deletion is waiting for processing to finish")
        # AI capture work uses its own analysis_jobs table because it stores a
        # separate provider lifecycle. Do not delete the owner row while an
        # image/provider task can still write a result.
        try:
            running_analysis = self.db.fetchone(
                "SELECT id FROM analysis_jobs j JOIN photo_captures c ON c.id=j.capture_id "
                "WHERE c.user_id=? AND j.status='running' LIMIT 1",
                (user_id,),
            )
        except Exception:
            running_analysis = None
        if running_analysis:
            raise RuntimeError(
                "Account deletion is waiting for AI processing to finish"
            )
        self.db.execute(
            "UPDATE users SET deleted_at=? WHERE id=?",
            (datetime.now(UTC).isoformat(), user_id),
        )
        self.db.execute(
            "UPDATE jobs SET status='failed', error='Account deletion requested' WHERE user_id=? AND status='queued'",
            (user_id,),
        )
        self.photos.delete_user(user_id)
        if self.auth_admin and user and user["supabase_uid"]:
            self.auth_admin.delete_user(user["supabase_uid"])
        self.db.execute("DELETE FROM users WHERE id=?", (user_id,))
        # A crash between these deletes is safe: photo and SQL deletion are idempotent.
        self.db.execute("DELETE FROM deletion_requests WHERE user_id=?", (user_id,))

    def run_pending(self, limit=20):
        rows = self.db.fetchall(
            "SELECT user_id FROM deletion_requests ORDER BY requested_at LIMIT ?",
            (limit,),
        )
        completed = 0
        for row in rows:
            try:
                self.process(row["user_id"])
                completed += 1
            except Exception:
                self.db.execute(
                    "UPDATE deletion_requests SET attempts=attempts+1 WHERE user_id=?",
                    (row["user_id"],),
                )
        return completed
