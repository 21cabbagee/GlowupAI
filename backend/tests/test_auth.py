from __future__ import annotations

import tempfile
import time
import unittest
from pathlib import Path

import jwt
from fastapi.testclient import TestClient

from glowupai.auth import AuthError, verify_access_token
from glowupai.complete_api import create_complete_app
from glowupai.complete_db import FullDatabase
from glowupai.complete_service import CompleteGlowupAIService
from glowupai.config import Settings
from glowupai.photos import MemoryPhotoStore

SUPABASE_URL = "https://glowup-test.supabase.co"
JWT_SECRET = "supabase-test-secret"


def make_token(
    *,
    sub: str = "supabase-uid-1",
    issuer: str = SUPABASE_URL + "/auth/v1",
    audience: str = "authenticated",
    expires_delta: int = 3600,
    role: str = "authenticated",
) -> str:
    now = int(time.time())
    return jwt.encode(
        {
            "iss": issuer,
            "aud": audience,
            "sub": sub,
            "iat": now,
            "exp": now + expires_delta,
            "role": role,
            "email": "demo@example.com",
            "email_verified": True,
            "user_metadata": {"full_name": "Demo User"},
        },
        JWT_SECRET,
        algorithm="HS256",
    )


class TokenVerificationTests(unittest.TestCase):
    def test_valid_supabase_token_is_accepted(self):
        identity = verify_access_token(make_token(), SUPABASE_URL, JWT_SECRET)
        self.assertEqual(identity.uid, "supabase-uid-1")
        self.assertEqual(identity.email, "demo@example.com")
        self.assertTrue(identity.email_verified)
        self.assertEqual(identity.name, "Demo User")

    def test_bad_signature_is_rejected(self):
        token = jwt.encode(
            {
                "iss": SUPABASE_URL + "/auth/v1",
                "aud": "authenticated",
                "sub": "x",
                "iat": int(time.time()),
                "exp": int(time.time()) + 60,
            },
            "wrong-secret",
            algorithm="HS256",
        )
        with self.assertRaises(AuthError):
            verify_access_token(token, SUPABASE_URL, JWT_SECRET)

    def test_wrong_audience_issuer_expiry_and_role_are_rejected(self):
        for kwargs in (
            {"audience": "other"},
            {"issuer": "https://other.supabase.co/auth/v1"},
            {"expires_delta": -60},
            {"role": "service_role"},
        ):
            with self.subTest(kwargs=kwargs), self.assertRaises(AuthError):
                verify_access_token(make_token(**kwargs), SUPABASE_URL, JWT_SECRET)

    def test_missing_configuration_and_token_fail_closed(self):
        with self.assertRaises(AuthError):
            verify_access_token("", SUPABASE_URL, JWT_SECRET)
        with self.assertRaises(AuthError):
            verify_access_token(make_token(), None, JWT_SECRET)
        with self.assertRaises(AuthError):
            verify_access_token(make_token(), SUPABASE_URL, None)

    def test_token_errors_do_not_expose_jwt_parser_details(self):
        with self.assertRaisesRegex(AuthError, "^malformed bearer token$"):
            verify_access_token("not.a.jwt", SUPABASE_URL, JWT_SECRET)

        now = int(time.time())
        bad_signature = jwt.encode(
            {
                "iss": SUPABASE_URL + "/auth/v1",
                "aud": "authenticated",
                "sub": "x",
                "iat": now,
                "exp": now + 60,
            },
            "wrong-secret",
            algorithm="HS256",
        )
        with self.assertRaisesRegex(AuthError, "^invalid Supabase token$"):
            verify_access_token(bad_signature, SUPABASE_URL, JWT_SECRET)


class AuthenticatedApiTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        db_path = Path(self.temp.name) / "auth.sqlite3"
        self.db = FullDatabase(db_path)
        settings = Settings(
            db_path=db_path,
            photo_dir=None,
            supabase_url=SUPABASE_URL,
            supabase_jwt_secret=JWT_SECRET,
            auth_required=True,
            admin_token="a" * 40,
            gemini_enabled=False,
        )
        service = CompleteGlowupAIService(
            self.db, settings=settings, photos=MemoryPhotoStore()
        )
        self.client = TestClient(create_complete_app(service))

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def bearer(self, token: str) -> dict[str, str]:
        return {"Authorization": f"Bearer {token}"}

    def test_session_is_idempotent_and_binds_supabase_uid(self):
        token = make_token(sub="uid-alpha")
        first = self.client.post("/api/auth/session", headers=self.bearer(token))
        second = self.client.post("/api/auth/session", headers=self.bearer(token))
        self.assertEqual(first.status_code, 200)
        self.assertTrue(first.json()["created"])
        self.assertFalse(second.json()["created"])
        self.assertEqual(first.json()["user"]["id"], second.json()["user"]["id"])
        rows = self.db.fetchall(
            "SELECT id FROM users WHERE supabase_uid = ?", ("uid-alpha",)
        )
        self.assertEqual(len(rows), 1)

        signup_events = self.db.fetchall(
            "SELECT * FROM analytics_events WHERE event_type = 'user_signup'"
        )
        self.assertEqual(len(signup_events), 1)

    def test_protected_profile_requires_matching_supabase_identity(self):
        owner = make_token(sub="owner")
        other = make_token(sub="other")
        user_id = self.client.post(
            "/api/auth/session", headers=self.bearer(owner)
        ).json()["user"]["id"]
        self.assertEqual(
            self.client.get(
                f"/api/users/{user_id}/profile", headers=self.bearer(owner)
            ).status_code,
            200,
        )
        self.assertEqual(
            self.client.get(
                f"/api/users/{user_id}/profile", headers=self.bearer(other)
            ).status_code,
            403,
        )
        self.assertEqual(
            self.client.get(f"/api/users/{user_id}/profile").status_code, 401
        )

    def test_legacy_anonymous_user_creation_is_disabled_when_auth_is_required(self):
        response = self.client.post("/api/users", json={})
        self.assertEqual(response.status_code, 401)
        self.assertEqual(
            self.db.fetchone("SELECT COUNT(*) AS count FROM users")["count"], 0
        )

    def test_global_product_write_requires_a_valid_user_jwt(self):
        payload = {"name": "Authenticated cleanser"}
        self.assertEqual(
            self.client.post("/api/products", json=payload).status_code, 401
        )
        self.assertEqual(
            self.client.post(
                "/api/products",
                json=payload,
                headers=self.bearer(make_token(sub="catalog-editor")),
            ).status_code,
            200,
        )


if __name__ == "__main__":
    unittest.main()
