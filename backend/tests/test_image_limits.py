from __future__ import annotations

import pytest
from pydantic import ValidationError

from glowupai.routers.captures import (
    CaptureCreate,
    MAX_IMAGE_BASE64_CHARS,
    ShelfScanCreate,
)


@pytest.mark.parametrize(
    "model,payload",
    [
        (CaptureCreate, {"user_id": "user-1"}),
        (ShelfScanCreate, {}),
    ],
)
def test_image_request_models_reject_oversized_base64_before_decode(model, payload):
    with pytest.raises(ValidationError):
        model.model_validate(
            {
                **payload,
                "image_base64": "A" * (MAX_IMAGE_BASE64_CHARS + 1),
            }
        )
