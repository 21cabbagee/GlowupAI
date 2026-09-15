"""Atomic provider quota and spend reservations.

Reservations are made before a network call and reconciled afterwards.  A
reservation with unknown provider usage remains ``ambiguous`` so a timeout
cannot accidentally make the next request overspend the configured budget.
"""

from __future__ import annotations

from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime, timezone
from decimal import Decimal
from typing import Any, Iterator
from uuid import uuid4
from zoneinfo import ZoneInfo

UTC = timezone.utc


_ACTIVE_STATES = ("reserved", "ambiguous", "reconciled")


@dataclass(frozen=True)
class QuotaLimits:
    max_requests: int | None = None
    max_tokens: int | None = None
    max_cost_usd: float | None = None
    user_max_requests: int | None = None
    user_max_tokens: int | None = None


@dataclass(frozen=True)
class UsageReservation:
    id: str
    provider: str
    project_alias: str
    model_id: str
    user_id: str | None
    task: str
    request_identity: str
    quota_window: str
    reserved_tokens: int
    estimated_cost_usd: float
    attempt: int
    state: str
    provider_request_id: str | None = None


class QuotaExceeded(RuntimeError):
    def __init__(self, provider: str, quota_window: str, reason: str) -> None:
        super().__init__(f"{provider} quota exhausted: {reason}")
        self.provider = provider
        self.quota_window = quota_window
        self.reason = reason


def utc_day_window(at: datetime | None = None) -> str:
    at = at or datetime.now(UTC)
    return f"day:{at.astimezone(UTC).date().isoformat()}"


def pacific_day_window(at: datetime | None = None) -> str:
    at = at or datetime.now(UTC)
    return f"day:{at.astimezone(ZoneInfo('America/Los_Angeles')).date().isoformat()}"


def utc_month_window(at: datetime | None = None) -> str:
    at = at or datetime.now(UTC)
    current = at.astimezone(UTC)
    return f"month:{current.year:04d}-{current.month:02d}"


class AIQuotaManager:
    def __init__(self, db: Any) -> None:
        self.db = db

    @contextmanager
    def _atomic(self) -> Iterator[Any]:
        """Acquire the bucket lock before reading or changing usage totals."""

        with self.db.transaction() as connection:
            if getattr(self.db, "backend", "sqlite") == "sqlite":
                # The FullDatabase connection is shared by worker threads.  A
                # write lock makes the read/check/insert/update sequence one
                # indivisible operation.  ``transaction`` commits/rolls back.
                connection.execute("BEGIN IMMEDIATE")
            yield connection

    def _execute(self, connection: Any, sql: str, params: tuple[Any, ...] = ()) -> Any:
        if getattr(self.db, "backend", "sqlite") == "postgresql":
            sql = self.db._sql(sql)
        return connection.execute(sql, params)

    def _fetchone(self, connection: Any, sql: str, params: tuple[Any, ...] = ()) -> Any:
        return self._execute(connection, sql, params).fetchone()

    @staticmethod
    def _field(row: Any, index: int, key: str) -> Any:
        if isinstance(row, dict):
            return row[key]
        try:
            return row[key]
        except (IndexError, KeyError, TypeError):
            return row[index]

    @staticmethod
    def _number(value: Any) -> float:
        if value is None:
            return 0.0
        if isinstance(value, Decimal):
            return float(value)
        return float(value)

    def reserve(
        self,
        *,
        provider: str,
        project_alias: str,
        model_id: str,
        user_id: str | None,
        task: str,
        request_identity: str,
        quota_window: str,
        estimated_tokens: int,
        estimated_cost_usd: float = 0.0,
        attempt: int = 1,
        limits: QuotaLimits | None = None,
        reservation_id: str | None = None,
    ) -> UsageReservation:
        """Atomically reserve a request's conservative maximum usage.

        Repeating the same provider/request/attempt identity returns the
        existing reservation, which prevents a replayed worker from spending
        the same allowance twice.
        """

        if not provider or not project_alias or not model_id or not task:
            raise ValueError("provider, project, model and task are required")
        if not request_identity:
            raise ValueError("request_identity is required")
        if attempt < 1:
            raise ValueError("attempt must be positive")
        if estimated_tokens < 1:
            raise ValueError("estimated_tokens must be positive")
        if estimated_cost_usd < 0:
            raise ValueError("estimated_cost_usd cannot be negative")
        limits = limits or QuotaLimits()
        reservation_id = reservation_id or str(uuid4())

        with self._atomic() as connection:
            # PostgreSQL can have several pooled connections, so the bucket
            # row alone is not enough to serialize duplicate identities (and
            # user limits may span multiple buckets/models).  Transaction
            # scoped advisory locks close both races without adding a schema
            # lock table. SQLite is already serialized by Database._lock and
            # BEGIN IMMEDIATE.
            if getattr(self.db, "backend", "sqlite") == "postgresql":
                self._execute(
                    connection,
                    "SELECT pg_advisory_xact_lock(hashtext(?))",
                    (f"request:{provider}:{request_identity}:{attempt}",),
                )
            existing = self._fetchone(
                connection,
                "SELECT * FROM ai_usage_reservations "
                "WHERE provider=? AND request_identity=? AND attempt=?",
                (provider, request_identity, attempt),
            )
            if existing is not None:
                return self._reservation(existing)

            self._execute(
                connection,
                "INSERT INTO ai_usage_buckets "
                "(provider, project_alias, model_id, quota_window) VALUES (?, ?, ?, ?) "
                "ON CONFLICT(provider, project_alias, model_id, quota_window) DO NOTHING",
                (provider, project_alias, model_id, quota_window),
            )
            lock_sql = (
                "SELECT reserved_tokens, reserved_requests, reserved_cost_usd "
                "FROM ai_usage_buckets WHERE provider=? AND project_alias=? "
                "AND model_id=? AND quota_window=?"
            )
            if getattr(self.db, "backend", "sqlite") == "postgresql":
                lock_sql += " FOR UPDATE"
            bucket = self._fetchone(
                connection,
                lock_sql,
                (provider, project_alias, model_id, quota_window),
            )
            if bucket is None:
                raise RuntimeError("usage bucket could not be created")

            # A concurrent request with the same identity may have committed
            # while this worker was waiting for the bucket row lock.
            existing = self._fetchone(
                connection,
                "SELECT * FROM ai_usage_reservations "
                "WHERE provider=? AND request_identity=? AND attempt=?",
                (provider, request_identity, attempt),
            )
            if existing is not None:
                return self._reservation(existing)

            bucket_tokens = int(self._field(bucket, 0, "reserved_tokens"))
            bucket_requests = int(self._field(bucket, 1, "reserved_requests"))
            bucket_cost = self._number(self._field(bucket, 2, "reserved_cost_usd"))
            if (
                limits.max_requests is not None
                and bucket_requests + 1 > limits.max_requests
            ):
                raise QuotaExceeded(provider, quota_window, "request limit")
            if (
                limits.max_tokens is not None
                and bucket_tokens + estimated_tokens > limits.max_tokens
            ):
                raise QuotaExceeded(provider, quota_window, "token limit")
            if (
                limits.max_cost_usd is not None
                and bucket_cost + estimated_cost_usd > limits.max_cost_usd
            ):
                raise QuotaExceeded(provider, quota_window, "spend limit")

            if user_id and (
                limits.user_max_requests is not None
                or limits.user_max_tokens is not None
            ):
                if getattr(self.db, "backend", "sqlite") == "postgresql":
                    self._execute(
                        connection,
                        "SELECT pg_advisory_xact_lock(hashtext(?))",
                        (f"user:{provider}:{user_id}:{quota_window}",),
                    )
                state_sql = ",".join("?" for _ in _ACTIVE_STATES)
                row = self._fetchone(
                    connection,
                    "SELECT COUNT(*), COALESCE(SUM(reserved_tokens), 0) "  # nosec B608
                    "FROM ai_usage_reservations WHERE user_id=? AND provider=? "
                    "AND quota_window=? AND state IN (" + state_sql + ")",
                    (user_id, provider, quota_window, *_ACTIVE_STATES),
                )
                user_requests = int(self._field(row, 0, "count"))
                user_tokens = int(self._field(row, 1, "coalesce"))
                if (
                    limits.user_max_requests is not None
                    and user_requests + 1 > limits.user_max_requests
                ):
                    raise QuotaExceeded(provider, quota_window, "user request limit")
                if (
                    limits.user_max_tokens is not None
                    and user_tokens + estimated_tokens > limits.user_max_tokens
                ):
                    raise QuotaExceeded(provider, quota_window, "user token limit")

            self._execute(
                connection,
                "INSERT INTO ai_usage_reservations "
                "(id, provider, project_alias, model_id, user_id, task, request_identity, "
                "quota_window, reserved_tokens, estimated_cost_usd, attempt, state) "
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'reserved')",
                (
                    reservation_id,
                    provider,
                    project_alias,
                    model_id,
                    user_id,
                    task,
                    request_identity,
                    quota_window,
                    estimated_tokens,
                    estimated_cost_usd,
                    attempt,
                ),
            )
            self._execute(
                connection,
                "UPDATE ai_usage_buckets SET reserved_tokens=reserved_tokens+?, "
                "reserved_requests=reserved_requests+1, reserved_cost_usd=reserved_cost_usd+?, "
                "updated_at=datetime('now') WHERE provider=? AND project_alias=? "
                "AND model_id=? AND quota_window=?",
                (
                    estimated_tokens,
                    estimated_cost_usd,
                    provider,
                    project_alias,
                    model_id,
                    quota_window,
                ),
            )
            return UsageReservation(
                reservation_id,
                provider,
                project_alias,
                model_id,
                user_id,
                task,
                request_identity,
                quota_window,
                estimated_tokens,
                float(estimated_cost_usd),
                attempt,
                "reserved",
            )

    def reconcile(
        self,
        reservation_id: str,
        *,
        actual_input_tokens: int,
        actual_output_tokens: int,
        actual_cost_usd: float | None = None,
        provider_request_id: str | None = None,
    ) -> UsageReservation | None:
        if actual_input_tokens < 0 or actual_output_tokens < 0:
            raise ValueError("actual token counts cannot be negative")
        actual_tokens = actual_input_tokens + actual_output_tokens
        with self._atomic() as connection:
            lock_sql = "SELECT * FROM ai_usage_reservations WHERE id=?"
            if getattr(self.db, "backend", "sqlite") == "postgresql":
                lock_sql += " FOR UPDATE"
            row = self._fetchone(connection, lock_sql, (reservation_id,))
            if row is None:
                return None
            current = self._reservation(row)
            if current.state not in {"reserved", "ambiguous"}:
                return current
            cost = (
                current.estimated_cost_usd
                if actual_cost_usd is None
                else max(0.0, actual_cost_usd)
            )
            self._execute(
                connection,
                "UPDATE ai_usage_reservations SET reserved_tokens=?, actual_input_tokens=?, "
                "actual_output_tokens=?, actual_cost_usd=?, state='reconciled', "
                "provider_request_id=?, reconciled_at=datetime('now') WHERE id=?",
                (
                    actual_tokens,
                    actual_input_tokens,
                    actual_output_tokens,
                    cost,
                    provider_request_id,
                    reservation_id,
                ),
            )
            nonnegative = (
                "GREATEST"
                if getattr(self.db, "backend", "sqlite") == "postgresql"
                else "MAX"
            )
            self._execute(
                connection,
                f"UPDATE ai_usage_buckets SET reserved_tokens={nonnegative}(0, reserved_tokens-?+?), "  # nosec B608
                f"reserved_cost_usd={nonnegative}(0, reserved_cost_usd-?+?), updated_at=datetime('now') "
                "WHERE provider=? AND project_alias=? AND model_id=? AND quota_window=?",
                (
                    current.reserved_tokens,
                    actual_tokens,
                    current.estimated_cost_usd,
                    cost,
                    current.provider,
                    current.project_alias,
                    current.model_id,
                    current.quota_window,
                ),
            )
            return UsageReservation(
                current.id,
                current.provider,
                current.project_alias,
                current.model_id,
                current.user_id,
                current.task,
                current.request_identity,
                current.quota_window,
                actual_tokens,
                cost,
                current.attempt,
                "reconciled",
                provider_request_id,
            )

    def release(self, reservation_id: str, *, error_code: str | None = None) -> bool:
        """Release a reservation only when the provider was not invoked."""

        with self._atomic() as connection:
            lock_sql = "SELECT * FROM ai_usage_reservations WHERE id=?"
            if getattr(self.db, "backend", "sqlite") == "postgresql":
                lock_sql += " FOR UPDATE"
            row = self._fetchone(connection, lock_sql, (reservation_id,))
            if row is None:
                return False
            current = self._reservation(row)
            if current.state != "reserved":
                return current.state == "released"
            self._execute(
                connection,
                "UPDATE ai_usage_reservations SET state='released', error_code=?, "
                "reconciled_at=datetime('now') WHERE id=?",
                (error_code, reservation_id),
            )
            self._decrement_bucket(connection, current, include_request=True)
            return True

    def mark_ambiguous(
        self, reservation_id: str, *, error_code: str = "provider_timeout"
    ) -> bool:
        """Retain the reservation when the provider may have processed it."""

        with self._atomic() as connection:
            result = self._execute(
                connection,
                "UPDATE ai_usage_reservations SET state='ambiguous', error_code=? "
                "WHERE id=? AND state='reserved'",
                (error_code, reservation_id),
            )
            return bool(result.rowcount)

    def fail_without_call(self, reservation_id: str, *, error_code: str) -> bool:
        """Record a pre-call failure and release its reservation."""

        with self._atomic() as connection:
            lock_sql = "SELECT * FROM ai_usage_reservations WHERE id=?"
            if getattr(self.db, "backend", "sqlite") == "postgresql":
                lock_sql += " FOR UPDATE"
            row = self._fetchone(connection, lock_sql, (reservation_id,))
            if row is None:
                return False
            current = self._reservation(row)
            if current.state != "reserved":
                return current.state == "failed"
            self._execute(
                connection,
                "UPDATE ai_usage_reservations SET state='failed', error_code=?, "
                "reconciled_at=datetime('now') WHERE id=?",
                (error_code, reservation_id),
            )
            self._decrement_bucket(connection, current, include_request=True)
            return True

    def _decrement_bucket(
        self, connection: Any, current: UsageReservation, *, include_request: bool
    ) -> None:
        nonnegative = (
            "GREATEST"
            if getattr(self.db, "backend", "sqlite") == "postgresql"
            else "MAX"
        )
        self._execute(
            connection,
            f"UPDATE ai_usage_buckets SET reserved_tokens={nonnegative}(0, reserved_tokens-?), "  # nosec B608
            f"reserved_requests={nonnegative}(0, reserved_requests-?), "
            f"reserved_cost_usd={nonnegative}(0, reserved_cost_usd-?), updated_at=datetime('now') "
            "WHERE provider=? AND project_alias=? AND model_id=? AND quota_window=?",
            (
                current.reserved_tokens,
                1 if include_request else 0,
                current.estimated_cost_usd,
                current.provider,
                current.project_alias,
                current.model_id,
                current.quota_window,
            ),
        )

    def bucket_usage(
        self, provider: str, project_alias: str, model_id: str, quota_window: str
    ) -> dict[str, Any]:
        row = self.db.fetchone(
            "SELECT * FROM ai_usage_buckets WHERE provider=? AND project_alias=? "
            "AND model_id=? AND quota_window=?",
            (provider, project_alias, model_id, quota_window),
        )
        return (
            dict(row)
            if row
            else {
                "provider": provider,
                "project_alias": project_alias,
                "model_id": model_id,
                "quota_window": quota_window,
                "reserved_tokens": 0,
                "reserved_requests": 0,
                "reserved_cost_usd": 0,
            }
        )

    def _reservation(self, row: Any) -> UsageReservation:
        return UsageReservation(
            id=self._field(row, 0, "id"),
            provider=self._field(row, 1, "provider"),
            project_alias=self._field(row, 2, "project_alias"),
            model_id=self._field(row, 3, "model_id"),
            user_id=self._field(row, 4, "user_id"),
            task=self._field(row, 5, "task"),
            request_identity=self._field(row, 6, "request_identity"),
            quota_window=self._field(row, 7, "quota_window"),
            reserved_tokens=int(self._field(row, 8, "reserved_tokens")),
            estimated_cost_usd=self._number(self._field(row, 11, "estimated_cost_usd")),
            attempt=int(self._field(row, 13, "attempt")),
            state=self._field(row, 14, "state"),
            provider_request_id=self._field(row, 15, "provider_request_id"),
        )


__all__ = [
    "AIQuotaManager",
    "QuotaExceeded",
    "QuotaLimits",
    "UsageReservation",
    "pacific_day_window",
    "utc_day_window",
    "utc_month_window",
]
