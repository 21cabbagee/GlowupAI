from __future__ import annotations

import re
from contextlib import contextmanager
from pathlib import Path
from typing import Any, Iterator


_SQLITE_PARAMETER = re.compile(r"\?")
_SQLITE_NOW = re.compile(r"datetime\('now'\)", re.IGNORECASE)
_SQLITE_IS_PARAMETER = re.compile(r"\bIS\s+\?", re.IGNORECASE)


class PostgresDatabase:
    """Small database adapter used by the existing service layer.

    The domain services use question-mark parameters because the test
    database is SQLite. This adapter translates those parameters at the
    boundary and keeps the service code database-agnostic. Each operation
    borrows a pooled connection, so synchronous FastAPI workers do not share
    a connection.
    """

    backend = "postgresql"

    def __init__(
        self,
        url: str,
        *,
        min_size: int = 1,
        max_size: int = 10,
        connect_timeout: int = 10,
    ) -> None:
        try:
            from psycopg.rows import dict_row
            from psycopg_pool import ConnectionPool
        except ImportError as exc:  # pragma: no cover - exercised in deployment
            raise RuntimeError(
                "PostgreSQL support requires the psycopg[binary,pool] dependency"
            ) from exc

        self._dict_row = dict_row
        self.pool = ConnectionPool(
            conninfo=url,
            kwargs={"connect_timeout": connect_timeout},
            min_size=min_size,
            max_size=max(max_size, min_size),
            open=False,
        )
        self.pool.open()
        self.pool.wait()
        self._migrate()

    @staticmethod
    def _sql(sql: str) -> str:
        # Keep the compatibility layer deliberately small and predictable.
        # The application only uses these SQLite spellings in shared queries.
        sql = _SQLITE_NOW.sub("CURRENT_TIMESTAMP", sql)
        sql = _SQLITE_IS_PARAMETER.sub("IS NOT DISTINCT FROM ?", sql)
        return _SQLITE_PARAMETER.sub("%s", sql)

    @staticmethod
    def _params(params: tuple[Any, ...] | list[Any] | None) -> tuple[Any, ...]:
        return tuple(params or ())

    def execute(self, sql: str, params: tuple[Any, ...] = ()):
        with self.pool.connection() as connection:
            with connection.cursor() as cursor:
                cursor.execute(self._sql(sql), self._params(params))
                result = _ExecutionResult(cursor.rowcount)
            connection.commit()
        return result

    def fetchone(self, sql: str, params: tuple[Any, ...] = ()) -> dict[str, Any] | None:
        with self.pool.connection() as connection:
            with connection.cursor(row_factory=self._dict_row) as cursor:
                cursor.execute(self._sql(sql), self._params(params))
                return cursor.fetchone()

    def fetchall(self, sql: str, params: tuple[Any, ...] = ()) -> list[dict[str, Any]]:
        with self.pool.connection() as connection:
            with connection.cursor(row_factory=self._dict_row) as cursor:
                cursor.execute(self._sql(sql), self._params(params))
                return list(cursor.fetchall())

    @contextmanager
    def transaction(self) -> Iterator[Any]:
        with self.pool.connection() as connection:
            try:
                yield connection
                connection.commit()
            except Exception:
                connection.rollback()
                raise

    def healthcheck(self) -> bool:
        return self.fetchone("SELECT 1 AS ok") == {"ok": 1}

    def _migrate(self) -> None:
        migration_dir = Path(__file__).parent / "migrations"
        with self.pool.connection() as connection:
            with connection.cursor() as cursor:
                cursor.execute(
                    """
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version TEXT PRIMARY KEY,
                        applied_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
                    )
                    """
                )
                cursor.execute("SELECT version FROM schema_migrations")
                applied = {row[0] for row in cursor.fetchall()}
                for migration in sorted(migration_dir.glob("*.sql")):
                    if migration.name in applied:
                        continue
                    statements = [
                        statement.strip()
                        for statement in migration.read_text(encoding="utf-8").split(";")
                        if statement.strip()
                    ]
                    for statement in statements:
                        cursor.execute(statement)
                    cursor.execute(
                        "INSERT INTO schema_migrations (version) VALUES (%s)",
                        (migration.name,),
                    )
            connection.commit()

    def close(self) -> None:
        self.pool.close()


class _ExecutionResult:
    def __init__(self, rowcount: int) -> None:
        self.rowcount = rowcount
