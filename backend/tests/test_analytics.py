from __future__ import annotations

from datetime import datetime, timezone

from glowupai.analytics import AnalyticsTracker


class PostgresDateRows:
    def fetchall(self, query, params):
        return [{"date": datetime.now(timezone.utc).date()}]


def test_user_streak_accepts_postgres_native_date_values():
    """PostgreSQL DATE() values are dates, unlike SQLite's string values."""
    tracker = object.__new__(AnalyticsTracker)
    tracker.db = PostgresDateRows()
    assert tracker.get_user_streak("user-1") == 1
