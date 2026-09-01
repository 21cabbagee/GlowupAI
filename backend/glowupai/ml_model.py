"""
ML model integration for skin analysis.

Loads trained PyTorch model and provides inference functionality.
"""

from __future__ import annotations

import io
import logging
import os
from dataclasses import dataclass
from pathlib import Path

import cv2
import numpy as np
import torch
import torch.nn as nn
from PIL import Image

from .face_alignment import align_face_safe
from .metrics import NOISE_FLOORS, MetricResult

logger = logging.getLogger(__name__)


class SkinAnalysisModel(nn.Module):
    """
    Multi-task skin analysis model.

    Predicts 4 metrics:
    - Redness score (0-1)
    - Blemish count (0-100+)
    - Texture score (0-30+)
    - Dark spot area (0-1)
    """

    def __init__(self, pretrained: bool = True):
        super().__init__()

        # Backbone: MobileNetV2 (lightweight, fast)
        import torchvision.models as models
        mobilenet = models.mobilenet_v2(pretrained=pretrained)

        # Remove classification head
        self.features = mobilenet.features
        self.avgpool = nn.AdaptiveAvgPool2d(1)

        # Feature dimension from MobileNetV2
        feature_dim = 1280

        # Shared hidden layer
        self.shared_fc = nn.Sequential(
            nn.Linear(feature_dim, 512),
            nn.ReLU(),
            nn.Dropout(0.3),
        )

        # Task-specific heads
        self.redness_head = nn.Sequential(
            nn.Linear(512, 128),
            nn.ReLU(),
            nn.Linear(128, 1),
            nn.Sigmoid(),  # Output: [0, 1]
        )

        self.blemish_head = nn.Sequential(
            nn.Linear(512, 128),
            nn.ReLU(),
            nn.Linear(128, 1),
            nn.ReLU(),  # Output: [0, ∞) count
        )

        self.texture_head = nn.Sequential(
            nn.Linear(512, 128),
            nn.ReLU(),
            nn.Linear(128, 1),
            nn.ReLU(),  # Output: [0, ∞) score
        )

        self.darkspot_head = nn.Sequential(
            nn.Linear(512, 128),
            nn.ReLU(),
            nn.Linear(128, 1),
            nn.Sigmoid(),  # Output: [0, 1]
        )

    def forward(self, x: torch.Tensor) -> dict[str, torch.Tensor]:
        """
        Forward pass.

        Args:
            x: Input tensor of shape (B, 3, H, W)

        Returns:
            Dictionary of predictions for each metric
        """
        # Extract features
        features = self.features(x)
        features = self.avgpool(features)
        features = torch.flatten(features, 1)

        # Shared representation
        shared = self.shared_fc(features)

        # Task-specific predictions
        predictions = {
            "redness": self.redness_head(shared).squeeze(-1),
            "blemishes": self.blemish_head(shared).squeeze(-1),
            "texture": self.texture_head(shared).squeeze(-1),
            "darkspots": self.darkspot_head(shared).squeeze(-1),
        }

        return predictions


class MLModelInference:
    """Production-ready inference wrapper for trained model."""

    def __init__(self, model_path: str | None = None, device: str = "cpu"):
        """
        Initialize ML model for inference.

        Args:
            model_path: Path to trained model weights. If None, uses default location.
            device: Device to run inference on ("cpu" or "cuda")
        """
        if model_path is None:
            # Default model path
            model_path = os.path.join(
                os.path.dirname(__file__),
                "models",
                "skin_analysis_v2.pth",
            )

        if not os.path.exists(model_path):
            raise FileNotFoundError(
                f"Model file not found: {model_path}\n"
                f"Please run the deployment script to copy the trained model.",
            )

        self.device = torch.device(device)
        self.model_path = model_path

        # Load model
        logger.info(f"Loading ML model from {model_path}")
        self.model = SkinAnalysisModel(pretrained=False)

        try:
            checkpoint = torch.load(model_path, map_location=self.device, weights_only=True)

            # Handle both full checkpoint and state_dict only formats
            if isinstance(checkpoint, dict) and "model_state_dict" in checkpoint:
                self.model.load_state_dict(checkpoint["model_state_dict"])
                logger.info(f"Loaded model checkpoint (val_loss: {checkpoint.get('val_loss', 'N/A')})")
            else:
                self.model.load_state_dict(checkpoint)
                logger.info(f"Loaded model state dict")

            self.model.to(self.device)
            self.model.eval()
            logger.info(f"ML model loaded successfully on {self.device}")

        except Exception as exc:
            logger.error(f"Failed to load model: {exc}")
            raise RuntimeError(f"Failed to load ML model from {model_path}: {exc}") from exc

    def preprocess_image(self, image_bytes: bytes) -> torch.Tensor:
        """
        Preprocess image for model inference.

        Args:
            image_bytes: Raw image bytes

        Returns:
            Preprocessed tensor ready for inference
        """
        # Apply face alignment first (same as deterministic model)
        aligned_bytes = align_face_safe(image_bytes, target_eye_distance=80, output_size=(224, 224))

        # Convert to PIL Image
        with Image.open(io.BytesIO(aligned_bytes)) as img:
            image = img.convert("RGB")

        # Convert to numpy array
        image_np = np.array(image)

        # Normalize to [0, 1]
        image_np = image_np.astype(np.float32) / 255.0

        # Convert to tensor (C, H, W)
        tensor = torch.from_numpy(image_np).permute(2, 0, 1)

        return tensor

    def predict(self, image_bytes: bytes, quality_score: float, baseline: MetricResult | None = None) -> MetricResult:
        """
        Analyze image and return metrics.

        Args:
            image_bytes: Raw image bytes
            quality_score: Image quality score (0-1)
            baseline: Optional baseline metrics for delta calculation

        Returns:
            MetricResult with predictions
        """
        try:
            # Preprocess
            tensor = self.preprocess_image(image_bytes)
            tensor = tensor.unsqueeze(0).to(self.device)

            # Inference
            with torch.no_grad():
                predictions = self.model(tensor)

            # Extract values
            redness = float(predictions["redness"].item())
            blemish_count = float(predictions["blemishes"].item())
            texture = float(predictions["texture"].item())
            darkspot_area = float(predictions["darkspots"].item())

            # Calculate confidence based on quality
            confidence = min(1.0, max(0.0, quality_score))

            # Calculate redness delta if baseline provided
            redness_delta = None
            if baseline is not None:
                redness_delta = redness - baseline.redness_score

            return MetricResult(
                blemish_count=round(blemish_count, 1),
                redness_score=round(redness, 5),
                redness_delta=None if redness_delta is None else round(redness_delta, 5),
                darkspot_area=round(darkspot_area, 5),
                texture_score=round(texture, 3),
                confidence=round(confidence, 3),
                noise_floors=dict(NOISE_FLOORS),
                model_version="ml-v2.0",
            )

        except Exception as exc:
            logger.error(f"ML model inference failed: {exc}")
            raise RuntimeError(f"ML model inference failed: {exc}") from exc


# Global model instance (lazy loaded)
_ml_model: MLModelInference | None = None


def get_ml_model() -> MLModelInference:
    """
    Get or create global ML model instance.

    Returns:
        MLModelInference instance
    """
    global _ml_model
    if _ml_model is None:
        _ml_model = MLModelInference()
    return _ml_model


def analyze_with_ml(
    image_bytes: bytes,
    quality_score: float,
    baseline: MetricResult | None = None,
) -> MetricResult:
    """
    Analyze image using ML model.

    Args:
        image_bytes: Raw image bytes
        quality_score: Image quality score (0-1)
        baseline: Optional baseline metrics for delta calculation

    Returns:
        MetricResult with ML model predictions
    """
    model = get_ml_model()
    return model.predict(image_bytes, quality_score, baseline)
