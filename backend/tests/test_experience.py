from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from fastapi.testclient import TestClient

from skinproof.complete_api import create_complete_app
from skinproof.complete_db import FullDatabase
from skinproof.complete_service import CompleteSkinProofService
from skinproof.config import Settings
from skinproof.photos import MemoryPhotoStore


class ExperienceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        db_path = Path(self.temp.name) / "experience.sqlite3"
        self.db = FullDatabase(db_path)
        settings = Settings(db_path=db_path, photo_dir=None, gemini_enabled=False)
        self.service = CompleteSkinProofService(self.db, settings=settings, photos=MemoryPhotoStore())
        self.client = TestClient(create_complete_app(self.service))

    def tearDown(self):
        self.db.close()
        self.temp.cleanup()

    def test_experience_profile_is_persistent_and_validated(self):
        created = self.client.post("/api/users", json={}).json()
        user_id = created["user"]["id"]
        self.assertEqual(created["experience_profile"]["goals"], [])

        response = self.client.patch(
            f"/api/users/{user_id}/profile",
            json={
                "display_name": "Ari",
                "skin_type": "combination",
                "focus_vertical": "skin",
                "goals": ["Redness", "Texture"],
                "experience_level": "steady",
                "onboarding_complete": True,
            },
        )
        self.assertEqual(response.status_code, 200)
        profile = response.json()
        self.assertEqual(profile["experience_profile"]["display_name"], "Ari")
        self.assertEqual(profile["experience_profile"]["goals"], ["Redness", "Texture"])
        self.assertIsNotNone(profile["experience_profile"]["onboarding_completed_at"])
        self.assertEqual(self.client.get(f"/api/users/{user_id}/profile").json()["experience_profile"]["display_name"], "Ari")

        invalid = self.client.patch(f"/api/users/{user_id}/profile", json={"focus_vertical": "teeth"})
        self.assertEqual(invalid.status_code, 400)

    def test_new_consumer_assets_are_served(self):
        home = self.client.get("/")
        css = self.client.get("/assets/styles.css")
        javascript = self.client.get("/assets/app.js")
        self.assertEqual(home.status_code, 200)
        self.assertEqual(css.status_code, 200)
        self.assertEqual(javascript.status_code, 200)
        self.assertIn("Know yourself over time", javascript.text)
        self.assertNotIn("Complete appearance workspace", home.text)


if __name__ == "__main__":
    unittest.main()
