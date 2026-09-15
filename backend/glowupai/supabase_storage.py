"""Private Supabase Storage adapter for user image objects."""

from __future__ import annotations

import hashlib
import json
import re
from collections.abc import Callable
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote
from urllib.request import Request, urlopen


class SupabaseStorageError(RuntimeError):
    """Raised when the private Supabase bucket cannot be read or written."""


class SupabasePhotoStore:
    """Store owner-scoped image paths in a private Supabase Storage bucket.

    The service-role key is accepted only by this backend adapter and is never
    part of an API response or Android build. Database rows contain paths such
    as ``users/<hash>/captures/<id>/raw.bin``; no public or signed URL is stored.
    """

    _capture_id = re.compile(r"^[A-Za-z0-9_-]+$")
    _object_path = re.compile(r"^users/[0-9a-f]{64}/captures/[A-Za-z0-9_-]+/raw\.bin$")

    def __init__(
        self,
        supabase_url: str,
        service_role_key: str,
        bucket: str,
        *,
        opener: Callable[..., Any] = urlopen,
    ) -> None:
        if not supabase_url.startswith("https://"):
            raise ValueError("SUPABASE_URL must use HTTPS")
        if not service_role_key:
            raise ValueError("SUPABASE_SERVICE_ROLE_KEY is required")
        if not bucket or "/" in bucket:
            raise ValueError("SUPABASE_STORAGE_BUCKET must be a bucket name")
        self.base_url = supabase_url.rstrip("/")
        self.service_role_key = service_role_key
        self.bucket = bucket
        self._opener = opener

    def _path_for(self, user_id: str, capture_id: str) -> str:
        if not capture_id or not self._capture_id.fullmatch(capture_id):
            raise ValueError("Invalid photo identifier")
        owner_hash = hashlib.sha256(user_id.encode("utf-8")).hexdigest()
        return f"users/{owner_hash}/captures/{capture_id}/raw.bin"

    def _request(
        self,
        method: str,
        endpoint: str,
        body: bytes | None = None,
        content_type: str | None = None,
        extra_headers: dict[str, str] | None = None,
    ) -> bytes:
        # New Supabase ``sb_secret_`` keys are opaque API keys, not JWTs.
        # Storage rejects them in Authorization as “Invalid Compact JWS”; they
        # must travel only in the apikey header. Legacy service-role JWTs keep
        # the Bearer header for backwards-compatible deployments.
        headers = {
            "apikey": self.service_role_key,
            "Accept": "application/json",
            **({"Content-Type": content_type} if content_type else {}),
            **(extra_headers or {}),
        }
        if not self.service_role_key.startswith("sb_"):
            headers["Authorization"] = f"Bearer {self.service_role_key}"
        request = Request(
            f"{self.base_url}/storage/v1/{endpoint.lstrip('/')}",
            data=body,
            method=method,
            headers=headers,
        )
        try:
            with self._opener(request, timeout=30) as response:  # type: ignore[union-attr]
                return bytes(response.read())  # type: ignore[union-attr]
        except (HTTPError, URLError, OSError) as exc:
            status = getattr(exc, "code", "unknown")
            # HTTPError carries the Storage API's response body. Preserve its
            # short, server-supplied diagnostic (never request headers or data)
            # so production logs can distinguish invalid credentials, a missing
            # bucket, and an invalid object request.
            detail = ""
            if isinstance(exc, HTTPError):
                try:
                    raw_detail = exc.read(1024).decode("utf-8", "replace").strip()
                    parsed_detail = json.loads(raw_detail)
                    if isinstance(parsed_detail, dict):
                        detail = str(
                            parsed_detail.get("message")
                            or parsed_detail.get("error")
                            or parsed_detail.get("code")
                            or ""
                        )
                    else:
                        detail = raw_detail
                except (OSError, UnicodeError, json.JSONDecodeError):
                    pass
            suffix = f": {detail[:300]}" if detail else ""
            raise SupabaseStorageError(
                f"Supabase Storage request failed ({status}){suffix}"
            ) from exc

    def save(self, user_id: str, capture_id: str, data: bytes) -> str:
        path = self._path_for(user_id, capture_id)
        endpoint = f"object/{quote(self.bucket, safe='')}/{quote(path, safe='/')}"
        try:
            self._request(
                "POST",
                endpoint,
                data,
                "application/octet-stream",
                {"x-upsert": "true"},
            )
        except SupabaseStorageError as exc:
            if "Bucket not found" not in str(exc):
                raise
            # A fresh Supabase project has no Storage buckets by default.
            # Create only the configured, private bucket and retry once. The
            # service key never reaches the Android client.
            self._request(
                "POST",
                "bucket",
                json.dumps(
                    {"id": self.bucket, "name": self.bucket, "public": False},
                    separators=(",", ":"),
                ).encode("utf-8"),
                "application/json",
            )
            self._request(
                "POST",
                endpoint,
                data,
                "application/octet-stream",
                {"x-upsert": "true"},
            )
        return path

    def read(self, reference: str) -> bytes:
        if not self._object_path.fullmatch(reference):
            raise ValueError(
                "Supabase photo reference is not a valid private object path"
            )
        return self._request(
            "GET",
            f"object/{quote(self.bucket, safe='')}/{quote(reference, safe='/')}",
        )

    def delete_user(self, user_id: str) -> None:
        owner_hash = hashlib.sha256(user_id.encode("utf-8")).hexdigest()
        prefix = f"users/{owner_hash}/"
        offset = 0
        while True:
            payload = json.dumps(
                {"prefix": prefix, "limit": 1000, "offset": offset},
                separators=(",", ":"),
            ).encode("utf-8")
            raw = self._request(
                "POST",
                f"object/list/{quote(self.bucket, safe='')}",
                payload,
                "application/json",
            )
            try:
                entries = json.loads(raw or b"[]")
            except json.JSONDecodeError as exc:
                raise SupabaseStorageError(
                    "Supabase Storage returned malformed object metadata"
                ) from exc
            paths = [
                item.get("name", "")
                for item in entries
                if isinstance(item, dict) and item.get("name")
            ]
            full_paths = [
                path if path.startswith(prefix) else prefix + path for path in paths
            ]
            if full_paths:
                self._request(
                    "POST",
                    f"object/remove/{quote(self.bucket, safe='')}",
                    json.dumps({"prefixes": full_paths}, separators=(",", ":")).encode(
                        "utf-8"
                    ),
                    "application/json",
                )
            if len(entries) < 1000:
                return
            offset += len(entries)
