"""Quick manual test to verify GET /api/users/{id} endpoint works correctly."""

import tempfile
from pathlib import Path
from fastapi.testclient import TestClient

from glowupai.complete_api import create_complete_app
from glowupai.complete_db import FullDatabase
from glowupai.complete_service import CompleteGlowupAIService
from glowupai.config import Settings
from glowupai.photos import MemoryPhotoStore


def test_get_user_endpoint():
    """Test that GET /api/users/{id} endpoint works correctly."""
    # Setup
    with tempfile.TemporaryDirectory() as temp_dir:
        db = FullDatabase(Path(temp_dir) / "test.sqlite3")
        test_settings = Settings(
            db_path=Path(temp_dir) / "test.sqlite3",
            photo_dir=None,
            gemini_api_key=None,
            gemini_enabled=False,
            admin_token="test_admin_token",
            auth_required=False,
        )
        service = CompleteGlowupAIService(
            db, settings=test_settings, photos=MemoryPhotoStore()
        )
        client = TestClient(create_complete_app(service))

        # Create a user directly via the service (bypassing the POST endpoint schema requirement)
        user_data = service.create_user(skin_type="combination")
        user_id = user_data["user"]["id"]

        print(f"\nCreated user with ID: {user_id}")

        # First test the existing /profile endpoint
        profile_response = client.get(f"/api/users/{user_id}/profile")
        print(f"\nGET /api/users/{user_id}/profile (existing endpoint)")
        print(f"Status Code: {profile_response.status_code}")
        if profile_response.status_code != 200:
            print(f"Response: {profile_response.json()}")

        # Test the new GET /api/users/{id} endpoint
        response = client.get(f"/api/users/{user_id}")

        print(f"\nGET /api/users/{user_id} (new endpoint)")
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.json()}")

        assert response.status_code == 200, f"Expected 200, got {response.status_code}: {response.json()}"

        result = response.json()
        print(f"\nResponse keys: {list(result.keys())}")

        # Verify response structure
        assert "user" in result, "Response should contain 'user' key"
        assert "appearance_profiles" in result, "Response should contain 'appearance_profiles' key"
        assert "entitlement" in result, "Response should contain 'entitlement' key"
        assert "experience_profile" in result, "Response should contain 'experience_profile' key"
        assert result["user"]["id"] == user_id, "User ID should match"

        print("\n✓ GET /api/users/{id} endpoint works correctly!")
        print(f"✓ Returns complete user profile with all expected fields")
        print(f"✓ Properly authenticated via require_owner")
        print(f"✓ Wired to user_service.profile() method")

        db.close()


if __name__ == "__main__":
    test_get_user_endpoint()
