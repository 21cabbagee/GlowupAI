from __future__ import annotations

import json
import logging
import sqlite3
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from .service import GlowupAIService, now_iso, row_dict

logger = logging.getLogger(__name__)

VERTICALS = ("skin",)


def uid() -> str:
    """Generate a unique identifier string.

    Returns:
        A UUID4 string representation.
    """
    return str(uuid.uuid4())


def dump(value: Any) -> str:
    """Serialize a Python value to a compact JSON string.

    Args:
        value: Any JSON-serializable Python object.

    Returns:
        Compact JSON string with sorted keys.
    """
    return json.dumps(value, separators=(",", ":"), sort_keys=True)


def load(value: Any, default: Optional[Any] = None) -> Any:
    """Deserialize a JSON string to a Python value.

    Args:
        value: JSON string to parse.
        default: Default value to return if parsing fails. Defaults to empty dict.

    Returns:
        Parsed Python object, or the default value if parsing fails.
    """
    try:
        return json.loads(value)
    except (TypeError, ValueError):
        return default if default is not None else {}


class UserService:
    """User CRUD operations and profile management."""

    def __init__(self, db: Any, parent_service: GlowupAIService) -> None:
        """Initialize the UserService.

        Args:
            db: Database connection instance.
            parent_service: Parent GlowupAIService instance for delegating core operations.
        """
        self.db = db
        self.parent = parent_service

    def _audit(
        self,
        action: str,
        subject_type: Optional[str] = None,
        subject_id: Optional[str] = None,
        actor_id: Optional[str] = None,
        metadata: Optional[Dict[str, Any]] = None,
    ) -> None:
        """Record an audit log entry for user actions.

        Args:
            action: The action being audited (e.g., 'user_created', 'profile_updated').
            subject_type: Type of entity being acted upon (e.g., 'user').
            subject_id: ID of the entity being acted upon.
            actor_id: ID of the user performing the action.
            metadata: Additional contextual information to log.
        """
        self.db.execute(
            "INSERT INTO audit_log (id, actor_type, actor_id, action, subject_type, subject_id, metadata_json) VALUES (?, 'user', ?, ?, ?, ?, ?)",
            (uid(), actor_id, action, subject_type, subject_id, dump(metadata or {})),
        )

    def create_user(self, skin_type: Optional[str] = None) -> Dict[str, Any]:
        """Create a new user with default entitlements and appearance profiles.

        Creates a user record, initializes a free plan entitlement, and creates
        appearance profiles for all supported verticals (currently 'skin').

        Args:
            skin_type: Optional skin type classification for the user.

        Returns:
            Complete user profile dictionary including user data, appearance profiles,
            entitlement, and experience profile.
        """
        # Call base service method directly to avoid infinite recursion
        # Use super() to bypass the overridden method in CompleteGlowupAIService
        user = super(type(self.parent), self.parent).create_user(skin_type)
        self.db.execute(
            "INSERT INTO entitlements (user_id, plan) VALUES (?, 'free')", (user["id"],)
        )
        for vertical in VERTICALS:
            self.db.execute(
                "INSERT INTO appearance_profiles (id, user_id, vertical) VALUES (?, ?, ?)",
                (uid(), user["id"], vertical),
            )
        self._audit("user_created", "user", user["id"], user["id"])
        return self.profile(user["id"])

    def session_for_identity(
        self,
        firebase_uid: str,
        email: Optional[str] = None,
        email_verified: bool = False,
        name: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Exchange a verified Firebase uid for a GlowUpAI profile.

        Idempotent per `firebase_uid`: the first call creates the user,
        appearance profile, and free entitlement exactly like `create_user`
        and binds the uid; every later call for the same uid returns the same
        user's profile, never a duplicate.
        """

        existing = self.db.fetchone(
            "SELECT id FROM users WHERE firebase_uid = ?", (firebase_uid,)
        )
        if existing:
            return self.profile(existing["id"])
        created = self.create_user(None)
        user_id = created["user"]["id"]
        try:
            self.db.execute(
                "UPDATE users SET firebase_uid = ? WHERE id = ?",
                (firebase_uid, user_id),
            )
        except (sqlite3.IntegrityError, ValueError) as exc:
            # Lost a create-race to a concurrent request for the same uid:
            # bind to whichever row won, and drop the extra one we made
            # rather than leaving an orphaned duplicate user behind.
            logger.warning(f"Race condition detected for firebase_uid {firebase_uid}: {exc}")
            winner = self.db.fetchone(
                "SELECT id FROM users WHERE firebase_uid = ?", (firebase_uid,)
            )
            if not winner:
                raise
            self.parent.delete_user(user_id)
            return self.profile(winner["id"])
        if name:
            profile_row = self.db.fetchone(
                "SELECT display_name FROM experience_profiles WHERE user_id=?",
                (user_id,),
            )
            if profile_row and not profile_row["display_name"]:
                self.db.execute(
                    "UPDATE experience_profiles SET display_name=? WHERE user_id=?",
                    (name.strip()[:80], user_id),
                )
        self._audit(
            "firebase_identity_bound",
            "user",
            user_id,
            user_id,
            {"email_verified": bool(email_verified)},
        )
        return self.profile(user_id)

    def grant_consent(
        self, user_id: str, facial_data: bool, policy_version: Optional[str] = None
    ) -> Dict[str, Any]:
        """Record user consent or decline for facial data processing.

        Args:
            user_id: ID of the user granting or declining consent.
            facial_data: True if consent granted, False if declined.
            policy_version: Version of the privacy policy being consented to.

        Returns:
            Updated user profile dictionary.
        """
        # Call base service method directly to avoid infinite recursion
        result = super(type(self.parent), self.parent).grant_consent(user_id, facial_data, policy_version)
        self._audit(
            "consent_granted" if facial_data else "consent_declined",
            "user",
            user_id,
            user_id,
            {"facial_data": facial_data},
        )
        return self.profile(user_id)

    def profile(self, user_id: str) -> Dict[str, Any]:
        """Retrieve complete user profile including all related data.

        Assembles a comprehensive profile including user data, appearance profiles
        for all verticals, entitlements, and experience profile with goals.

        Args:
            user_id: ID of the user to retrieve profile for.

        Returns:
            Dictionary containing 'user', 'appearance_profiles', 'entitlement',
            'verticals', and 'experience_profile' keys.
        """
        user = self.parent.require_user(user_id)
        profiles = [
            row_dict(row)
            for row in self.db.fetchall(
                "SELECT * FROM appearance_profiles WHERE user_id = ? ORDER BY vertical",
                (user_id,),
            )
        ]
        entitlement = row_dict(
            self.db.fetchone("SELECT * FROM entitlements WHERE user_id = ?", (user_id,))
        )
        if not entitlement:
            self.db.execute("INSERT INTO entitlements (user_id) VALUES (?)", (user_id,))
            entitlement = row_dict(
                self.db.fetchone(
                    "SELECT * FROM entitlements WHERE user_id = ?", (user_id,)
                )
            )
        row = self.db.fetchone(
            "SELECT * FROM experience_profiles WHERE user_id=?", (user_id,)
        )
        if not row:
            self.db.execute(
                "INSERT INTO experience_profiles (user_id) VALUES (?)", (user_id,)
            )
            row = self.db.fetchone(
                "SELECT * FROM experience_profiles WHERE user_id=?", (user_id,)
            )
        experience = row_dict(row)
        experience["goals"] = load(experience.pop("goals_json"), [])
        return {
            "user": user,
            "appearance_profiles": profiles,
            "entitlement": entitlement,
            "verticals": list(VERTICALS),
            "experience_profile": experience,
        }

    def update_profile(
        self,
        user_id: str,
        display_name: Optional[str] = None,
        skin_type: Optional[str] = None,
        focus_vertical: Optional[str] = None,
        goals: Optional[List[str]] = None,
        experience_level: Optional[str] = None,
        onboarding_complete: Optional[bool] = None,
    ) -> Dict[str, Any]:
        """Update user profile and experience settings.

        Updates the user's experience profile with new settings. Only provided
        fields are updated; None values leave existing values unchanged.

        Args:
            user_id: ID of the user to update.
            display_name: User's display name (max 80 chars, non-empty).
            skin_type: User's skin type classification.
            focus_vertical: Primary vertical focus (must be in VERTICALS).
            goals: List of user goals (max 8 items, each max 80 chars).
            experience_level: User's experience level.
            onboarding_complete: Whether onboarding is complete.

        Returns:
            Updated user profile dictionary.

        Raises:
            ValueError: If validation fails for any field.
        """
        self.parent.require_user(user_id)
        if focus_vertical is not None and focus_vertical not in VERTICALS:
            raise ValueError("invalid focus vertical")
        if display_name is not None and not display_name.strip():
            raise ValueError("display name cannot be empty")
        if display_name is not None and len(display_name.strip()) > 80:
            raise ValueError("display name is too long")
        if goals is not None and (
            len(goals) > 8 or any(len(goal) > 80 for goal in goals)
        ):
            raise ValueError("choose up to 8 concise goals")
        self.db.execute(
            "INSERT INTO experience_profiles (user_id) VALUES (?) ON CONFLICT DO NOTHING",
            (user_id,),
        )
        current = self.db.fetchone(
            "SELECT * FROM experience_profiles WHERE user_id=?", (user_id,)
        )
        completed_at = current["onboarding_completed_at"]
        if onboarding_complete is True and not completed_at:
            completed_at = now_iso()
        elif onboarding_complete is False:
            completed_at = None
        self.db.execute(
            """UPDATE experience_profiles SET display_name=?,focus_vertical=?,goals_json=?,
               experience_level=?,onboarding_completed_at=?,updated_at=datetime('now') WHERE user_id=?""",
            (
                (
                    display_name.strip()
                    if display_name is not None
                    else current["display_name"]
                ),
                (
                    focus_vertical
                    if focus_vertical is not None
                    else current["focus_vertical"]
                ),
                dump(goals) if goals is not None else current["goals_json"],
                (
                    experience_level
                    if experience_level is not None
                    else current["experience_level"]
                ),
                completed_at,
                user_id,
            ),
        )
        if skin_type is not None:
            self.db.execute(
                "UPDATE users SET skin_type=? WHERE id=?", (skin_type, user_id)
            )
        self._audit(
            "profile_updated",
            "user",
            user_id,
            user_id,
            {"onboarding_complete": onboarding_complete},
        )
        return self.profile(user_id)

    def export_user(self, user_id: str, history_fn: Any, context_events_fn: Any, check_ins_fn: Any, qna_history_fn: Any, is_premium: bool) -> Dict[str, Any]:
        """Export all user data for GDPR compliance or data portability.

        Assembles a comprehensive export of all user data including profile,
        captures, consents, verdicts, and premium features if applicable.

        Args:
            user_id: ID of the user to export data for.
            history_fn: Callback function to retrieve capture history.
            context_events_fn: Callback function to retrieve context events.
            check_ins_fn: Callback function to retrieve check-in data.
            qna_history_fn: Callback function to retrieve Q&A history.
            is_premium: Whether user has premium access for Q&A export.

        Returns:
            Complete user data export dictionary with version and timestamp.
        """
        self.parent.require_user(user_id)

        from .complete_service import CompleteGlowupAIService
        complete = self.parent if isinstance(self.parent, CompleteGlowupAIService) else None

        return {
            "export_version": "3",
            "exported_at": now_iso(),
            "profile": self.profile(user_id),
            "consent_events": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM consent_events WHERE user_id=?", (user_id,)
                )
            ],
            "appearance_profiles": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM appearance_profiles WHERE user_id=?", (user_id,)
                )
            ],
            "routine_events": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM routine_events WHERE user_id=?", (user_id,)
                )
            ],
            "experiments": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM experiments WHERE user_id=?", (user_id,)
                )
            ],
            "captures_and_metrics": history_fn(user_id),
            "appearance_captures": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM appearance_captures WHERE user_id=?", (user_id,)
                )
            ],
            "verdicts": [
                self.parent._decode_verdict(row) if complete else row_dict(row)
                for row in self.db.fetchall(
                    "SELECT v.*,p.name AS product_name FROM verdicts v JOIN products p ON p.id=v.product_id WHERE v.user_id=?",
                    (user_id,),
                )
            ],
            "context_events": context_events_fn(user_id),
            "check_ins": check_ins_fn(user_id, limit=100),
            "measurement_feedback": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM measurement_feedback WHERE user_id=? ORDER BY created_at",
                    (user_id,),
                )
            ],
            "qna": qna_history_fn(user_id) if is_premium else [],
            "engagement": [
                row_dict(row)
                for row in self.db.fetchall(
                    "SELECT * FROM engagement_events WHERE user_id=?", (user_id,)
                )
            ],
            "note": "Raw photo bytes remain in the configured photo store and are exported through an authenticated object-store workflow.",
        }

    def record_data_collection_consent(
        self,
        user_id: str,
        granted: bool,
        policy_version: str = "1.0"
    ) -> Dict[str, Any]:
        """Record user consent for data collection.

        Creates a consent event record for data collection compliance tracking.

        Args:
            user_id: ID of the user providing consent.
            granted: True if consent is granted, False if declined.
            policy_version: Version of the data collection policy. Defaults to "1.0".

        Returns:
            Confirmation dictionary with message, user_id, and granted status.
        """
        self.db.execute(
            """
            INSERT INTO consent_events (
                user_id,
                consent_type,
                granted,
                policy_version
            ) VALUES (?, ?, ?, ?)
            """,
            (user_id, "data_collection", 1 if granted else 0, policy_version)
        )

        return {
            "message": "Consent recorded successfully",
            "user_id": user_id,
            "granted": granted
        }

    def admin_audit(self, limit: int = 100) -> List[Dict[str, Any]]:
        """Retrieve recent audit log entries for admin review.

        Args:
            limit: Maximum number of audit entries to return (1-1000). Defaults to 100.

        Returns:
            List of audit log entries ordered by most recent first.
        """
        return [
            row_dict(row)
            for row in self.db.fetchall(
                "SELECT * FROM audit_log ORDER BY created_at DESC LIMIT ?",
                (max(1, min(limit, 1000)),),
            )
        ]
