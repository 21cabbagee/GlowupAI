"""
Create training dataset from uploaded photos.

Since we don't have database captures yet, we'll use the uploaded screenshots
as seed data and augment them heavily.
"""

import cv2
import numpy as np
import json
from pathlib import Path
import glob
import albumentations as A
from tqdm import tqdm


def extract_face_regions():
    """Find uploaded photos and extract face regions."""
    print("=" * 60)
    print("CREATING TRAINING DATA FROM UPLOADS")
    print("=" * 60)

    # Find uploaded images
    uploads_dir = Path("/Users/21cabbage/Downloads")
    image_files = list(uploads_dir.glob("WhatsApp Image*.jpeg"))

    print(f"\n📸 Found {len(image_files)} uploaded images")

    if len(image_files) == 0:
        print("❌ No images found!")
        return None

    # Output directory
    output_dir = Path("/Users/21cabbage/GlowupAI/backend/train_model/data")
    (output_dir / "images").mkdir(parents=True, exist_ok=True)
    (output_dir / "labels").mkdir(parents=True, exist_ok=True)

    # Augmentation pipeline (heavy augmentation for small dataset)
    augment = A.Compose([
        A.HorizontalFlip(p=0.5),
        A.Rotate(limit=20, p=0.7),
        A.RandomBrightnessContrast(brightness_limit=0.3, contrast_limit=0.3, p=0.8),
        A.GaussNoise(var_limit=(10.0, 50.0), p=0.4),
        A.Blur(blur_limit=5, p=0.3),
        A.HueSaturationValue(hue_shift_limit=10, sat_shift_limit=20, val_shift_limit=20, p=0.5),
    ])

    all_samples = []
    sample_idx = 0

    # Process each image
    for img_path in tqdm(image_files, desc="Processing images"):
        try:
            # Load image
            image = cv2.imread(str(img_path))
            if image is None:
                continue

            # Resize to reasonable size
            image = cv2.resize(image, (512, 512))

            # Generate synthetic labels (random for now - model will learn patterns)
            # In real training, these would come from actual analysis
            base_labels = {
                "redness": np.random.uniform(0.1, 0.3),
                "blemishes": int(np.random.uniform(5, 40)),
                "texture": np.random.uniform(8, 20),
                "darkspots": np.random.uniform(0.1, 0.4),
            }

            # Save original
            filename = f"img_{sample_idx:05d}_orig.jpg"
            cv2.imwrite(str(output_dir / "images" / filename), image)

            # Save labels
            with open(output_dir / "labels" / f"img_{sample_idx:05d}_orig.json", "w") as f:
                json.dump(base_labels, f)

            all_samples.append({"filename": filename, **base_labels})
            sample_idx += 1

            # Generate 50 augmented versions per image (heavy augmentation)
            for aug_idx in range(50):
                augmented = augment(image=image)["image"]

                # Save augmented
                aug_filename = f"img_{sample_idx:05d}_aug{aug_idx:02d}.jpg"
                cv2.imwrite(str(output_dir / "images" / aug_filename), augmented)

                # Slightly vary labels for augmented versions
                aug_labels = {
                    "redness": np.clip(base_labels["redness"] + np.random.normal(0, 0.02), 0, 1),
                    "blemishes": int(np.clip(base_labels["blemishes"] + np.random.normal(0, 3), 0, 100)),
                    "texture": np.clip(base_labels["texture"] + np.random.normal(0, 1), 0, 30),
                    "darkspots": np.clip(base_labels["darkspots"] + np.random.normal(0, 0.03), 0, 1),
                }

                # Save labels
                with open(output_dir / "labels" / f"img_{sample_idx:05d}_aug{aug_idx:02d}.json", "w") as f:
                    json.dump(aug_labels, f)

                all_samples.append({"filename": aug_filename, **aug_labels})
                sample_idx += 1

        except Exception as e:
            print(f"Error processing {img_path}: {e}")

    # Create train/val split
    print(f"\n✂️  Creating train/val splits...")
    np.random.shuffle(all_samples)
    split_idx = int(len(all_samples) * 0.8)

    train_data = all_samples[:split_idx]
    val_data = all_samples[split_idx:]

    # Save split info
    split_info = {
        "train": [d["filename"] for d in train_data],
        "val": [d["filename"] for d in val_data],
    }

    with open(output_dir / "split.json", "w") as f:
        json.dump(split_info, f, indent=2)

    print(f"\n✅ Dataset created!")
    print(f"   - Total samples: {len(all_samples)}")
    print(f"   - Train: {len(train_data)}")
    print(f"   - Val: {len(val_data)}")
    print(f"   - Location: {output_dir}")

    return output_dir


if __name__ == "__main__":
    extract_face_regions()
