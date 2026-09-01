"""
Deploy trained model to production.

Converts PyTorch model to production-ready format and integrates with backend.
"""

import torch
import numpy as np
import cv2
from pathlib import Path
import json
from typing import Dict

from model import SkinAnalysisModel


class ProductionModel:
    """Production-ready inference wrapper."""

    def __init__(self, model_path: str, device: str = "cpu"):
        self.device = torch.device(device)

        # Load model
        self.model = SkinAnalysisModel(pretrained=False)
        checkpoint = torch.load(model_path, map_location=self.device)

        if "model_state_dict" in checkpoint:
            self.model.load_state_dict(checkpoint["model_state_dict"])
        else:
            self.model.load_state_dict(checkpoint)

        self.model.to(self.device)
        self.model.eval()

        print(f"✓ Loaded model from {model_path}")

    def preprocess(self, image_path: str) -> torch.Tensor:
        """Preprocess image for inference."""
        # Load image
        image = cv2.imread(image_path)
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        # Resize to 224x224
        image = cv2.resize(image, (224, 224))

        # Normalize to [0, 1]
        image = image.astype(np.float32) / 255.0

        # Convert to tensor (C, H, W)
        tensor = torch.from_numpy(image).permute(2, 0, 1)

        return tensor

    def predict(self, image_path: str) -> Dict[str, float]:
        """
        Make prediction for an image.

        Args:
            image_path: Path to image file

        Returns:
            Dictionary of metric predictions
        """
        # Preprocess
        tensor = self.preprocess(image_path)
        tensor = tensor.unsqueeze(0).to(self.device)

        # Inference
        with torch.no_grad():
            predictions = self.model(tensor)

        # Convert to dict
        return {
            "redness_score": float(predictions["redness"].item()),
            "blemish_count": int(predictions["blemishes"].item()),
            "texture_score": float(predictions["texture"].item()),
            "darkspot_area": float(predictions["darkspots"].item()),
        }


def export_model():
    """Export model for production use."""
    print("=" * 60)
    print("EXPORTING MODEL FOR PRODUCTION")
    print("=" * 60)

    MODEL_PATH = "/Users/21cabbage/GlowupAI/backend/train_model/checkpoints/best_model.pth"
    OUTPUT_PATH = "/Users/21cabbage/GlowupAI/backend/skinproof/models/skin_analysis_v2.pth"

    # Create output directory
    Path(OUTPUT_PATH).parent.mkdir(exist_ok=True)

    # Load model
    print(f"\n📦 Loading checkpoint...")
    checkpoint = torch.load(MODEL_PATH, map_location="cpu")

    # Save model state dict only (smaller file)
    print(f"\n💾 Saving production model...")
    torch.save(checkpoint["model_state_dict"], OUTPUT_PATH)

    # Get file size
    size_mb = Path(OUTPUT_PATH).stat().st_size / (1024 * 1024)

    print(f"\n✅ Model exported successfully!")
    print(f"📁 Location: {OUTPUT_PATH}")
    print(f"📊 Size: {size_mb:.2f} MB")
    print(f"🎯 Val Loss: {checkpoint['val_loss']:.4f}")

    # Test inference
    print(f"\n🧪 Testing inference...")
    model = ProductionModel(OUTPUT_PATH)

    print(f"✓ Inference test passed!")

    return OUTPUT_PATH


if __name__ == "__main__":
    export_model()
