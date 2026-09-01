"""Verify that the GET /api/users/{id} endpoint is properly registered."""

import tempfile
from pathlib import Path

from glowupai.complete_api import create_complete_app
from glowupai.complete_db import FullDatabase
from glowupai.complete_service import CompleteGlowupAIService
from glowupai.config import Settings
from glowupai.photos import MemoryPhotoStore


def verify_routes():
    """Check that all users routes are registered."""
    with tempfile.TemporaryDirectory() as temp_dir:
        db = FullDatabase(Path(temp_dir) / "test.sqlite3")
        test_settings = Settings(
            db_path=Path(temp_dir) / "test.sqlite3",
            photo_dir=None,
            gemini_api_key=None,
            gemini_enabled=False,
            admin_token="test_admin",
            auth_required=False,
        )
        service = CompleteGlowupAIService(
            db, settings=test_settings, photos=MemoryPhotoStore()
        )
        app = create_complete_app(service)

        print("\n" + "="*80)
        print("REGISTERED USERS ENDPOINTS")
        print("="*80)

        users_routes = []
        for route in app.routes:
            if hasattr(route, 'path') and hasattr(route, 'methods'):
                path = route.path
                if '/users/' in path or path.endswith('/users'):
                    for method in route.methods:
                        if method != 'HEAD':  # Skip HEAD methods
                            users_routes.append((method, path, route.name))

        # Sort by path and method
        users_routes.sort(key=lambda x: (x[1], x[0]))

        for method, path, name in users_routes:
            marker = "✓ NEW" if path == "/api/users/{id}" else ""
            print(f"  {method:7s} {path:50s} {name:30s} {marker}")

        print("="*80)
        print(f"\nTotal users endpoints: {len(users_routes)}")

        # Check if our new endpoint is registered
        new_endpoint_found = any(path == "/api/users/{id}" and method == "GET"
                                for method, path, _ in users_routes)

        if new_endpoint_found:
            print("\n✓ SUCCESS: GET /api/users/{id} endpoint is properly registered!")
        else:
            print("\n✗ ERROR: GET /api/users/{id} endpoint NOT found!")

        db.close()


if __name__ == "__main__":
    verify_routes()
