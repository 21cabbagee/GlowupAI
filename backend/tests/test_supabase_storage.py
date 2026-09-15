from __future__ import annotations

import json
from io import BytesIO
from urllib.error import HTTPError

import pytest

from glowupai.supabase_auth_admin import SupabaseAuthAdmin
from glowupai.photos import build_photo_store
from glowupai.supabase_storage import SupabasePhotoStore, SupabaseStorageError


class FakeResponse:
    def __init__(self, body: bytes):
        self.body = body

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def read(self):
        return self.body


class FakeStorage:
    def __init__(self):
        self.objects: dict[str, bytes] = {}
        self.requests: list[tuple[str, str, bytes | None]] = []

    def __call__(self, request, timeout=30):
        body = request.data
        self.requests.append((request.method, request.full_url, body))
        if "/object/list/" in request.full_url:
            payload = json.loads(body)
            prefix = payload["prefix"]
            names = [key for key in self.objects if key.startswith(prefix)]
            return FakeResponse(json.dumps([{"name": name} for name in names]).encode())
        if "/object/remove/" in request.full_url:
            for path in json.loads(body)["prefixes"]:
                self.objects.pop(path, None)
            return FakeResponse(b"[]")
        marker = "/storage/v1/object/private/"
        path = request.full_url.split(marker, 1)[1]
        if request.method == "POST":
            self.objects[path] = body or b""
            return FakeResponse(b"{}")
        return FakeResponse(self.objects[path])


def test_supabase_private_object_paths_round_trip_without_public_urls():
    fake = FakeStorage()
    store = SupabasePhotoStore(
        "https://project.supabase.co",
        "server-only-service-role-key",
        "private",
        opener=fake,
    )
    path = store.save("alice", "capture-1", b"private photo")
    assert path.startswith("users/")
    assert path.endswith("/raw.bin")
    assert "supabase.co" not in path
    assert store.read(path) == b"private photo"
    with pytest.raises(ValueError):
        store.read(path.replace("users/", "other/", 1))


def test_deletion_is_scoped_to_one_user():
    fake = FakeStorage()
    store = SupabasePhotoStore(
        "https://project.supabase.co", "server-key", "private", opener=fake
    )
    alice = store.save("alice", "one", b"one")
    bob = store.save("bob", "one", b"keep")
    store.delete_user("alice")
    assert alice not in fake.objects
    assert store.read(bob) == b"keep"


def test_auth_admin_uses_service_role_only_on_the_backend():
    requests = []

    def opener(request, timeout=30):
        requests.append(request)
        return FakeResponse(b"{}")

    SupabaseAuthAdmin(
        "https://project.supabase.co",
        "server-only-service-role-key",
        opener=opener,
    ).delete_user("supabase-user")

    assert requests[0].method == "DELETE"
    assert requests[0].full_url.endswith("/auth/v1/admin/users/supabase-user")
    assert requests[0].headers["Authorization"].endswith("server-only-service-role-key")


def test_photo_store_accepts_current_supabase_secret_key_alias(monkeypatch):
    monkeypatch.setenv("SUPABASE_URL", "https://project.supabase.co")
    monkeypatch.delenv("SUPABASE_SERVICE_ROLE_KEY", raising=False)
    monkeypatch.setenv("SUPABASE_SECRET_KEY", "server-only-secret-key")
    store = build_photo_store(None)
    assert isinstance(store, SupabasePhotoStore)
    assert store.service_role_key == "server-only-secret-key"


def test_new_supabase_secret_key_is_sent_only_as_apikey():
    """Opaque ``sb_secret`` keys are not JWTs and must not be Bearer tokens."""
    requests = []

    def opener(request, timeout=30):
        requests.append(request)
        return FakeResponse(b"{}")

    SupabasePhotoStore(
        "https://project.supabase.co",
        "sb_secret_test-key",
        "private",
        opener=opener,
    ).save("alice", "capture-1", b"private photo")

    headers = {name.casefold(): value for name, value in requests[0].headers.items()}
    assert headers["apikey"] == "sb_secret_test-key"
    assert "authorization" not in headers


def test_storage_error_includes_safe_api_error_detail():
    def opener(request, timeout=30):
        raise HTTPError(
            request.full_url,
            400,
            "Bad request",
            hdrs=None,
            fp=BytesIO(b'{"message":"Bucket not found"}'),
        )

    store = SupabasePhotoStore(
        "https://project.supabase.co",
        "server-only-service-role-key",
        "private",
        opener=opener,
    )
    with pytest.raises(SupabaseStorageError, match="400.*Bucket not found"):
        store.save("alice", "capture-1", b"private photo")


def test_save_creates_missing_private_bucket_then_retries_upload():
    requests = []
    bucket_created = False

    def opener(request, timeout=30):
        nonlocal bucket_created
        requests.append(request)
        if request.full_url.endswith("/storage/v1/bucket"):
            bucket_created = True
            return FakeResponse(b"{}")
        if not bucket_created:
            raise HTTPError(
                request.full_url,
                400,
                "Bad request",
                hdrs=None,
                fp=BytesIO(b'{"message":"Bucket not found"}'),
            )
        return FakeResponse(b"{}")

    store = SupabasePhotoStore(
        "https://project.supabase.co",
        "server-only-service-role-key",
        "private",
        opener=opener,
    )
    assert store.save("alice", "capture-1", b"private photo").endswith("/raw.bin")
    assert [request.method for request in requests] == ["POST", "POST", "POST"]
    assert requests[1].full_url.endswith("/storage/v1/bucket")
    assert json.loads(requests[1].data) == {
        "id": "private",
        "name": "private",
        "public": False,
    }
