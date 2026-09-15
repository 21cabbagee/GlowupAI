"""Persistent, at-least-once jobs. Handlers must tolerate replay."""

from __future__ import annotations

import json
import logging
import time
import uuid
from concurrent.futures import ThreadPoolExecutor
from datetime import UTC, datetime
from threading import Event, Thread

logger = logging.getLogger(__name__)


def now_iso():
    return datetime.now(UTC).isoformat().replace("+00:00", "Z")


class JobRunner:
    def __init__(
        self, db, max_workers=4, *, inline=True, lease_seconds=120, max_attempts=3
    ):
        self.db = db
        self.handlers = {}
        self.lease_seconds = lease_seconds
        self.max_attempts = max_attempts
        self._executor = ThreadPoolExecutor(max_workers=max_workers) if inline else None

    def register(self, job_type, handler):
        self.handlers[job_type] = handler

    def submit(self, job_type, fn=None, *args, user_id=None, payload=None):
        if job_type not in self.handlers:
            raise ValueError("job type has no registered durable handler")
        job_id = str(uuid.uuid4())
        self.db.execute(
            "INSERT INTO jobs (id, user_id, job_type, payload_json) VALUES (?, ?, ?, ?)",
            (job_id, user_id, job_type, json.dumps(payload or {})),
        )
        # The worker creates the lease, so a crash here cannot strand work.
        if self._executor:
            self._executor.submit(self.run, job_id)
        return job_id

    def run_pending(self, limit=20):
        rows = self.db.fetchall(
            "SELECT j.id FROM jobs j LEFT JOIN job_leases l ON l.job_id=j.id "
            "WHERE j.status IN ('queued','running') "
            "AND (l.job_id IS NULL OR l.expires_at < ?) ORDER BY j.created_at LIMIT ?",
            (time.time(), limit),
        )
        return sum(self.run(row["id"]) for row in rows)

    def run(self, job_id):
        row = self.db.fetchone("SELECT * FROM jobs WHERE id=?", (job_id,))
        if not row or row["status"] not in ("queued", "running"):
            return False
        if row["job_type"] not in self.handlers:
            return False
        self.db.execute(
            "INSERT INTO job_leases (job_id) VALUES (?) ON CONFLICT (job_id) DO NOTHING",
            (job_id,),
        )
        token = str(uuid.uuid4())
        claimed = self.db.execute(
            "UPDATE job_leases SET token=?, expires_at=?, attempts=attempts+1 "
            "WHERE job_id=? AND expires_at < ?",
            (token, time.time() + self.lease_seconds, job_id, time.time()),
        )
        if not claimed.rowcount:
            return False
        # Another worker may have completed between SELECT and acquisition.
        row = self.db.fetchone("SELECT * FROM jobs WHERE id=?", (job_id,))
        if not row or row["status"] not in ("queued", "running"):
            return False
        lease = self.db.fetchone(
            "SELECT attempts FROM job_leases WHERE job_id=?", (job_id,)
        )
        attempts = lease["attempts"]
        if attempts > self.max_attempts:
            self._finish(
                job_id,
                token,
                "failed",
                error="Job interrupted repeatedly; please retry.",
            )
            return True
        self.db.execute(
            "UPDATE jobs SET status='running', started_at=?, error=NULL WHERE id=?",
            (now_iso(), job_id),
        )
        stop = Event()

        def heartbeat():
            while not stop.wait(self.lease_seconds / 3):
                try:
                    changed = self.db.execute(
                        "UPDATE job_leases SET expires_at=? WHERE job_id=? AND token=?",
                        (time.time() + self.lease_seconds, job_id, token),
                    )
                    if not changed.rowcount:
                        return
                except Exception:
                    logger.warning("Job lease renewal failed", extra={"job_id": job_id})
                    return

        thread = Thread(target=heartbeat, daemon=True)
        thread.start()
        try:
            result = self.handlers[row["job_type"]](
                dict(row), json.loads(row["payload_json"])
            )
            self._finish(job_id, token, "completed", result=result)
        except Exception:
            # Provider errors may contain credentials or personal data.
            logger.warning(
                "Job execution failed", extra={"job_id": job_id, "attempt": attempts}
            )
            status = "failed" if attempts >= self.max_attempts else "queued"
            self._finish(
                job_id,
                token,
                status,
                error=(
                    "Processing failed; please retry." if status == "failed" else None
                ),
            )
        finally:
            stop.set()
            thread.join()
        return True

    def _finish(self, job_id, token, status, *, result=None, error=None):
        self.db.execute(
            "UPDATE jobs SET status=?, result_json=?, error=?, completed_at=? "
            "WHERE id=? AND EXISTS (SELECT 1 FROM job_leases WHERE job_id=? AND token=?)",
            (
                status,
                json.dumps(result) if result is not None else None,
                error,
                now_iso() if status in ("completed", "failed") else None,
                job_id,
                job_id,
                token,
            ),
        )
        self.db.execute(
            "UPDATE job_leases SET expires_at=? WHERE job_id=? AND token=?",
            (time.time() + 5, job_id, token),
        )

    def get(self, job_id, user_id=None):
        row = self.db.fetchone("SELECT * FROM jobs WHERE id=?", (job_id,))
        if not row:
            return None
        row = dict(row)
        if user_id is not None and row.get("user_id") != user_id:
            return None
        row.pop("payload_json")
        result = row.pop("result_json")
        row["payload"] = {}  # Never disclose private object references.
        row["result"] = json.loads(result) if result is not None else None
        return row

    def shutdown(self):
        if self._executor:
            self._executor.shutdown(wait=True, cancel_futures=True)
