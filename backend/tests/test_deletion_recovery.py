import pytest

from glowupai.deletion import AccountDeletion
from glowupai.full_db import FullDatabase
from glowupai.photos import MemoryPhotoStore


def test_deletion_recovers_after_storage_failure(tmp_path):
    db = FullDatabase(tmp_path / "delete.sqlite3")
    db.execute("INSERT INTO users (id) VALUES ('owner')")
    photos = MemoryPhotoStore()
    original_delete = photos.delete_user
    reference = photos.save("owner", "photo", b"private")
    photos.delete_user = lambda user: (_ for _ in ()).throw(OSError("offline"))
    try:
        with pytest.raises(OSError):
            AccountDeletion(db, photos).request("owner")
        assert db.fetchone("SELECT deleted_at FROM users WHERE id='owner'")[
            "deleted_at"
        ]
        assert db.fetchone("SELECT * FROM deletion_requests WHERE user_id='owner'")
        photos.delete_user = original_delete
        assert AccountDeletion(db, photos).run_pending() == 1
        assert db.fetchone("SELECT * FROM users WHERE id='owner'") is None
        assert db.fetchone("SELECT * FROM deletion_requests") is None
        with pytest.raises(KeyError):
            photos.read(reference)
        AccountDeletion(db, photos).request("owner")
    finally:
        db.close()
