"""Keep repository tests offline even when a developer has local API keys."""

import os
import tempfile
from pathlib import Path


os.environ.setdefault("GLOWUPAI_DISABLE_LOCAL_ENV", "1")

# App modules instantiate a default application during collection. Never let
# that import open or migrate a developer's real database or image directory.
_test_data = tempfile.TemporaryDirectory(prefix="glowupai-pytest-")
os.environ["GLOWUPAI_DB_PATH"] = str(Path(_test_data.name) / "app.sqlite3")
os.environ["GLOWUPAI_PHOTO_DIR"] = str(Path(_test_data.name) / "photos")
# Runtime kill switches are deployment state. Tests must never inherit a local
# incident switch (for example, a temporary capture pause) or they stop testing
# the user journey and become order/environment dependent.
os.environ["GLOWUPAI_CONTROLS_FILE"] = str(Path(_test_data.name) / "runtime_controls.json")
