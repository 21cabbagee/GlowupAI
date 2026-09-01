#!/usr/bin/env python3
"""
Generate synthetic training dataset for skin analysis.

Creates realistic procedural face textures with known ground truth labels.
This is a standard approach for bootstrapping ML models.
"""

import os
import sys
import json
import numpy as np
from pathlib import Path

# Check dependencies
try:
    import cv2
except ImportError:
    print("Installing opencv-python...")
    os.system(f"{sys.executable} -m pip install --user opencv-python --break-system-packages")
    import cv2

print("=" * 60)
print("GENERATING SYNTHETIC TRAINING DATASET")
print("=" * 60)

# Configuration
OUTPUT_DIR = Path("data")
(OUTPUT_DIR / "images").mkdir(parents=True, exist_ok=True)
(OUTPUT_DIR / "labels").mkdir(parents=True, exist_ok=True)

NUM_BASE_SAMPLES = 200  # Base synthetic images
AUGMENTATIONS_PER = 10  # Augmented versions each
TOTAL_SAMPLES = NUM_BASE_SAMPLES * (AUGMENTATIONS_PER + 1)

print(f"\n📊 Configuration:")
print(f"   - Base samples: {NUM_BASE_SAMPLES}")
print(f"   - Augmentations per sample: {AUGMENTATIONS_PER}")
print(f"   - Total samples: {TOTAL_SAMPLES}")


def generate_skin_texture(seed: int, size=224) -> tuple:
    """Generate synthetic skin texture with realistic characteristics."""
    np.random.seed(seed)

    # Base skin tone (realistic range)
    tone = np.random.randint(150, 230, 3)
    image = np.ones((size, size, 3), dtype=np.uint8) * tone

    # Add natural texture variation
    noise = np.random.randint(-15, 15, (size, size, 3))
    image = np.clip(image + noise, 0, 255).astype(np.uint8)

    # Redness (inflamed areas)
    redness_level = np.random.uniform(0.05, 0.6)
    num_red_areas = int(redness_level * 30)
    for _ in range(num_red_areas):
        x, y = np.random.randint(10, size-10, 2)
        radius = np.random.randint(5, 20)
        color = (120, 100, int(200 * redness_level))
        cv2.circle(image, (x, y), radius, color, -1, cv2.LINE_AA)

    # Blemishes (acne, spots)
    blemish_count = int(np.random.uniform(3, 50))
    for _ in range(blemish_count):
        x, y = np.random.randint(0, size, 2)
        radius = np.random.randint(2, 5)
        color = tuple(np.random.randint(80, 150, 3).tolist())
        cv2.circle(image, (x, y), radius, color, -1)

    # Dark spots (hyperpigmentation)
    darkspot_level = np.random.uniform(0.05, 0.5)
    num_darkspots = int(darkspot_level * 25)
    for _ in range(num_darkspots):
        x, y = np.random.randint(10, size-10, 2)
        radius = np.random.randint(8, 20)
        color = tuple((tone * 0.6).astype(int).tolist())
        cv2.circle(image, (x, y), radius, color, -1, cv2.LINE_AA)

    # Blur for realism
    image = cv2.GaussianBlur(image, (7, 7), 0)

    # Generate ground truth labels
    red_intensity = np.mean(image[:, :, 2]) / 255.0
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    texture_score = np.std(gray) * 0.8

    labels = {
        "redness": float(np.clip(red_intensity * 0.8, 0, 1)),
        "blemishes": int(blemish_count),
        "texture": float(np.clip(texture_score, 0, 30)),
        "darkspots": float(np.clip(darkspot_level * 0.9, 0, 1)),
    }

    return image, labels


def augment_image(image: np.ndarray, seed: int) -> np.ndarray:
    """Apply random augmentations."""
    np.random.seed(seed)
    aug = image.copy()

    # Random flip
    if np.random.rand() > 0.5:
        aug = cv2.flip(aug, 1)

    # Random rotation
    angle = np.random.uniform(-15, 15)
    M = cv2.getRotationMatrix2D((112, 112), angle, 1.0)
    aug = cv2.warpAffine(aug, M, (224, 224))

    # Random brightness/contrast
    alpha = np.random.uniform(0.8, 1.2)  # Contrast
    beta = np.random.randint(-20, 20)    # Brightness
    aug = cv2.convertScaleAbs(aug, alpha=alpha, beta=beta)

    # Random blur
    if np.random.rand() > 0.7:
        aug = cv2.GaussianBlur(aug, (3, 3), 0)

    return aug


# Generate dataset
print(f"\n🎨 Generating synthetic faces...")
all_samples = []

for i in range(NUM_BASE_SAMPLES):
    if i % 20 == 0:
        print(f"   Progress: {i}/{NUM_BASE_SAMPLES}")

    # Generate base image
    image, labels = generate_skin_texture(i)

    # Save original
    filename = f"syn_{i:05d}_orig.jpg"
    cv2.imwrite(str(OUTPUT_DIR / "images" / filename), image)

    with open(OUTPUT_DIR / "labels" / f"syn_{i:05d}_orig.json", "w") as f:
        json.dump(labels, f)

    all_samples.append({"filename": filename, **labels})

    # Generate augmented versions
    for aug_idx in range(AUGMENTATIONS_PER):
        aug_image = augment_image(image, i * 100 + aug_idx)

        aug_filename = f"syn_{i:05d}_aug{aug_idx:02d}.jpg"
        cv2.imwrite(str(OUTPUT_DIR / "images" / aug_filename), aug_image)

        # Slightly vary labels
        aug_labels = {
            "redness": np.clip(labels["redness"] + np.random.normal(0, 0.02), 0, 1),
            "blemishes": int(np.clip(labels["blemishes"] + np.random.normal(0, 2), 0, 100)),
            "texture": np.clip(labels["texture"] + np.random.normal(0, 0.5), 0, 30),
            "darkspots": np.clip(labels["darkspots"] + np.random.normal(0, 0.02), 0, 1),
        }

        with open(OUTPUT_DIR / "labels" / f"syn_{i:05d}_aug{aug_idx:02d}.json", "w") as f:
            json.dump({k: float(v) for k, v in aug_labels.items()}, f)

        all_samples.append({"filename": aug_filename, **aug_labels})

# Create train/val split
print(f"\n✂️  Creating train/val split...")
np.random.shuffle(all_samples)
split_idx = int(len(all_samples) * 0.8)

train_data = all_samples[:split_idx]
val_data = all_samples[split_idx:]

split_info = {
    "train": [d["filename"] for d in train_data],
    "val": [d["filename"] for d in val_data],
}

with open(OUTPUT_DIR / "split.json", "w") as f:
    json.dump(split_info, f, indent=2)

print(f"\n" + "=" * 60)
print(f"✅ DATASET GENERATED!")
print(f"=" * 60)
print(f"📊 Total samples: {len(all_samples)}")
print(f"   - Train: {len(train_data)}")
print(f"   - Val: {len(val_data)}")
print(f"📁 Location: {OUTPUT_DIR.absolute()}")
print(f"\n🚀 Ready for training!")
