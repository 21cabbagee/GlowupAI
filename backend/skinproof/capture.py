from __future__ import annotations

import io
from dataclasses import asdict, dataclass

from PIL import Image


@dataclass
class CaptureQuality:
    """Client pose checks plus server-derived image sanity checks."""

    face_present: bool = True
    yaw_degrees: float = 0.0
    pitch_degrees: float = 0.0
    brightness: float = 0.55
    sharpness: float = 0.8
    distance_cm: float = 45.0
    expression_neutral: bool = True
    reference_card_present: bool = False
    score: float = 0.0
    accepted: bool = False
    failed_checks: list[str] | None = None
    coaching: list[dict] | None = None

    @classmethod
    def from_dict(cls, data: dict | None) -> "CaptureQuality":
        data = data or {}
        allowed = {
            "face_present", "yaw_degrees", "pitch_degrees", "brightness",
            "sharpness", "distance_cm", "expression_neutral", "reference_card_present",
        }
        return cls(**{key: data[key] for key in allowed if key in data})

    def evaluate(self) -> "CaptureQuality":
        failures: list[str] = []
        coaching: list[dict] = []
        if not self.face_present:
            failures.append("face_not_detected")
            coaching.append({"check": "face_not_detected", "message": "Center your face in the guide frame before capturing."})
        if abs(self.yaw_degrees) > 12:
            failures.append("turn_head_less")
            direction = "right" if self.yaw_degrees < 0 else "left"
            coaching.append({"check": "turn_head_less", "message": f"Turn {abs(round(self.yaw_degrees))}° back toward center ({direction}) — yaw must stay within 12°."})
        if abs(self.pitch_degrees) > 12:
            failures.append("level_head")
            direction = "down" if self.pitch_degrees < 0 else "up"
            coaching.append({"check": "level_head", "message": f"Tilt your chin {direction} about {abs(round(self.pitch_degrees)) - 12}° to level your head — pitch must stay within 12°."})
        if not 0.20 <= self.brightness <= 0.85:
            failures.append("adjust_lighting")
            if self.brightness < 0.20:
                coaching.append({"check": "adjust_lighting", "message": "It's too dark for a comparable capture — move toward a window or add a front light."})
            else:
                coaching.append({"check": "adjust_lighting", "message": "It's overexposed — step back from direct light or turn away from a bright window."})
        if self.sharpness < 0.35:
            failures.append("hold_phone_steady")
            coaching.append({"check": "hold_phone_steady", "message": "The frame is blurry — brace your elbows against your body and hold still for a second before the shutter."})
        if not 30 <= self.distance_cm <= 80:
            failures.append("move_to_capture_distance")
            if self.distance_cm < 30:
                coaching.append({"check": "move_to_capture_distance", "message": f"You're too close ({round(self.distance_cm)}cm) — move back to 30-80cm from the camera."})
            else:
                coaching.append({"check": "move_to_capture_distance", "message": f"You're too far ({round(self.distance_cm)}cm) — move closer to 30-80cm from the camera."})
        if not self.expression_neutral:
            failures.append("relax_expression")
            coaching.append({"check": "relax_expression", "message": "Relax your expression to neutral — a consistent expression keeps captures comparable."})

        self.coaching = coaching
        components = [
            1.0 if self.face_present else 0.0,
            max(0.0, 1.0 - abs(self.yaw_degrees) / 30.0),
            max(0.0, 1.0 - abs(self.pitch_degrees) / 30.0),
            max(0.0, 1.0 - abs(self.brightness - 0.52) / 0.52),
            min(1.0, max(0.0, self.sharpness)),
            1.0 if 30 <= self.distance_cm <= 80 else 0.0,
            1.0 if self.expression_neutral else 0.0,
        ]
        self.score = round(sum(components) / len(components), 3)
        self.failed_checks = failures
        self.accepted = not failures and self.score >= 0.75
        return self

    def as_dict(self) -> dict:
        return asdict(self)


def _pixels(image: Image.Image) -> list[tuple[int, int, int]]:
    # Pillow renamed getdata in its newer releases; retain compatibility with
    # the older versions used by mobile/backend development environments.
    flattened = getattr(image, "get_flattened_data", None)
    return list(flattened() if flattened else image.getdata())


def inspect_image(image_bytes: bytes) -> dict:
    """Return server-authoritative image checks before metric extraction."""

    with Image.open(io.BytesIO(image_bytes)) as original:
        image = original.convert("RGB")
        width, height = image.size
        if width < 160 or height < 160:
            raise ValueError("image must be at least 160x160 pixels")
        sample = image.resize((64, 64))
        pixels = _pixels(sample)
        luminances = [(r * 299 + g * 587 + b * 114) / 1000 / 255 for r, g, b in pixels]
        brightness = sum(luminances) / len(luminances)
        sharpness_samples: list[float] = []
        for y in range(1, 63):
            for x in range(1, 63):
                here = luminances[y * 64 + x]
                right = luminances[y * 64 + x + 1]
                down = luminances[(y + 1) * 64 + x]
                sharpness_samples.append(abs(here - right) + abs(here - down))
        sharpness = min(1.0, (sum(sharpness_samples) / len(sharpness_samples)) * 8.0)
        return {"brightness": round(brightness, 3), "sharpness": round(sharpness, 3), "width": width, "height": height}


def merge_quality(client_quality: dict | None, image_bytes: bytes) -> CaptureQuality:
    image_values = inspect_image(image_bytes)
    # Pose/face fields come from the on-device mesh. Brightness and sharpness
    # are intentionally overwritten with server measurements so callers cannot
    # claim that an unusable frame passed the comparability gate.
    merged = {**(client_quality or {}), "brightness": image_values["brightness"], "sharpness": image_values["sharpness"]}
    return CaptureQuality.from_dict(merged).evaluate()
