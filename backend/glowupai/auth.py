"""Verification of Supabase Auth access tokens."""

from __future__ import annotations

import json
import re
import time
from collections.abc import Callable
from dataclasses import dataclass
from typing import Any
from urllib.error import URLError
from urllib.request import Request, urlopen

import jwt
from jwt import PyJWK

DEFAULT_JWKS_TTL_SECONDS = 300
MIN_JWKS_TTL_SECONDS = 60


class AuthError(Exception):
    """Raised whenever a bearer token cannot be trusted."""


@dataclass(frozen=True)
class SupabaseIdentity:
    """The claims the application needs from a verified Supabase user token."""

    uid: str
    email: str | None
    email_verified: bool
    name: str | None


def _parse_max_age(cache_control: str | None) -> int:
    if not cache_control:
        return DEFAULT_JWKS_TTL_SECONDS
    match = re.search(r"max-age\s*=\s*(\d+)", cache_control)
    return (
        max(MIN_JWKS_TTL_SECONDS, int(match.group(1)))
        if match
        else DEFAULT_JWKS_TTL_SECONDS
    )


def _http_fetch_jwks(url: str) -> tuple[dict[str, object], int]:
    request = Request(url, headers={"Accept": "application/json"})
    try:
        # The URL comes only from trusted Supabase deployment configuration.
        with urlopen(request, timeout=5) as response:  # nosec B310
            body = response.read()
            max_age = _parse_max_age(response.headers.get("Cache-Control"))
    except (URLError, OSError) as exc:
        raise AuthError(
            f"could not reach the Supabase signing-key endpoint: {exc}"
        ) from exc
    try:
        payload = json.loads(body)
    except json.JSONDecodeError as exc:
        raise AuthError(
            "Supabase signing-key endpoint returned malformed JSON"
        ) from exc
    keys: dict[str, object] = {}
    if not isinstance(payload, dict):
        raise AuthError("Supabase signing-key endpoint returned an invalid payload")
    for jwk in payload.get("keys", []):
        if not isinstance(jwk, dict):
            continue
        kid = jwk.get("kid")
        if not kid:
            continue
        try:
            keys[kid] = PyJWK.from_dict(jwk).key
        except (TypeError, ValueError):
            continue
    if not keys:
        raise AuthError("Supabase signing-key endpoint returned no usable keys")
    return keys, max_age


class JWKSCache:
    """Caches a Supabase project's public signing keys and honours max-age."""

    def __init__(
        self,
        url: str,
        fetch: Callable[[str], tuple[dict[str, object], int]] | None = None,
    ) -> None:
        self._url = url
        self._fetch = fetch or _http_fetch_jwks
        self._keys: dict[str, object] = {}
        self._expires_at = 0.0

    def get_key(self, kid: str) -> object:
        if time.time() >= self._expires_at or kid not in self._keys:
            self._refresh()
        key = self._keys.get(kid)
        if key is None:
            raise AuthError(f"no Supabase signing key matches kid={kid!r}")
        return key

    def _refresh(self) -> None:
        try:
            keys, max_age = self._fetch(self._url)
        except AuthError:
            if self._keys:
                return
            raise
        self._keys = keys
        self._expires_at = time.time() + max_age


_caches: dict[str, JWKSCache] = {}


def _cache_for(supabase_url: str, jwks_url: str | None = None) -> JWKSCache:
    url = (
        jwks_url or f"{supabase_url.rstrip('/')}/auth/v1/.well-known/jwks.json"
    ).strip()
    if url not in _caches:
        _caches[url] = JWKSCache(url)
    return _caches[url]


def verify_access_token(
    token: str,
    supabase_url: str | None,
    jwt_secret: str | None,
    *,
    jwks: JWKSCache | None = None,
    jwks_url: str | None = None,
) -> SupabaseIdentity:
    """Verify one Supabase Auth access JWT and return its trusted identity."""

    if not token or not token.strip():
        raise AuthError("missing bearer token")
    if not supabase_url or not supabase_url.strip().startswith("https://"):
        raise AuthError("SUPABASE_URL is not configured on this server")
    issuer = f"{supabase_url.rstrip('/')}/auth/v1"
    try:
        header = jwt.get_unverified_header(token)
    except jwt.PyJWTError as exc:
        # Do not reflect parser/crypto implementation details to unauthenticated
        # callers.  They are not actionable and can vary between PyJWT versions.
        raise AuthError("malformed bearer token") from exc
    algorithm = header.get("alg")
    key: Any
    try:
        if algorithm == "HS256":
            if not jwt_secret:
                raise AuthError("SUPABASE_JWT_SECRET is not configured on this server")
            key = jwt_secret
            algorithms = ["HS256"]
        elif algorithm in {"RS256", "ES256", "EdDSA"}:
            kid = header.get("kid")
            if not kid:
                raise AuthError("token header is missing 'kid'")
            key = (jwks or _cache_for(supabase_url, jwks_url)).get_key(kid)
            algorithms = [algorithm]
        else:
            raise AuthError("token uses an unsupported signing algorithm")
        claims = jwt.decode(
            token,
            key=key,  # type: ignore[arg-type]
            algorithms=algorithms,
            audience="authenticated",
            issuer=issuer,
            options={"require": ["exp", "iat", "sub", "aud", "iss"]},
        )
    except AuthError:
        raise
    except jwt.ExpiredSignatureError as exc:
        raise AuthError("token has expired") from exc
    except jwt.InvalidAudienceError as exc:
        raise AuthError("token audience is not authenticated") from exc
    except jwt.InvalidIssuerError as exc:
        raise AuthError("token issuer does not match this Supabase project") from exc
    except jwt.PyJWTError as exc:
        raise AuthError("invalid Supabase token") from exc

    if claims.get("role") not in (None, "authenticated"):
        raise AuthError("token role is not an authenticated user")
    subject = claims.get("sub")
    if not isinstance(subject, str) or not subject:
        raise AuthError("token is missing a valid 'sub' claim")
    metadata = claims.get("user_metadata")
    if not isinstance(metadata, dict):
        metadata = {}
    name = claims.get("name") or metadata.get("full_name") or metadata.get("name")
    return SupabaseIdentity(
        uid=subject,
        email=claims.get("email") if isinstance(claims.get("email"), str) else None,
        email_verified=bool(
            claims.get("email_verified") or claims.get("email_confirmed_at")
        ),
        name=name if isinstance(name, str) else None,
    )


# Generic compatibility name; it now verifies Supabase tokens and has no provider-specific behavior.
verify_id_token = verify_access_token
