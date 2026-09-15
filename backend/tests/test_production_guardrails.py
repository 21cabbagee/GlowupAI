from __future__ import annotations

import base64
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from glowupai.config import Settings


class ProductionGuardrailTests(unittest.TestCase):
    def valid_settings(self) -> Settings:
        return Settings(
            db_path=Path("/tmp/glowup-production-test.sqlite3"),
            photo_dir=Path("/tmp/glowup-production-photos"),
            database_url="postgresql://user:password@example.test/glowup",
            supabase_url="https://glowup-production.supabase.co",
            supabase_jwt_secret="supabase-jwt-secret",
            supabase_service_role_key="supabase-service-role-key",
            supabase_storage_bucket="user-images",
            auth_required=True,
            admin_token="a" * 40,
            allowed_origins=["https://app.example.test"],
        )

    def test_production_accepts_complete_durable_configuration(self):
        photo_key = base64.b64encode(b"k" * 32).decode()
        with patch.dict(
            os.environ,
            {
                "GLOWUPAI_ENV": "production",
                "GLOWUPAI_BILLING_KEY": photo_key,
                "GLOWUPAI_PLAY_PACKAGE_NAME": "com.glowup.ai",
                "GLOWUPAI_PLAY_PRODUCT_IDS": "premium",
                "GLOWUPAI_PLAY_RTDN_AUDIENCE": "https://api.example.test/api/billing/play/notifications",
                "GLOWUPAI_PLAY_RTDN_EMAIL": "play-rtdn@example.iam.gserviceaccount.com",
                "GLOWUPAI_PLAY_SERVICE_ACCOUNT_JSON_B64": "service-account-json",
                "GLOWUPAI_SKIP_QUALITY_CHECKS": "0",
            },
            clear=False,
        ):
            self.valid_settings().validate_for_production()

    def test_production_rejects_every_unsafe_boundary(self):
        with tempfile.TemporaryDirectory():
            with patch.dict(
                os.environ,
                {
                    "GLOWUPAI_ENV": "production",
                    "GLOWUPAI_BILLING_KEY": "",
                    "GLOWUPAI_PLAY_PACKAGE_NAME": "",
                    "GLOWUPAI_PLAY_PRODUCT_IDS": "",
                    "GLOWUPAI_SKIP_QUALITY_CHECKS": "1",
                },
                clear=False,
            ):
                settings = Settings(
                    db_path=Path("/tmp/test.sqlite3"),
                    photo_dir=None,
                    database_url="sqlite:///unsafe.sqlite3",
                    supabase_url=None,
                    supabase_jwt_secret=None,
                    supabase_service_role_key=None,
                    auth_required=False,
                    admin_token="short",
                    allowed_origins=["http://localhost:3000"],
                )
                with self.assertRaisesRegex(
                    RuntimeError, "Invalid production configuration"
                ):
                    settings.validate_for_production()

    def test_production_allows_play_billing_to_be_disabled(self):
        with patch.dict(
            os.environ,
            {
                "GLOWUPAI_ENV": "production",
                "GLOWUPAI_PLAY_BILLING_ENABLED": "0",
                "GLOWUPAI_SKIP_QUALITY_CHECKS": "0",
            },
            clear=False,
        ):
            self.valid_settings().validate_for_production()

    def test_development_can_use_local_defaults(self):
        with patch.dict(os.environ, {"GLOWUPAI_ENV": "development"}, clear=False):
            settings = Settings(
                db_path=Path("/tmp/test.sqlite3"),
                photo_dir=None,
                database_url=None,
                allowed_origins=["http://localhost:3000"],
            )
            settings.validate_for_production()

    def test_current_supabase_key_aliases_and_jwks_are_loaded(self):
        with patch.dict(
            os.environ,
            {
                "GLOWUPAI_ENV": "development",
                "SUPABASE_URL": "https://egkfbdjelgojryckapcp.supabase.co",
                "SUPABASE_JWKS_URL": "https://egkfbdjelgojryckapcp.supabase.co/auth/v1/.well-known/jwks.json",
                "SUPABASE_SECRET_KEY": "server-secret",
                "SUPABASE_SERVICE_ROLE_KEY": "",
            },
            clear=False,
        ):
            settings = Settings.from_env()

        self.assertEqual(
            settings.supabase_url, "https://egkfbdjelgojryckapcp.supabase.co"
        )
        self.assertEqual(
            settings.supabase_jwks_url,
            "https://egkfbdjelgojryckapcp.supabase.co/auth/v1/.well-known/jwks.json",
        )
        self.assertEqual(settings.supabase_service_role_key, "server-secret")


if __name__ == "__main__":
    unittest.main()
