#!/usr/bin/env python3
"""
Upload ML checkpoints to Hugging Face Hub.

Usage:
    1. pip install huggingface_hub
    2. huggingface-cli login
    3. python3 upload_to_huggingface.py
"""

import os
from pathlib import Path
from huggingface_hub import HfApi, create_repo

# Configuration
REPO_NAME = "glowupai-skin-analysis"  # Change this to your desired repo name
ORG_NAME = None  # Set to your org name if uploading to org, otherwise None
CHECKPOINT_DIR = Path(__file__).parent / "checkpoints"

def upload_checkpoints():
    """Upload all checkpoints to Hugging Face Hub."""

    print("=" * 60)
    print("UPLOADING CHECKPOINTS TO HUGGING FACE HUB")
    print("=" * 60)

    # Initialize API
    api = HfApi()

    # Get username
    user_info = api.whoami()
    username = user_info["name"]

    repo_id = f"{ORG_NAME}/{REPO_NAME}" if ORG_NAME else f"{username}/{REPO_NAME}"

    print(f"\n📦 Repository: {repo_id}")

    # Create repository if it doesn't exist
    try:
        print(f"\n🔨 Creating repository...")
        create_repo(
            repo_id=repo_id,
            repo_type="model",
            exist_ok=True,
            private=False,  # Set to True for private repo
        )
        print(f"✅ Repository ready: https://huggingface.co/{repo_id}")
    except Exception as e:
        print(f"⚠️  Repository might already exist: {e}")

    # Get all checkpoint files
    checkpoints = list(CHECKPOINT_DIR.glob("*.pth"))

    if not checkpoints:
        print(f"\n❌ No checkpoint files found in {CHECKPOINT_DIR}")
        return

    print(f"\n📊 Found {len(checkpoints)} checkpoints:")
    for cp in checkpoints:
        size_mb = cp.stat().st_size / (1024 * 1024)
        print(f"   - {cp.name} ({size_mb:.1f} MB)")

    # Upload each checkpoint
    print(f"\n⬆️  Uploading to Hugging Face...")

    for checkpoint in checkpoints:
        print(f"\n   Uploading {checkpoint.name}...")

        try:
            api.upload_file(
                path_or_fileobj=str(checkpoint),
                path_in_repo=f"checkpoints/{checkpoint.name}",
                repo_id=repo_id,
                repo_type="model",
            )
            print(f"   ✅ {checkpoint.name} uploaded!")
        except Exception as e:
            print(f"   ❌ Failed to upload {checkpoint.name}: {e}")

    # Upload README
    readme_content = f"""---
license: apache-2.0
tags:
- skin-analysis
- computer-vision
- pytorch
- mobilenet
---

# GlowUp AI - Skin Analysis Model

This model analyzes facial skin and predicts 4 key metrics:
- **Redness**: Inflammation level (0-1)
- **Blemishes**: Count of acne/spots (0-100)
- **Texture**: Skin smoothness score (0-30)
- **Dark Spots**: Hyperpigmentation level (0-1)

## Model Details

- **Architecture**: MobileNetV2 (transfer learning)
- **Input**: 224x224 RGB images
- **Framework**: PyTorch
- **Training**: {len(checkpoints)} epochs on synthetic skin dataset

## Checkpoints

- `best_model.pth`: Best performing model (Val Loss: 0.7547)
- `checkpoint_epoch_XX.pth`: Intermediate checkpoints

## Usage

```python
import torch
from torchvision import models

# Load model
model = models.mobilenet_v2(pretrained=False)
model.classifier[1] = torch.nn.Linear(model.classifier[1].in_features, 4)
checkpoint = torch.load("best_model.pth")
model.load_state_dict(checkpoint["model_state_dict"])
model.eval()

# Inference
with torch.no_grad():
    predictions = model(image_tensor)
```

## Training Data

Trained on synthetic procedurally-generated skin textures with ground truth labels.

## License

Apache 2.0
"""

    print(f"\n📝 Creating README...")
    readme_path = CHECKPOINT_DIR.parent / "MODEL_CARD.md"
    readme_path.write_text(readme_content)

    try:
        api.upload_file(
            path_or_fileobj=str(readme_path),
            path_in_repo="README.md",
            repo_id=repo_id,
            repo_type="model",
        )
        print(f"✅ README uploaded!")
    except Exception as e:
        print(f"⚠️  Could not upload README: {e}")

    print(f"\n" + "=" * 60)
    print(f"✅ UPLOAD COMPLETE!")
    print(f"=" * 60)
    print(f"\n🔗 View your model: https://huggingface.co/{repo_id}")
    print(f"\n📥 Download command:")
    print(f"   huggingface-cli download {repo_id} --local-dir ./checkpoints")
    print()


if __name__ == "__main__":
    try:
        upload_checkpoints()
    except Exception as e:
        print(f"\n❌ Error: {e}")
        print("\n💡 Make sure you've run:")
        print("   1. pip install huggingface_hub")
        print("   2. huggingface-cli login")
