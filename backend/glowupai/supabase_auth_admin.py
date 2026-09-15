"""Server-only Supabase Auth administration operations."""

from __future__ import annotations

from collections.abc import Callable
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen


class SupabaseAuthAdminError(RuntimeError):
    """Raised when a server-only Supabase Auth admin request fails."""


class SupabaseAuthAdmin:
    """Delete Auth identities with the backend-only service-role credential."""

    def __init__(
        self,
        supabase_url: str,
        service_role_key: str,
        *,
        opener: Callable[..., Any] = urlopen,
    ) -> None:
        if not supabase_url.startswith("https://"):
            raise ValueError("SUPABASE_URL must use HTTPS")
        if not service_role_key:
            raise ValueError("SUPABASE_SERVICE_ROLE_KEY is required")
        self.base_url = supabase_url.rstrip("/")
        self.service_role_key = service_role_key
        self._opener = opener

    def delete_user(self, supabase_uid: str) -> None:
        if not supabase_uid or "/" in supabase_uid or "\\" in supabase_uid:
            raise ValueError("Invalid Supabase user ID")
        request = Request(
            f"{self.base_url}/auth/v1/admin/users/{quote(supabase_uid, safe='')}",
            method="DELETE",
            headers={
                "Authorization": f"Bearer {self.service_role_key}",
                "apikey": self.service_role_key,
                "Accept": "application/json",
            },
        )
        try:
            with self._opener(request, timeout=30) as response:  # type: ignore[union-attr]
                response.read()  # type: ignore[union-attr]
        except HTTPError as exc:
            # Deletion is idempotent: a prior retry may already have removed it.
            if exc.code == 404:
                return
            raise SupabaseAuthAdminError(
                f"Supabase Auth admin request failed ({exc.code})",
            ) from exc
        except (URLError, OSError) as exc:
            raise SupabaseAuthAdminError("Supabase Auth admin request failed") from exc
