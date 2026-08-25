from __future__ import annotations

import base64
import hashlib
import hmac
import os
from pathlib import Path
from typing import Protocol
from urllib.parse import parse_qs, quote


class PhotoStore(Protocol):
    def save(self, user_id: str, capture_id: str, data: bytes) -> str: ...
    def read(self, reference: str) -> bytes: ...
    def delete_user(self, user_id: str) -> None: ...


class MemoryPhotoStore:
    """Default local/demo store. Raw bytes never enter SQLite or logs."""

    def __init__(self) -> None:
        self._items: dict[str, bytes] = {}

    def save(self, user_id: str, capture_id: str, data: bytes) -> str:
        reference = f"memory://{user_id}/{capture_id}"
        self._items[reference] = bytes(data)
        return reference

    def read(self, reference: str) -> bytes:
        return self._items[reference]

    def delete_user(self, user_id: str) -> None:
        prefix = f"memory://{user_id}/"
        for reference in list(self._items):
            if reference.startswith(prefix):
                del self._items[reference]


class EncryptedFilePhotoStore:
    """AES-GCM object store for deployments with ``cryptography`` installed.

    ``SKINPROOF_PHOTO_KEY`` is a base64-encoded 32-byte root key. A distinct
    per-user key is derived with HMAC and each object gets a fresh nonce.
    """

    def __init__(self, root: str | Path, root_key: bytes) -> None:
        try:
            from cryptography.hazmat.primitives.ciphers.aead import AESGCM
        except ImportError as exc:  # pragma: no cover - exercised in deployment
            raise RuntimeError("Encrypted photo storage requires the 'cryptography' package") from exc
        if len(root_key) != 32:
            raise ValueError("photo root key must be exactly 32 bytes")
        self.root = Path(root)
        self.root.mkdir(parents=True, exist_ok=True)
        self.root_key = root_key
        self._aesgcm = AESGCM

    def _user_hash(self, user_id: str) -> str:
        return hashlib.sha256(user_id.encode()).hexdigest()

    def _key(self, user_id: str) -> bytes:
        return hmac.new(self.root_key, user_id.encode(), hashlib.sha256).digest()

    def _path(self, user_id: str, capture_id: str) -> Path:
        return self.root / self._user_hash(user_id) / f"{capture_id}.bin"

    def save(self, user_id: str, capture_id: str, data: bytes) -> str:
        path = self._path(user_id, capture_id)
        path.parent.mkdir(parents=True, exist_ok=True)
        nonce = os.urandom(12)
        ciphertext = self._aesgcm(self._key(user_id)).encrypt(nonce, data, capture_id.encode())
        path.write_bytes(nonce + ciphertext)
        # Keep the canonical Windows path in the reference instead of asking
        # urlparse to reinterpret drive letters as a URI authority.
        return f"file://{path.as_posix()}?user_id={quote(user_id)}&capture_id={quote(capture_id)}"

    def read(self, reference: str) -> bytes:
        if not reference.startswith("file://"):
            raise ValueError("encrypted photo reference must use the file scheme")
        raw_path, _, raw_query = reference.removeprefix("file://").partition("?")
        values = parse_qs(raw_query)
        user_id = values.get("user_id", [None])[0]
        capture_id = values.get("capture_id", [None])[0]
        if not user_id or not capture_id:
            raise ValueError("encrypted photo reference is missing authenticated lookup metadata")
        expected_path = self._path(user_id, capture_id).resolve()
        if Path(raw_path).resolve() != expected_path:
            raise ValueError("encrypted photo reference path does not match its authenticated metadata")
        blob = expected_path.read_bytes()
        return self._aesgcm(self._key(user_id)).decrypt(blob[:12], blob[12:], capture_id.encode())

    def read_for_user(self, user_id: str, capture_id: str) -> bytes:
        reference = f"file://{self._path(user_id, capture_id).as_posix()}?user_id={quote(user_id)}&capture_id={quote(capture_id)}"
        return self.read(reference)

    def delete_user(self, user_id: str) -> None:
        user_dir = self.root / self._user_hash(user_id)
        if user_dir.exists():
            for path in user_dir.glob("*.bin"):
                path.unlink()
            user_dir.rmdir()


def build_photo_store(photo_dir: Path | None) -> PhotoStore:
    """Use encrypted local storage only when explicitly configured correctly."""

    encoded_key = os.getenv("SKINPROOF_PHOTO_KEY", "").strip()
    if photo_dir and encoded_key:
        try:
            key = base64.b64decode(encoded_key, validate=True)
            return EncryptedFilePhotoStore(photo_dir, key)
        except (ValueError, TypeError) as exc:
            raise RuntimeError("SKINPROOF_PHOTO_KEY must be valid base64 for 32 bytes") from exc
    return MemoryPhotoStore()
