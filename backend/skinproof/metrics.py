from __future__ import annotations

import io
from dataclasses import asdict, dataclass

from PIL import Image


NOISE_FLOORS = {"blemish_count": 1.5, "redness_score": 0.015, "darkspot_area": 0.008, "texture_score": 1.5}


@dataclass
class MetricResult:
    blemish_count: float
    redness_score: float
    redness_delta: float | None
    darkspot_area: float
    texture_score: float
    confidence: float
    noise_floors: dict[str, float]
    model_version: str

    def as_dict(self) -> dict:
        result = asdict(self)
        result["noise_floor"] = result.pop("noise_floors")
        return result


def _skin_pixel(r: int, g: int, b: int) -> bool:
    return r > 45 and g > 25 and b > 15 and r >= g * 0.78 and r >= b * 1.03 and max(r, g, b) - min(r, g, b) > 12


def _luminance(r: int, g: int, b: int) -> float:
    return (r * 299 + g * 587 + b * 114) / 1000


def _pixels(image: Image.Image) -> list[tuple[int, int, int]]:
    flattened = getattr(image, "get_flattened_data", None)
    return list(flattened() if flattened else image.getdata())


def analyze(image_bytes: bytes, quality_score: float, baseline: MetricResult | None = None, model_version: str = "deterministic-3.0") -> MetricResult:
    """Compute transparent longitudinal cosmetic measurements."""

    with Image.open(io.BytesIO(image_bytes)) as original:
        image = original.convert("RGB").resize((96, 96))
    pixels = _pixels(image)
    points: list[tuple[int, int, int, int, int]] = []
    for index, (r, g, b) in enumerate(pixels):
        x, y = index % 96, index // 96
        nx = (x - 47.5) / 39.0
        ny = (y - 47.5) / 44.0
        if nx * nx + ny * ny <= 1.0 and _skin_pixel(r, g, b):
            points.append((x, y, r, g, b))
    if len(points) < 50:
        return MetricResult(0.0, 0.0, None, 0.0, 0.0, min(0.25, quality_score), dict(NOISE_FLOORS), model_version)

    reds = [(r - g) / 255 for _, _, r, g, _ in points]
    redness = max(0.0, sum(max(0.0, value) for value in reds) / len(reds))
    luminances = sorted(_luminance(r, g, b) for _, _, r, g, b in points)
    median_luminance = luminances[len(luminances) // 2]
    dark_threshold = median_luminance * 0.72
    darkspot_area = sum(1 for _, _, r, g, b in points if _luminance(r, g, b) < dark_threshold) / len(points)

    candidate = {(x, y) for x, y, r, g, b in points if r - g > 42 and _luminance(r, g, b) > 55}
    blemishes = 0
    while candidate:
        seed = candidate.pop()
        stack = [seed]
        component_size = 1
        while stack:
            x, y = stack.pop()
            for neighbor in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if neighbor in candidate:
                    candidate.remove(neighbor)
                    stack.append(neighbor)
                    component_size += 1
        if 2 <= component_size <= 80:
            blemishes += 1

    texture_diffs: list[float] = []
    for y in range(1, 95):
        for x in range(1, 95):
            current = _luminance(*pixels[y * 96 + x])
            right = _luminance(*pixels[y * 96 + x + 1])
            down = _luminance(*pixels[(y + 1) * 96 + x])
            texture_diffs.append((abs(current - right) + abs(current - down)) / 2)
    texture = min(100.0, sum(texture_diffs) / len(texture_diffs) * 1.8)
    confidence = min(1.0, max(0.0, quality_score)) * min(1.0, len(points) / 2500)
    redness_delta = None if baseline is None else redness - baseline.redness_score
    return MetricResult(float(blemishes), round(redness, 5), None if redness_delta is None else round(redness_delta, 5), round(darkspot_area, 5), round(texture, 3), round(confidence, 3), dict(NOISE_FLOORS), model_version)
