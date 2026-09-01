"""
Test trained model for consistency.
"""

import torch
import numpy as np
import cv2
from pathlib import Path
from deploy_model import ProductionModel


def test_consistency():
    """Test if model gives consistent predictions for same image."""
    print("=" * 60)
    print("TESTING MODEL CONSISTENCY")
    print("=" * 60)

    MODEL_PATH = "/Users/21cabbage/GlowupAI/backend/train_model/checkpoints/best_model.pth"
    TEST_IMAGE = "/Users/21cabbage/GlowupAI/backend/train_model/data/images"

    # Check if model exists
    if not Path(MODEL_PATH).exists():
        print(f"❌ Model not found: {MODEL_PATH}")
        print("   Run training first: ./RUN_TRAINING.sh")
        return

    # Load model
    print("\n📦 Loading model...")
    model = ProductionModel(MODEL_PATH)

    # Find test image
    test_images = list(Path(TEST_IMAGE).glob("*.jpg"))
    if not test_images:
        print(f"❌ No test images found in {TEST_IMAGE}")
        return

    test_image = str(test_images[0])
    print(f"📸 Test image: {Path(test_image).name}")

    # Run multiple predictions
    print(f"\n🧪 Running 5 predictions on same image...")
    predictions = []

    for i in range(5):
        pred = model.predict(test_image)
        predictions.append(pred)
        print(f"   Run {i+1}: Redness={pred['redness_score']:.4f}, "
              f"Blemishes={pred['blemish_count']}, "
              f"Texture={pred['texture_score']:.2f}")

    # Calculate variance
    print(f"\n📊 Consistency Analysis:")

    metrics = ["redness_score", "blemish_count", "texture_score", "darkspot_area"]
    for metric in metrics:
        values = [p[metric] for p in predictions]
        mean = np.mean(values)
        std = np.std(values)
        variance = np.var(values)

        print(f"   {metric}:")
        print(f"      Mean: {mean:.4f}")
        print(f"      Std:  {std:.4f}")
        print(f"      Var:  {variance:.6f}")

        if variance < 0.01:
            print(f"      ✅ Excellent consistency!")
        elif variance < 0.1:
            print(f"      ✓ Good consistency")
        else:
            print(f"      ⚠️  High variance - may need more training")

    print("\n" + "=" * 60)
    print("✅ CONSISTENCY TEST COMPLETE")
    print("=" * 60)


if __name__ == "__main__":
    test_consistency()
