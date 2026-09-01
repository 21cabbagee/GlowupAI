"""Unit tests for ML model inference."""

from __future__ import annotations

import io
import tempfile
import unittest
from pathlib import Path
from unittest.mock import MagicMock, Mock, patch

import numpy as np
import pytest
from PIL import Image

# Skip all tests in this module if torch is not available
torch = pytest.importorskip("torch", reason="torch not installed")

# Import after torch check to avoid import errors
try:
    from glowupai.metrics import MetricResult
    from glowupai.ml_model import (
        MLModelInference,
        SkinAnalysisModel,
        analyze_with_ml,
        get_ml_model,
    )
except ImportError:
    # If imports fail (e.g., due to torch dependency), skip all tests
    pytest.skip("ML model dependencies not available", allow_module_level=True)


def create_test_image(width: int = 224, height: int = 224) -> bytes:
    """Create a test image for inference."""
    image = Image.new("RGB", (width, height), (210, 165, 145))
    output = io.BytesIO()
    image.save(output, format="JPEG")
    return output.getvalue()


class TestSkinAnalysisModel(unittest.TestCase):
    """Test SkinAnalysisModel architecture."""

    def test_model_initialization(self):
        """Test model can be initialized."""
        model = SkinAnalysisModel(pretrained=False)
        self.assertIsNotNone(model)
        self.assertIsNotNone(model.features)
        self.assertIsNotNone(model.redness_head)
        self.assertIsNotNone(model.blemish_head)
        self.assertIsNotNone(model.texture_head)
        self.assertIsNotNone(model.darkspot_head)

    def test_model_forward_pass(self):
        """Test forward pass produces correct output shape."""
        model = SkinAnalysisModel(pretrained=False)
        model.eval()

        # Create dummy input
        batch_size = 2
        x = torch.randn(batch_size, 3, 224, 224)

        with torch.no_grad():
            predictions = model(x)

        # Check output structure
        self.assertIn("redness", predictions)
        self.assertIn("blemishes", predictions)
        self.assertIn("texture", predictions)
        self.assertIn("darkspots", predictions)

        # Check output shapes
        self.assertEqual(predictions["redness"].shape, (batch_size,))
        self.assertEqual(predictions["blemishes"].shape, (batch_size,))
        self.assertEqual(predictions["texture"].shape, (batch_size,))
        self.assertEqual(predictions["darkspots"].shape, (batch_size,))

    def test_model_output_ranges(self):
        """Test model outputs are in expected ranges."""
        model = SkinAnalysisModel(pretrained=False)
        model.eval()

        x = torch.randn(1, 3, 224, 224)

        with torch.no_grad():
            predictions = model(x)

        # Redness and darkspots should be in [0, 1] due to sigmoid
        self.assertTrue(0 <= predictions["redness"].item() <= 1)
        self.assertTrue(0 <= predictions["darkspots"].item() <= 1)

        # Blemishes and texture should be non-negative due to ReLU
        self.assertTrue(predictions["blemishes"].item() >= 0)
        self.assertTrue(predictions["texture"].item() >= 0)


class TestMLModelInference(unittest.TestCase):
    """Test MLModelInference class."""

    def setUp(self):
        """Set up test fixtures."""
        self.temp_dir = tempfile.TemporaryDirectory()
        self.model_path = Path(self.temp_dir.name) / "test_model.pth"

    def tearDown(self):
        """Clean up test fixtures."""
        self.temp_dir.cleanup()

    def _create_mock_model(self):
        """Create a mock model checkpoint."""
        model = SkinAnalysisModel(pretrained=False)
        checkpoint = {
            "model_state_dict": model.state_dict(),
            "val_loss": 0.123,
        }
        torch.save(checkpoint, self.model_path)

    def test_model_loading_with_checkpoint(self):
        """Test model loads successfully from checkpoint."""
        self._create_mock_model()

        inference = MLModelInference(model_path=str(self.model_path), device="cpu")

        self.assertIsNotNone(inference.model)
        self.assertEqual(str(inference.device), "cpu")
        self.assertEqual(inference.model_path, str(self.model_path))

    def test_model_loading_with_state_dict_only(self):
        """Test model loads from state_dict without checkpoint wrapper."""
        model = SkinAnalysisModel(pretrained=False)
        torch.save(model.state_dict(), self.model_path)

        inference = MLModelInference(model_path=str(self.model_path), device="cpu")

        self.assertIsNotNone(inference.model)

    def test_model_loading_missing_file(self):
        """Test model loading fails with missing file."""
        with self.assertRaises(FileNotFoundError) as context:
            MLModelInference(model_path="/nonexistent/model.pth")

        self.assertIn("Model file not found", str(context.exception))

    def test_model_loading_corrupted_file(self):
        """Test model loading fails with corrupted checkpoint."""
        # Write invalid data to model file
        with open(self.model_path, "wb") as f:
            f.write(b"corrupted data")

        with self.assertRaises(RuntimeError) as context:
            MLModelInference(model_path=str(self.model_path))

        self.assertIn("Failed to load ML model", str(context.exception))

    @patch("glowupai.ml_model.align_face_safe")
    def test_preprocess_image(self, mock_align):
        """Test image preprocessing."""
        self._create_mock_model()
        inference = MLModelInference(model_path=str(self.model_path))

        test_image = create_test_image()
        mock_align.return_value = test_image

        tensor = inference.preprocess_image(test_image)

        # Check tensor properties
        self.assertIsInstance(tensor, torch.Tensor)
        self.assertEqual(tensor.shape, (3, 224, 224))
        self.assertTrue(torch.all(tensor >= 0))
        self.assertTrue(torch.all(tensor <= 1))

        # Verify face alignment was called
        mock_align.assert_called_once()

    @patch("glowupai.ml_model.align_face_safe")
    def test_predict_success(self, mock_align):
        """Test successful prediction."""
        self._create_mock_model()
        inference = MLModelInference(model_path=str(self.model_path))

        test_image = create_test_image()
        mock_align.return_value = test_image

        result = inference.predict(test_image, quality_score=0.85)

        # Check result type
        self.assertIsInstance(result, MetricResult)

        # Check all metrics are present
        self.assertIsNotNone(result.blemish_count)
        self.assertIsNotNone(result.redness_score)
        self.assertIsNotNone(result.darkspot_area)
        self.assertIsNotNone(result.texture_score)
        self.assertIsNotNone(result.confidence)

        # Check confidence matches quality score
        self.assertEqual(result.confidence, 0.85)

        # Check model version
        self.assertEqual(result.model_version, "ml-v2.0")

    @patch("glowupai.ml_model.align_face_safe")
    def test_predict_with_baseline(self, mock_align):
        """Test prediction with baseline for delta calculation."""
        self._create_mock_model()
        inference = MLModelInference(model_path=str(self.model_path))

        test_image = create_test_image()
        mock_align.return_value = test_image

        baseline = MetricResult(
            blemish_count=5.0,
            redness_score=0.5,
            redness_delta=None,
            darkspot_area=0.2,
            texture_score=10.0,
            confidence=0.9,
            noise_floors={},
            model_version="test",
        )

        result = inference.predict(test_image, quality_score=0.9, baseline=baseline)

        # Check redness_delta is calculated
        self.assertIsNotNone(result.redness_delta)
        expected_delta = result.redness_score - baseline.redness_score
        self.assertAlmostEqual(result.redness_delta, expected_delta, places=4)

    @patch("glowupai.ml_model.align_face_safe")
    def test_predict_quality_score_clamping(self, mock_align):
        """Test quality score is clamped to [0, 1]."""
        self._create_mock_model()
        inference = MLModelInference(model_path=str(self.model_path))

        test_image = create_test_image()
        mock_align.return_value = test_image

        # Test with quality > 1
        result = inference.predict(test_image, quality_score=1.5)
        self.assertEqual(result.confidence, 1.0)

        # Test with quality < 0
        result = inference.predict(test_image, quality_score=-0.5)
        self.assertEqual(result.confidence, 0.0)

    @patch("glowupai.ml_model.align_face_safe")
    def test_predict_error_handling(self, mock_align):
        """Test prediction error handling."""
        self._create_mock_model()
        inference = MLModelInference(model_path=str(self.model_path))

        # Simulate alignment failure
        mock_align.side_effect = Exception("Face alignment failed")

        with self.assertRaises(RuntimeError) as context:
            inference.predict(create_test_image(), quality_score=0.8)

        self.assertIn("ML model inference failed", str(context.exception))

    def test_output_validation(self):
        """Test that predictions are properly rounded."""
        self._create_mock_model()
        inference = MLModelInference(model_path=str(self.model_path))

        with patch.object(inference, "preprocess_image") as mock_preprocess:
            mock_tensor = torch.randn(3, 224, 224)
            mock_preprocess.return_value = mock_tensor

            result = inference.predict(create_test_image(), quality_score=0.85)

            # Check rounding
            self.assertEqual(result.blemish_count, round(result.blemish_count, 1))
            self.assertEqual(result.redness_score, round(result.redness_score, 5))
            self.assertEqual(result.darkspot_area, round(result.darkspot_area, 5))
            self.assertEqual(result.texture_score, round(result.texture_score, 3))
            self.assertEqual(result.confidence, round(result.confidence, 3))


class TestGlobalModelInstance(unittest.TestCase):
    """Test global model instance management."""

    def test_get_ml_model_singleton(self):
        """Test get_ml_model returns singleton instance."""
        with patch("glowupai.ml_model.MLModelInference") as mock_inference_class:
            mock_instance = Mock()
            mock_inference_class.return_value = mock_instance

            # Reset global state
            import glowupai.ml_model as ml_module

            ml_module._ml_model = None

            # First call creates instance
            result1 = get_ml_model()
            self.assertEqual(result1, mock_instance)
            mock_inference_class.assert_called_once()

            # Second call returns same instance
            result2 = get_ml_model()
            self.assertEqual(result2, mock_instance)
            self.assertEqual(result1, result2)
            # Still only called once
            mock_inference_class.assert_called_once()

    def test_analyze_with_ml(self):
        """Test analyze_with_ml convenience function."""
        with patch("glowupai.ml_model.get_ml_model") as mock_get_model:
            mock_model = Mock()
            mock_result = MetricResult(
                blemish_count=10.0,
                redness_score=0.3,
                redness_delta=None,
                darkspot_area=0.1,
                texture_score=5.0,
                confidence=0.9,
                noise_floors={},
                model_version="ml-v2.0",
            )
            mock_model.predict.return_value = mock_result
            mock_get_model.return_value = mock_model

            test_image = create_test_image()
            result = analyze_with_ml(test_image, quality_score=0.9)

            self.assertEqual(result, mock_result)
            mock_model.predict.assert_called_once_with(test_image, 0.9, None)

    def test_analyze_with_ml_with_baseline(self):
        """Test analyze_with_ml with baseline."""
        with patch("glowupai.ml_model.get_ml_model") as mock_get_model:
            mock_model = Mock()
            mock_model.predict.return_value = Mock()
            mock_get_model.return_value = mock_model

            baseline = MetricResult(
                blemish_count=5.0,
                redness_score=0.5,
                redness_delta=None,
                darkspot_area=0.2,
                texture_score=10.0,
                confidence=0.9,
                noise_floors={},
                model_version="test",
            )

            test_image = create_test_image()
            analyze_with_ml(test_image, quality_score=0.8, baseline=baseline)

            mock_model.predict.assert_called_once_with(test_image, 0.8, baseline)


if __name__ == "__main__":
    unittest.main()
