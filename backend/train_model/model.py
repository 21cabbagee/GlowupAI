"""
Skin analysis model architecture.

Uses transfer learning with MobileNetV2 for efficient training and inference.
"""

import torch
import torch.nn as nn
import torchvision.models as models
from typing import Dict


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

    def forward(self, x: torch.Tensor) -> Dict[str, torch.Tensor]:
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

    def predict(self, x: torch.Tensor) -> Dict[str, float]:
        """
        Make predictions for a single image.

        Args:
            x: Input tensor of shape (3, H, W)

        Returns:
            Dictionary of metric predictions
        """
        self.eval()
        with torch.no_grad():
            x = x.unsqueeze(0)  # Add batch dimension
            predictions = self.forward(x)

            # Convert to Python floats
            return {
                "redness": predictions["redness"].item(),
                "blemishes": int(predictions["blemishes"].item()),
                "texture": predictions["texture"].item(),
                "darkspots": predictions["darkspots"].item(),
            }


def count_parameters(model: nn.Module) -> int:
    """Count trainable parameters."""
    return sum(p.numel() for p in model.parameters() if p.requires_grad)


if __name__ == "__main__":
    # Test model
    print("=" * 60)
    print("SKIN ANALYSIS MODEL")
    print("=" * 60)

    model = SkinAnalysisModel(pretrained=True)

    print(f"\n📊 Model Statistics:")
    print(f"   - Parameters: {count_parameters(model):,}")
    print(f"   - Architecture: MobileNetV2 + Multi-task heads")

    # Test forward pass
    print(f"\n🧪 Testing forward pass...")
    x = torch.randn(2, 3, 224, 224)  # Batch of 2 images
    predictions = model(x)

    print(f"✓ Input shape: {x.shape}")
    print(f"✓ Output shapes:")
    for metric, pred in predictions.items():
        print(f"   - {metric}: {pred.shape}")

    print("\n✅ Model test passed!")
