"""Firebase ID token verification for SkinProof.

Why JWKS verification instead of the `firebase-admin` SDK: `firebase-admin`
pulls in `google-cloud-firestore`, `google-cloud-storage`,
`google-api-core`/`grpcio`/`protobuf`, and initializes clients for products
(Firestore, Realtime Database, Cloud Messaging, Remote Config) this backend
does not use anywhere else, just to call the one method that verifies a JWT.

A Firebase ID token is a standard RS256 JWT signed by Google's
`securetoken` service. Google publishes the current signing keys in JWKS
format at

    https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com

(the same "verify with a third-party JWT library" approach Firebase itself
documents:
https://firebase.google.com/docs/auth/admin/verify-id-tokens#verify_id_tokens_using_a_third-party_jwt_library).
Verifying against that endpoint needs only `PyJWT`, which is pure Python and
uses the `cryptography` package this project already depends on for photo
handling — no new transitive dependency tree.

This module fails closed: any missing, malformed, mis-signed, expired, or
wrong-audience/issuer token raises `AuthError`, and callers are expected to
turn that into an HTTP 401/403 rather than treating a verification failure
as "anonymous".
"""

from __future__ import annotations

import json
import re
import time
from collections.abc import Callable
from dataclasses import dataclass
from urllib.error import URLError
from urllib.request import Request, urlopen

import jwt
from jwt import PyJWK

FIREBASE_JWKS_URL = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
DEFAULT_JWKS_TTL_SECONDS = 300
MIN_JWKS_TTL_SECONDS = 60


class AuthError(Exception):
    """Raised whenever a bearer token cannot be trusted. Always fail closed."""


@dataclass(frozen=True)
class FirebaseIdentity:
    """The subset of Firebase ID token claims this backend needs."""

    uid: str
    email: str | None
    email_verified: bool
    name: str | None


def _parse_max_age(cache_control: str | None) -> int:
    if not cache_control:
        return DEFAULT_JWKS_TTL_SECONDS
    match = re.search(r"max-age\s*=\s*(\d+)", cache_control)
    if not match:
        return DEFAULT_JWKS_TTL_SECONDS
    return max(MIN_JWKS_TTL_SECONDS, int(match.group(1)))


def _http_fetch_jwks(url: str) -> tuple[dict[str, object], int]:
    """Fetch the JWKS document, returning {kid: public_key} and a TTL in seconds."""

    request = Request(url, headers={"Accept": "application/json"})
    try:
        with urlopen(request, timeout=5) as response:  # nosec B310 - fixed Google URL
            body = response.read()
            max_age = _parse_max_age(response.headers.get("Cache-Control"))
    except URLError as exc:
        raise AuthError(
            f"could not reach the Firebase signing-key endpoint: {exc}",
        ) from exc
    try:
        payload = json.loads(body)
    except json.JSONDecodeError as exc:
        raise AuthError(
            "Firebase signing-key endpoint returned malformed JSON",
        ) from exc
    keys: dict[str, object] = {}
    for jwk in payload.get("keys", []):
        kid = jwk.get("kid")
        if not kid:
            continue
        try:
            # PyJWT 2.x uses PyJWK instead of RSAAlgorithm.from_jwk
            pyjwk = PyJWK.from_dict(jwk)
            keys[kid] = pyjwk.key
        except (ValueError, TypeError):
            continue
    if not keys:
        raise AuthError("Firebase signing-key endpoint returned no usable keys")
    return keys, max_age


class JWKSCache:
    """Caches Firebase's public signing keys, honouring the response's max-age.

    The fetch function is injectable so tests can mint their own key pair and
    serve it locally instead of making a real network call.
    """

    def __init__(
        self,
        url: str = FIREBASE_JWKS_URL,
        fetch: Callable[[str], tuple[dict[str, object], int]] | None = None,
    ) -> None:
        self._url = url
        self._fetch = fetch or _http_fetch_jwks
        self._keys: dict[str, object] = {}
        self._expires_at: float = 0.0

    def get_key(self, kid: str) -> object:
        now = time.time()
        if now >= self._expires_at or kid not in self._keys:
            self._refresh()
        key = self._keys.get(kid)
        if key is None:
            raise AuthError(f"no Firebase signing key matches kid={kid!r}")
        return key

    def _refresh(self) -> None:
        try:
            keys, max_age = self._fetch(self._url)
        except AuthError:
            if self._keys:
                # Keep serving the stale-but-still-valid key set rather than
                # locking every request out on a transient network blip.
                return
            raise
        self._keys = keys
        self._expires_at = time.time() + max_age


_default_cache: JWKSCache | None = None


def get_default_cache() -> JWKSCache:
    global _default_cache
    if _default_cache is None:
        _default_cache = JWKSCache()
    return _default_cache


def verify_id_token(
    token: str, project_id: str | None, *, jwks: JWKSCache | None = None,
) -> FirebaseIdentity:
    """Verify a Firebase ID token and return the identity it asserts.

    Checks signature (RS256 against Google's published JWKS), `exp`, `iss`
    (`https://securetoken.google.com/<project_id>`), and `aud`
    (`<project_id>`). Raises `AuthError` on any failure.
    """

    if not token or not token.strip():
        raise AuthError("missing bearer token")
    if not project_id:
        raise AuthError(
            "GLOWUPAI_FIREBASE_PROJECT_ID is not configured on this server",
        )
    cache = jwks or get_default_cache()
    try:
        header = jwt.get_unverified_header(token)
    except jwt.PyJWTError as exc:
        raise AuthError(f"malformed token: {exc}") from exc
    kid = header.get("kid")
    if not kid:
        raise AuthError("token header is missing 'kid'")
    public_key = cache.get_key(kid)
    try:
        claims = jwt.decode(
            token,
            key=public_key,
            algorithms=["RS256"],
            audience=project_id,
            issuer=f"https://securetoken.google.com/{project_id}",
            options={"require": ["exp", "iat", "sub", "aud", "iss"]},
        )
    except jwt.ExpiredSignatureError as exc:
        raise AuthError("token has expired") from exc
    except jwt.InvalidAudienceError as exc:
        raise AuthError("token audience does not match this project") from exc
    except jwt.InvalidIssuerError as exc:
        raise AuthError("token issuer does not match this project") from exc
    except jwt.PyJWTError as exc:
        raise AuthError(f"invalid token: {exc}") from exc
    sub = claims.get("sub")
    if not sub or not isinstance(sub, str):
        raise AuthError("token is missing a valid 'sub' claim")
    return FirebaseIdentity(
        uid=sub,
        email=claims.get("email"),
        email_verified=bool(claims.get("email_verified", False)),
        name=claims.get("name"),
    )
