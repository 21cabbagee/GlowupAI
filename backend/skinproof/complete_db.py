"""Compatibility name for the extended product database."""

from .config import Settings
from .full_db import FULL_SCHEMA, FullDatabase
from .postgres_db import PostgresDatabase


def build_full_database(settings: Settings):
    """Build the production database or the explicit SQLite test fallback."""
    if settings.database_url:
        return PostgresDatabase(
            settings.database_url,
            min_size=settings.database_pool_min_size,
            max_size=settings.database_pool_max_size,
            connect_timeout=settings.database_connect_timeout,
        )
    return FullDatabase(settings.db_path)

__all__ = ["FULL_SCHEMA", "FullDatabase", "PostgresDatabase", "build_full_database"]
