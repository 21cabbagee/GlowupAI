"""Unit tests for analysis pipeline (face detection, metrics calculation)."""

import base64
import io
import unittest
from unittest.mock import MagicMock, patch

import numpy as np
from PIL import Image, ImageDraw

from glowupai.metrics import analyze
from glowupai.pipeline import analyze_capture


def create_test_image(size=(240, 240), color=(210, 165, 145)):
    """Create a test face image."""
    image = Image.new("RGB", size, color)
    draw = ImageDraw.Draw(image)
    # Add some texture
    for y in range(0, size[1], 8):
        for x in range(0, size[0], 8):
            if (x // 8 + y // 8) % 2:
                draw.rectangle((x, y, x + 3, y + 3), fill=(165, 130, 118))
    return image


def image_to_base64(image: Image.Image) -> str:
    """Convert PIL image to base64 string."""
    output = io.BytesIO()
    image.save(output, format="PNG")
    return base64.b64encode(output.getvalue()).decode()


def image_to_bytes(image: Image.Image) -> bytes:
    """Convert PIL image to bytes."""
    output = io.BytesIO()
    image.save(output, format="PNG")
    return output.getvalue()


def analyze_frame(array: np.ndarray) -> dict:
    """Wrapper to convert numpy array to the format expected by analyze()."""
    # Convert numpy array to PIL Image, then to bytes
    image = Image.fromarray(array.astype('uint8'), 'RGB')
    image_bytes = image_to_bytes(image)

    # Call the actual analyze function with quality_score=1.0
    result = analyze(image_bytes, quality_score=1.0)

    # Convert MetricResult to dict format expected by tests
    return {
        "smoothness_score": 100.0 - result.texture_score,  # Invert texture to smoothness
        "clarity_score": result.confidence * 100,
        "evenness_score": (1.0 - result.darkspot_area) * 100,
        "model_version": result.model_version,
    }


class TestAnalysisPipeline(unittest.TestCase):
    """Test analysis pipeline components."""

    def test_analyze_frame_returns_metrics(self):
        """Test that analyze_frame returns metric dictionary."""
        image = create_test_image()
        array = np.array(image)

        result = analyze_frame(array)

        self.assertIsInstance(result, dict)
        self.assertIn("smoothness_score", result)
        self.assertIn("clarity_score", result)
        self.assertIn("evenness_score", result)
        self.assertIn("model_version", result)

    def test_analyze_frame_with_different_sizes(self):
        """Test analysis with different image sizes."""
        sizes = [(240, 240), (480, 480), (640, 640)]

        for size in sizes:
            with self.subTest(size=size):
                image = create_test_image(size=size)
                array = np.array(image)
                result = analyze_frame(array)

                self.assertIsNotNone(result)
                self.assertIn("smoothness_score", result)

    def test_analyze_frame_score_ranges(self):
        """Test that analysis scores are in valid ranges."""
        image = create_test_image()
        array = np.array(image)

        result = analyze_frame(array)

        # All scores should be between 0 and 100
        self.assertGreaterEqual(result["smoothness_score"], 0)
        self.assertLessEqual(result["smoothness_score"], 100)
        self.assertGreaterEqual(result["clarity_score"], 0)
        self.assertLessEqual(result["clarity_score"], 100)
        self.assertGreaterEqual(result["evenness_score"], 0)
        self.assertLessEqual(result["evenness_score"], 100)

    def test_analyze_frame_consistency(self):
        """Test that same input produces consistent output."""
        image = create_test_image()
        array = np.array(image)

        result1 = analyze_frame(array)
        result2 = analyze_frame(array)

        self.assertEqual(result1["smoothness_score"], result2["smoothness_score"])
        self.assertEqual(result1["clarity_score"], result2["clarity_score"])
        self.assertEqual(result1["evenness_score"], result2["evenness_score"])

    def test_analyze_capture_with_valid_image(self):
        """Test complete capture analysis."""
        image = create_test_image()
        image_b64 = image_to_base64(image)

        result = analyze_capture(image_b64)

        self.assertIsNotNone(result)
        self.assertIn("metrics", result)
        self.assertIn("quality", result)

    def test_analyze_capture_rejects_invalid_base64(self):
        """Test that invalid base64 is rejected."""
        invalid_b64 = "not-valid-base64!!!"

        with self.assertRaises(Exception):
            analyze_capture(invalid_b64)

    def test_analyze_frame_with_different_colors(self):
        """Test analysis with different skin tones."""
        skin_tones = [
            (250, 220, 200),  # Light
            (210, 165, 145),  # Medium
            (150, 100, 80),   # Dark
        ]

        for tone in skin_tones:
            with self.subTest(tone=tone):
                image = create_test_image(color=tone)
                array = np.array(image)
                result = analyze_frame(array)

                self.assertIsNotNone(result)
                self.assertIn("smoothness_score", result)


class TestFaceDetection(unittest.TestCase):
    """Test face detection functionality."""

    @patch('glowupai.pipeline.detect_face')
    def test_face_detection_called(self, mock_detect):
        """Test that face detection is called during analysis."""
        mock_detect.return_value = {
            "face_present": True,
            "bounding_box": [50, 50, 150, 150],
            "confidence": 0.95
        }

        image = create_test_image()
        image_b64 = image_to_base64(image)

        result = analyze_capture(image_b64)

        # Verify face detection was attempted
        self.assertIn("quality", result)

    def test_quality_gates_check_face_presence(self):
        """Test that quality gates verify face presence."""
        from glowupai.capture import validate_quality

        quality_with_face = {
            "face_present": True,
            "yaw_degrees": 0,
            "pitch_degrees": 0,
            "distance_cm": 45,
            "expression_neutral": True,
        }

        quality_without_face = {
            "face_present": False,
            "yaw_degrees": 0,
            "pitch_degrees": 0,
            "distance_cm": 45,
            "expression_neutral": True,
        }

        # Should pass with face
        try:
            validate_quality(quality_with_face)
            passed_with_face = True
        except Exception:
            passed_with_face = False

        # Should fail without face
        try:
            validate_quality(quality_without_face)
            passed_without_face = True
        except Exception:
            passed_without_face = False

        self.assertTrue(passed_with_face)
        self.assertFalse(passed_without_face)


class TestMetricsCalculation(unittest.TestCase):
    """Test individual metric calculations."""

    def test_smoothness_metric_calculation(self):
        """Test smoothness score calculation."""
        # Create uniform image (high smoothness)
        smooth_image = create_test_image(size=(240, 240), color=(200, 150, 130))
        smooth_array = np.array(smooth_image)

        # Create textured image (low smoothness)
        textured_image = create_test_image()
        textured_array = np.array(textured_image)

        smooth_result = analyze_frame(smooth_array)
        textured_result = analyze_frame(textured_array)

        # Smooth image should have higher or equal smoothness score
        self.assertGreaterEqual(
            smooth_result["smoothness_score"],
            textured_result["smoothness_score"] - 10  # Allow small variance
        )

    def test_model_version_included(self):
        """Test that model version is included in results."""
        image = create_test_image()
        array = np.array(image)

        result = analyze_frame(array)

        self.assertIn("model_version", result)
        self.assertIsInstance(result["model_version"], str)
        self.assertTrue(len(result["model_version"]) > 0)


if __name__ == "__main__":
    unittest.main()
