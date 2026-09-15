from concurrent.futures import ThreadPoolExecutor
from threading import Event

import pytest

from glowupai.durable_jobs import JobRunner
from glowupai.full_db import FullDatabase


@pytest.fixture
def db(tmp_path):
    database = FullDatabase(tmp_path / "jobs.sqlite3")
    database.execute("INSERT INTO users (id) VALUES ('owner')")
    yield database
    database.close()


def runner(db, handler):
    result = JobRunner(db, inline=False)
    result.register("test", handler)
    return result


def test_payload_survives_database_reopen(db):
    first = runner(db, lambda job, payload: None)
    job_id = first.submit("test", user_id="owner", payload={"value": 42})
    path = db.connection.execute("PRAGMA database_list").fetchone()[2]
    reopened = FullDatabase(path)
    try:
        second = runner(reopened, lambda job, payload: {"answer": payload["value"]})
        assert second.run_pending() == 1
        assert second.get(job_id, "owner")["result"] == {"answer": 42}
        assert second.run_pending() == 0
    finally:
        reopened.close()


def test_only_one_worker_claims_job(db):
    entered, release = Event(), Event()
    calls = []

    def work(job, payload):
        calls.append(job["id"])
        entered.set()
        assert release.wait(5)
        return {"ok": True}

    first, second = runner(db, work), runner(db, work)
    job_id = first.submit("test", user_id="owner")
    with ThreadPoolExecutor() as pool:
        future = pool.submit(first.run, job_id)
        try:
            assert entered.wait(5)
            assert second.run(job_id) is False
        finally:
            release.set()
        assert future.result()
    assert calls == [job_id]


def test_expired_lease_recovers_and_stale_worker_cannot_overwrite(db):
    jobs = runner(db, lambda job, payload: {"recovered": True})
    job_id = jobs.submit("test", user_id="owner")
    db.execute("UPDATE jobs SET status='running' WHERE id=?", (job_id,))
    db.execute(
        "INSERT INTO job_leases (job_id,token,expires_at,attempts) VALUES (?, 'old', 0, 1)",
        (job_id,),
    )
    assert jobs.run_pending() == 1
    jobs._finish(job_id, "old", "failed", error="stale")
    assert jobs.get(job_id, "owner")["status"] == "completed"


def test_retries_are_bounded_and_errors_are_private(db):
    def fail(job, payload):
        raise RuntimeError("secret-token-and-photo-reference")

    jobs = runner(db, fail)
    job_id = jobs.submit("test", user_id="owner", payload={"secret": "private"})
    for attempt in range(3):
        assert jobs.run_pending() == 1
        db.execute("UPDATE job_leases SET expires_at=0 WHERE job_id=?", (job_id,))
    result = jobs.get(job_id, "owner")
    assert result["status"] == "failed"
    assert result["payload"] == {}
    assert "secret" not in result["error"]
    assert jobs.run_pending() == 0
    assert jobs.get(job_id, "other") is None


def test_unowned_jobs_are_not_visible_to_users(db):
    jobs = runner(db, lambda job, payload: {})
    job_id = jobs.submit("test")
    assert jobs.get(job_id, "owner") is None
