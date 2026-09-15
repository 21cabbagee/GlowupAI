from __future__ import annotations

import re
from collections.abc import Iterator
from contextlib import contextmanager
from pathlib import Path
from typing import Any

_SQLITE_PARAMETER = re.compile(r"\?")
_SQLITE_NOW = re.compile(r"datetime\('now'\)", re.IGNORECASE)
_SQLITE_IS_PARAMETER = re.compile(r"\bIS\s+\?", re.IGNORECASE)
_DOLLAR_QUOTE = re.compile(r"\$[A-Za-z_][A-Za-z0-9_]*\$|\$\$")


def _split_migration_sql(sql: str) -> list[str]:
    """Split SQL statements without breaking PostgreSQL dollar-quoted blocks."""
    statements: list[str] = []
    current: list[str] = []
    quote: str | None = None
    dollar_quote: str | None = None
    index = 0
    while index < len(sql):
        if dollar_quote:
            if sql.startswith(dollar_quote, index):
                current.append(dollar_quote)
                index += len(dollar_quote)
                dollar_quote = None
            else:
                current.append(sql[index])
                index += 1
            continue
        character = sql[index]
        if quote:
            current.append(character)
            if character == quote:
                if index + 1 < len(sql) and sql[index + 1] == quote:
                    current.append(sql[index + 1])
                    index += 2
                    continue
                quote = None
            index += 1
            continue
        if character in {"'", '"'}:
            quote = character
            current.append(character)
            index += 1
            continue
        if character == "$":
            match = _DOLLAR_QUOTE.match(sql, index)
            if match:
                dollar_quote = match.group(0)
                current.append(dollar_quote)
                index = match.end()
                continue
        if character == ";":
            statement = "".join(current).strip()
            if statement:
                statements.append(statement)
            current = []
        else:
            current.append(character)
        index += 1
    statement = "".join(current).strip()
    if statement:
        statements.append(statement)
    return statements


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
        pool_timeout: int = 30,
        statement_timeout: int = 30000,
    ) -> None:
        try:
            from psycopg.rows import dict_row
            from psycopg_pool import ConnectionPool
        except ImportError as exc:  # pragma: no cover - exercised in deployment
            raise RuntimeError(
                "PostgreSQL support requires the psycopg[binary,pool] dependency",
            ) from exc

        self._dict_row = dict_row

        # Build connection options
        conn_kwargs = {
            "connect_timeout": connect_timeout,
            "options": f"-c statement_timeout={statement_timeout}",
            # Supabase's transaction pooler may route consecutive queries to
            # different PostgreSQL sessions. psycopg's auto-prepared
            # statements then become missing or duplicate (_pg3_*), breaking
            # otherwise successful capture previews and history reads.
            "prepare_threshold": None,
        }

        self.pool = ConnectionPool(
            conninfo=url,
            kwargs=conn_kwargs,
            min_size=min_size,
            max_size=max(max_size, min_size),
            timeout=pool_timeout,
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
        with (
            self.pool.connection() as connection,
            connection.cursor(row_factory=self._dict_row) as cursor,
        ):
            cursor.execute(self._sql(sql), self._params(params))
            result: dict[str, Any] | None = cursor.fetchone()
            return result

    def fetchall(self, sql: str, params: tuple[Any, ...] = ()) -> list[dict[str, Any]]:
        with (
            self.pool.connection() as connection,
            connection.cursor(row_factory=self._dict_row) as cursor,
        ):
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

    def count_tables(self) -> int:
        """Count the number of user-created tables in the database."""
        result = self.fetchone(
            "SELECT COUNT(*) as count FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'"
        )
        return result["count"] if result else 0

    def _migrate(self) -> None:
        migration_dir = Path(__file__).parent / "migrations"
        with self.pool.connection() as connection:
            with connection.cursor() as cursor:
                cursor.execute("""
                    CREATE TABLE IF NOT EXISTS schema_migrations (
                        version TEXT PRIMARY KEY,
                        applied_at TEXT NOT NULL DEFAULT (CURRENT_TIMESTAMP::text)
                    )
                    """)
                cursor.execute("SELECT version FROM schema_migrations")
                applied = {row[0] for row in cursor.fetchall()}
                for migration in sorted(migration_dir.glob("*.sql")):
                    if migration.name in applied:
                        continue
                    statements = _split_migration_sql(
                        migration.read_text(encoding="utf-8"),
                    )
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
