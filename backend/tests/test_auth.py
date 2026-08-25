from __future__ import annotations

import tempfile
import time
import unittest
from pathlib import Path

import jwt
import skinproof.auth as auth_module
from cryptography.hazmat.primitives.asymmetric import rsa
from fastapi.testclient import TestClient

from skinproof.auth import AuthError, JWKSCache, verify_id_token
from skinproof.complete_api import create_complete_app
from skinproof.complete_db import FullDatabase
from skinproof.complete_service import CompleteSkinProofService
from skinproof.config import Settings
from skinproof.photos import MemoryPhotoStore

PROJECT_ID = "glowup-test"
KID = "test-key-1"
OTHER_KID = "test-key-2"


def _rsa_pair():
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    return private_key, private_key.public_key()


PRIVATE_KEY, PUBLIC_KEY = _rsa_pair()
OTHER_PRIVATE_KEY, OTHER_PUBLIC_KEY = _rsa_pair()


class _NetworkForbiddenCache(JWKSCache):
    """A JWKSCache preloaded with test keys; raises if it ever tries the network."""

    def __init__(self, keys: dict[str, object]):
        def _forbidden_fetch(url: str):  # pragma: no cover - should never run
            raise AuthError("tests must never fetch JWKS over the network")

        super().__init__(fetch=_forbidden_fetch)
        self._keys = dict(keys)
        self._expires_at = time.time() + 3600


def make_token(private_key, kid=KID, *, sub="firebase-uid-1", project_id=PROJECT_ID, iss=None, aud=None, exp_delta=3600, email=None, email_verified=None, name=None):
    now = int(time.time())
    claims = {
        "iss": iss if iss is not None else f"https://securetoken.google.com/{project_id}",
        "aud": aud if aud is not None else project_id,
        "sub": sub,
        "iat": now,
        "exp": now + exp_delta,
    }
    if email is not None:
        claims["email"] = email
    if email_verified is not None:
        claims["email_verified"] = email_verified
    if name is not None:
        claims["name"] = name
    return jwt.encode(claims, private_key, algorithm="RS256", headers={"kid": kid})


class TokenVerificationTests(unittest.TestCase):
    """Unit tests for skinproof.auth, independent of the HTTP layer."""

    def setUp(self):
        self.cache = _NetworkForbiddenCache({KID: PUBLIC_KEY})

    def test_valid_token_is_accepted_and_claims_extracted(self):
        token = make_token(PRIVATE_KEY, email="demo@example.com", email_verified=True, name="Alex Demo")
        identity = verify_id_token(token, PROJECT_ID, jwks=self.cache)
        self.assertEqual(identity.uid, "firebase-uid-1")
        self.assertEqual(identity.email, "demo@example.com")
        self.assertTrue(identity.email_verified)
        self.assertEqual(identity.name, "Alex Demo")

    def test_bad_signature_is_rejected(self):
        # Signed by a different private key but claiming the trusted kid.
        token = make_token(OTHER_PRIVATE_KEY, kid=KID)
        with self.assertRaises(AuthError):
            verify_id_token(token, PROJECT_ID, jwks=self.cache)

    def test_unknown_kid_is_rejected(self):
        token = make_token(OTHER_PRIVATE_KEY, kid=OTHER_KID)
        with self.assertRaises(AuthError):
            verify_id_token(token, PROJECT_ID, jwks=self.cache)

    def test_wrong_audience_is_rejected(self):
        token = make_token(PRIVATE_KEY, aud="some-other-project")
        with self.assertRaises(AuthError):
            verify_id_token(token, PROJECT_ID, jwks=self.cache)

    def test_wrong_issuer_is_rejected(self):
        token = make_token(PRIVATE_KEY, iss="https://securetoken.google.com/some-other-project")
        with self.assertRaises(AuthError):
            verify_id_token(token, PROJECT_ID, jwks=self.cache)

    def test_expired_token_is_rejected(self):
        token = make_token(PRIVATE_KEY, exp_delta=-60)
        with self.assertRaises(AuthError):
            verify_id_token(token, PROJECT_ID, jwks=self.cache)

    def test_missing_token_fails_closed(self):
        with self.assertRaises(AuthError):
            verify_id_token("", PROJECT_ID, jwks=self.cache)

    def test_unconfigured_project_id_fails_closed(self):
        token = make_token(PRIVATE_KEY)
        with self.assertRaises(AuthError):
            verify_id_token(token, None, jwks=self.cache)

    def test_malformed_token_fails_closed(self):
        with self.assertRaises(AuthError):
            verify_id_token("not-a-jwt", PROJECT_ID, jwks=self.cache)


class _BaseAppTest(unittest.TestCase):
    auth_required = True
    admin_token: str | None = "test-admin-token"

    def setUp(self):
        self._original_default_cache = auth_module._default_cache
        auth_module._default_cache = _NetworkForbiddenCache({KID: PUBLIC_KEY})
        self.temp = tempfile.TemporaryDirectory()
        db_path = Path(self.temp.name) / "auth.sqlite3"
        self.db = FullDatabase(db_path)
        self.settings = Settings(
            db_path=db_path,
            photo_dir=None,
            gemini_enabled=False,
            firebase_project_id=PROJECT_ID,
            auth_required=self.auth_required,
            admin_token=self.admin_token,
        )
        self.service = CompleteSkinProofService(self.db, settings=self.settings, photos=MemoryPhotoStore())
        self.client = TestClient(create_complete_app(self.service))

    def tearDown(self):
        auth_module._default_cache = self._original_default_cache
        self.db.close()
        self.temp.cleanup()

    def bearer(self, token: str) -> dict:
        return {"Authorization": f"Bearer {token}"}


class SessionEndpointTests(_BaseAppTest):
    def test_session_requires_a_bearer_token(self):
        response = self.client.post("/api/auth/session")
        self.assertEqual(response.status_code, 401)

    def test_session_rejects_an_invalid_token(self):
        bad_token = make_token(OTHER_PRIVATE_KEY, kid=KID)
        response = self.client.post("/api/auth/session", headers=self.bearer(bad_token))
        self.assertEqual(response.status_code, 401)

    def test_session_creates_profile_on_first_sight(self):
        token = make_token(PRIVATE_KEY, sub="uid-alpha")
        response = self.client.post("/api/auth/session", headers=self.bearer(token))
        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertIn("user", body)
        self.assertIn("appearance_profiles", body)
        self.assertIn("entitlement", body)
        self.assertEqual(body["user"]["consent_state"], "pending")
        self.assertEqual(body["entitlement"]["plan"], "free")

    def test_session_is_idempotent_per_uid(self):
        token = make_token(PRIVATE_KEY, sub="uid-beta")
        first = self.client.post("/api/auth/session", headers=self.bearer(token))
        second = self.client.post("/api/auth/session", headers=self.bearer(token))
        self.assertEqual(first.status_code, 200)
        self.assertEqual(second.status_code, 200)
        self.assertEqual(first.json()["user"]["id"], second.json()["user"]["id"])

        rows = self.db.fetchall("SELECT id FROM users WHERE firebase_uid = ?", ("uid-beta",))
        self.assertEqual(len(rows), 1)

    def test_session_returns_the_same_shape_as_get_profile(self):
        token = make_token(PRIVATE_KEY, sub="uid-gamma")
        session_body = self.client.post("/api/auth/session", headers=self.bearer(token)).json()
        user_id = session_body["user"]["id"]
        profile_body = self.client.get(f"/api/users/{user_id}/profile", headers=self.bearer(token)).json()
        self.assertEqual(set(session_body.keys()), set(profile_body.keys()))
        self.assertEqual(session_body["user"]["id"], profile_body["user"]["id"])


class OwnershipEnforcementTests(_BaseAppTest):
    def _session(self, sub: str) -> tuple[str, str]:
        token = make_token(PRIVATE_KEY, sub=sub)
        user_id = self.client.post("/api/auth/session", headers=self.bearer(token)).json()["user"]["id"]
        return user_id, token

    def test_owner_can_read_their_own_profile(self):
        user_id, token = self._session("uid-owner")
        response = self.client.get(f"/api/users/{user_id}/profile", headers=self.bearer(token))
        self.assertEqual(response.status_code, 200)

    def test_mismatched_uid_gets_403(self):
        user_id_a, _token_a = self._session("uid-owner-a")
        _user_id_b, token_b = self._session("uid-owner-b")
        response = self.client.get(f"/api/users/{user_id_a}/profile", headers=self.bearer(token_b))
        self.assertEqual(response.status_code, 403)

    def test_missing_token_on_a_protected_route_gets_401(self):
        user_id, _token = self._session("uid-owner-c")
        response = self.client.get(f"/api/users/{user_id}/profile")
        self.assertEqual(response.status_code, 401)

    def test_body_scoped_route_enforces_ownership(self):
        # POST /api/routine-events carries user_id in the JSON body, not the path.
        user_id_a, _token_a = self._session("uid-routine-a")
        _user_id_b, token_b = self._session("uid-routine-b")
        product = self.client.post("/api/products", json={"name": "Auth test serum"}).json()
        response = self.client.post(
            "/api/routine-events",
            json={"user_id": user_id_a, "product_id": product["id"], "action": "start"},
            headers=self.bearer(token_b),
        )
        self.assertEqual(response.status_code, 403)

    def test_health_and_triage_stay_open_without_a_token(self):
        self.assertEqual(self.client.get("/api/health").status_code, 200)
        self.assertEqual(self.client.post("/api/triage", json={"text": "just checking in"}).status_code, 200)


class AuthDisabledPassthroughTests(_BaseAppTest):
    auth_required = False

    def test_unauthenticated_requests_behave_as_before(self):
        user = self.client.post("/api/users", json={}).json()
        user_id = user["user"]["id"]
        # No Authorization header at all -- must work exactly as it did before auth existed.
        response = self.client.get(f"/api/users/{user_id}/profile")
        self.assertEqual(response.status_code, 200)

    def test_a_mismatched_token_is_ignored_while_auth_is_off(self):
        user = self.client.post("/api/users", json={}).json()
        user_id = user["user"]["id"]
        unrelated_token = make_token(PRIVATE_KEY, sub="someone-else")
        response = self.client.get(f"/api/users/{user_id}/profile", headers=self.bearer(unrelated_token))
        self.assertEqual(response.status_code, 200)


class AdminBoundaryTests(_BaseAppTest):
    def test_admin_route_rejects_missing_token(self):
        self.assertEqual(self.client.get("/api/admin/audit").status_code, 403)

    def test_admin_route_rejects_wrong_token(self):
        response = self.client.get("/api/admin/audit", headers=self.bearer("not-the-token"))
        self.assertEqual(response.status_code, 403)

    def test_admin_route_accepts_the_configured_token(self):
        response = self.client.get("/api/admin/audit", headers=self.bearer("test-admin-token"))
        self.assertEqual(response.status_code, 200)

    def test_admin_offers_route_is_gated_too(self):
        product = self.client.post("/api/products", json={"name": "Admin gated product"}).json()
        payload = {"product_id": product["id"], "merchant": "Acme", "url": "https://example.com/p"}
        self.assertEqual(self.client.post("/api/admin/offers", json=payload).status_code, 403)
        self.assertEqual(
            self.client.post("/api/admin/offers", json=payload, headers=self.bearer("test-admin-token")).status_code,
            200,
        )

    def test_measurement_feedback_summary_is_gated(self):
        self.assertEqual(self.client.get("/api/admin/measurement-feedback").status_code, 403)
        self.assertEqual(
            self.client.get("/api/admin/measurement-feedback", headers=self.bearer("test-admin-token")).status_code,
            200,
        )


class AdminBoundaryWithoutTokenConfiguredTests(_BaseAppTest):
    """When SKINPROOF_ADMIN_TOKEN is unset, admin routes must refuse everyone."""

    admin_token = None

    def test_admin_routes_refuse_every_request_when_no_token_is_configured(self):
        self.assertEqual(self.client.get("/api/admin/audit").status_code, 403)
        self.assertEqual(self.client.get("/api/admin/audit", headers=self.bearer("anything")).status_code, 403)
        self.assertEqual(self.client.get("/api/admin/measurement-feedback").status_code, 403)


if __name__ == "__main__":
    unittest.main()
