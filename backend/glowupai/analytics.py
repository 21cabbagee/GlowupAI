"""Analytics event tracking for user behavior and key metrics."""

from __future__ import annotations

import json
import logging
from datetime import UTC, datetime, timedelta
from typing import Any

from .db import Database

logger = logging.getLogger(__name__)


class AnalyticsTracker:
    """Track analytics events for user behavior and product metrics."""

    def __init__(self, db: Database) -> None:
        self.db = db
        self._ensure_schema()

    def _ensure_schema(self) -> None:
        """Create analytics_events table if it doesn't exist."""
        schema = """
        CREATE TABLE IF NOT EXISTS analytics_events (
            id TEXT PRIMARY KEY,
            user_id TEXT REFERENCES users(id) ON DELETE CASCADE,
            event_type TEXT NOT NULL,
            event_data TEXT NOT NULL DEFAULT '{}',
            created_at TEXT NOT NULL DEFAULT (datetime('now'))
        );

        CREATE INDEX IF NOT EXISTS idx_analytics_user_type
        ON analytics_events(user_id, event_type, created_at);

        CREATE INDEX IF NOT EXISTS idx_analytics_type_time
        ON analytics_events(event_type, created_at);
        """
        with self.db._lock:
            self.db.connection.executescript(schema)
            self.db.connection.commit()

    def track_event(
        self,
        event_type: str,
        user_id: str | None = None,
        event_data: dict[str, Any] | None = None,
    ) -> str:
        """Track an analytics event.

        Args:
            event_type: Type of event (e.g., 'user_signup', 'capture_created')
            user_id: User ID (optional for anonymous events)
            event_data: Additional event data as dictionary

        Returns:
            Event ID
        """
        import uuid

        event_id = f"evt_{uuid.uuid4().hex[:12]}"
        event_data_json = json.dumps(event_data or {})

        self.db.execute(
            """
            INSERT INTO analytics_events (id, user_id, event_type, event_data, created_at)
            VALUES (?, ?, ?, ?, datetime('now'))
            """,
            (event_id, user_id, event_type, event_data_json),
        )

        logger.info(
            f"Analytics event tracked: {event_type}",
            extra={
                "event_type": event_type,
                "user_id": user_id,
                "event_id": event_id,
            },
        )

        return event_id

    def track_user_signup(
        self,
        user_id: str,
        method: str,
        email: str | None = None,
    ) -> str:
        """Track user signup event.

        Args:
            user_id: User ID
            method: Signup method ('google' or 'email')
            email: User email (optional)

        Returns:
            Event ID
        """
        return self.track_event(
            "user_signup",
            user_id=user_id,
            event_data={"method": method, "email": email},
        )

    def track_capture_created(
        self,
        user_id: str,
        capture_id: str,
        is_baseline: bool = False,
        metrics: dict[str, Any] | None = None,
    ) -> str:
        """Track capture creation event.

        Args:
            user_id: User ID
            capture_id: Capture ID
            is_baseline: Whether this is a baseline capture
            metrics: Capture metrics (optional)

        Returns:
            Event ID
        """
        return self.track_event(
            "capture_created",
            user_id=user_id,
            event_data={
                "capture_id": capture_id,
                "is_baseline": is_baseline,
                "metrics": metrics,
            },
        )

    def track_comparison_viewed(
        self,
        user_id: str,
        capture_ids: list[str] | None = None,
    ) -> str:
        """Track comparison view event.

        Args:
            user_id: User ID
            capture_ids: List of capture IDs being compared (optional)

        Returns:
            Event ID
        """
        return self.track_event(
            "comparison_viewed",
            user_id=user_id,
            event_data={"capture_ids": capture_ids},
        )

    def track_baseline_set(self, user_id: str, capture_id: str) -> str:
        """Track baseline setting event.

        Args:
            user_id: User ID
            capture_id: Capture ID set as baseline

        Returns:
            Event ID
        """
        return self.track_event(
            "baseline_set",
            user_id=user_id,
            event_data={"capture_id": capture_id},
        )

    def track_streak_milestone(self, user_id: str, days: int) -> str:
        """Track streak milestone event.

        Args:
            user_id: User ID
            days: Number of days in streak (3, 7, 14, 30, etc.)

        Returns:
            Event ID
        """
        return self.track_event(
            "streak_milestone",
            user_id=user_id,
            event_data={"days": days},
        )

    def get_user_events(
        self,
        user_id: str,
        event_type: str | None = None,
        limit: int = 100,
    ) -> list[dict[str, Any]]:
        """Get analytics events for a user.

        Args:
            user_id: User ID
            event_type: Filter by event type (optional)
            limit: Maximum number of events to return

        Returns:
            List of event dictionaries
        """
        if event_type:
            rows = self.db.fetchall(
                """
                SELECT id, event_type, event_data, created_at
                FROM analytics_events
                WHERE user_id = ? AND event_type = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (user_id, event_type, limit),
            )
        else:
            rows = self.db.fetchall(
                """
                SELECT id, event_type, event_data, created_at
                FROM analytics_events
                WHERE user_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                (user_id, limit),
            )

        return [
            {
                "id": row["id"],
                "event_type": row["event_type"],
                "event_data": json.loads(row["event_data"]),
                "created_at": row["created_at"],
            }
            for row in rows
        ]

    def get_event_counts(
        self,
        days: int = 7,
        event_type: str | None = None,
    ) -> dict[str, int]:
        """Get event counts for the last N days.

        Args:
            days: Number of days to look back
            event_type: Filter by event type (optional)

        Returns:
            Dictionary mapping event types to counts
        """
        since = (datetime.now(UTC) - timedelta(days=days)).isoformat()

        if event_type:
            rows = self.db.fetchall(
                """
                SELECT event_type, COUNT(*) as count
                FROM analytics_events
                WHERE created_at >= ? AND event_type = ?
                GROUP BY event_type
                """,
                (since, event_type),
            )
        else:
            rows = self.db.fetchall(
                """
                SELECT event_type, COUNT(*) as count
                FROM analytics_events
                WHERE created_at >= ?
                GROUP BY event_type
                ORDER BY count DESC
                """,
                (since,),
            )

        return {row["event_type"]: row["count"] for row in rows}

    def get_daily_stats(self, days: int = 30) -> list[dict[str, Any]]:
        """Get daily aggregated stats for the last N days.

        Args:
            days: Number of days to look back

        Returns:
            List of daily stat dictionaries
        """
        since = (datetime.now(UTC) - timedelta(days=days)).isoformat()

        rows = self.db.fetchall(
            """
            SELECT
                DATE(created_at) as date,
                event_type,
                COUNT(*) as count
            FROM analytics_events
            WHERE created_at >= ?
            GROUP BY DATE(created_at), event_type
            ORDER BY date DESC, count DESC
            """,
            (since,),
        )

        # Group by date
        daily_stats = {}
        for row in rows:
            date = row["date"]
            if date not in daily_stats:
                daily_stats[date] = {"date": date, "events": {}}
            daily_stats[date]["events"][row["event_type"]] = row["count"]

        return list(daily_stats.values())

    def get_user_streak(self, user_id: str) -> int:
        """Calculate user's current capture streak in days.

        Args:
            user_id: User ID

        Returns:
            Number of consecutive days with captures
        """
        # Get distinct dates with captures
        rows = self.db.fetchall(
            """
            SELECT DISTINCT DATE(created_at) as date
            FROM analytics_events
            WHERE user_id = ? AND event_type = 'capture_created'
            ORDER BY date DESC
            """,
            (user_id,),
        )

        if not rows:
            return 0

        # Check for consecutive days
        streak = 0
        today = datetime.now(UTC).date()

        for row in rows:
            capture_date = datetime.fromisoformat(row["date"]).date()
            expected_date = today - timedelta(days=streak)

            if capture_date == expected_date:
                streak += 1
            else:
                break

        return streak

    def get_analytics_summary(self, days: int = 7) -> dict[str, Any]:
        """Get comprehensive analytics summary.

        Args:
            days: Number of days to look back

        Returns:
            Summary dictionary with key metrics
        """
        since = (datetime.now(UTC) - timedelta(days=days)).isoformat()

        # Total events
        total_events_row = self.db.fetchone(
            "SELECT COUNT(*) as count FROM analytics_events WHERE created_at >= ?",
            (since,),
        )
        total_events = total_events_row["count"] if total_events_row else 0

        # Unique users
        unique_users_row = self.db.fetchone(
            """
            SELECT COUNT(DISTINCT user_id) as count
            FROM analytics_events
            WHERE created_at >= ? AND user_id IS NOT NULL
            """,
            (since,),
        )
        unique_users = unique_users_row["count"] if unique_users_row else 0

        # Event counts by type
        event_counts = self.get_event_counts(days)

        # Top users by activity
        top_users = self.db.fetchall(
            """
            SELECT user_id, COUNT(*) as event_count
            FROM analytics_events
            WHERE created_at >= ? AND user_id IS NOT NULL
            GROUP BY user_id
            ORDER BY event_count DESC
            LIMIT 10
            """,
            (since,),
        )

        return {
            "period_days": days,
            "total_events": total_events,
            "unique_users": unique_users,
            "event_counts": event_counts,
            "top_users": [
                {"user_id": row["user_id"], "event_count": row["event_count"]}
                for row in top_users
            ],
            "generated_at": datetime.now(UTC).isoformat(),
        }
