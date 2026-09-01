"""
Prepare training data for skin analysis model.

Uses image augmentation to create a robust training dataset from limited samples.
"""

import os
import cv2
import numpy as np
from pathlib import Path
import json
from typing import List, Tuple, Dict
import albumentations as A
from tqdm import tqdm


class SkinDatasetPreparator:
    """Prepares and augments skin images for training."""

    def __init__(self, output_dir: str = "training_data"):
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(exist_ok=True)

        # Create subdirectories
        (self.output_dir / "images").mkdir(exist_ok=True)
        (self.output_dir / "labels").mkdir(exist_ok=True)

        # Augmentation pipeline
        self.augment = A.Compose([
            A.HorizontalFlip(p=0.5),
            A.Rotate(limit=15, p=0.5),
            A.RandomBrightnessContrast(
                brightness_limit=0.2,
                contrast_limit=0.2,
                p=0.5
            ),
            A.GaussNoise(var_limit=(10.0, 50.0), p=0.3),
            A.Blur(blur_limit=3, p=0.3),
        ])

    def extract_from_database(self, db_path: str) -> List[Dict]:
        """
        Extract existing captures from SQLite database.

        Args:
            db_path: Path to skinproof.db

        Returns:
            List of capture dictionaries with image paths and metrics
        """
        import sqlite3

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()

        # Get all captures with metrics
        cursor.execute("""
            SELECT
                c.image_path,
                m.redness_score,
                m.blemish_count,
                m.texture_score,
                m.darkspot_area
            FROM captures c
            LEFT JOIN metrics m ON c.id = m.capture_id
            WHERE m.redness_score IS NOT NULL
        """)

        data = []
        for row in cursor.fetchall():
            data.append({
                "image_path": row[0],
                "redness": row[1],
                "blemishes": row[2],
                "texture": row[3],
                "darkspots": row[4],
            })

        conn.close()
        print(f"✓ Extracted {len(data)} captures from database")
        return data

    def augment_dataset(
        self,
        samples: List[Dict],
        augmentations_per_image: int = 10
    ) -> List[Dict]:
        """
        Augment dataset using image transformations.

        Args:
            samples: List of original samples
            augmentations_per_image: Number of augmented versions per image

        Returns:
            Augmented dataset (original + augmented)
        """
        augmented_data = []

        print(f"\n🔄 Augmenting {len(samples)} images...")

        for idx, sample in enumerate(tqdm(samples)):
            # Check if image exists
            image_path = sample["image_path"]
            if not os.path.exists(image_path):
                print(f"⚠️  Skipping missing image: {image_path}")
                continue

            # Load image
            image = cv2.imread(image_path)
            if image is None:
                print(f"⚠️  Could not load: {image_path}")
                continue

            # Save original
            original_filename = f"img_{idx:05d}_orig.jpg"
            cv2.imwrite(
                str(self.output_dir / "images" / original_filename),
                image
            )

            # Save original labels
            self._save_labels(original_filename, sample)
            augmented_data.append({
                "filename": original_filename,
                **{k: v for k, v in sample.items() if k != "image_path"}
            })

            # Generate augmented versions
            for aug_idx in range(augmentations_per_image):
                # Apply augmentation
                augmented = self.augment(image=image)["image"]

                # Save augmented image
                aug_filename = f"img_{idx:05d}_aug{aug_idx:02d}.jpg"
                cv2.imwrite(
                    str(self.output_dir / "images" / aug_filename),
                    augmented
                )

                # Save labels (same as original)
                self._save_labels(aug_filename, sample)
                augmented_data.append({
                    "filename": aug_filename,
                    **{k: v for k, v in sample.items() if k != "image_path"}
                })

        print(f"✓ Created {len(augmented_data)} training samples")
        return augmented_data

    def _save_labels(self, filename: str, sample: Dict):
        """Save labels as JSON."""
        label_path = self.output_dir / "labels" / f"{Path(filename).stem}.json"
        with open(label_path, "w") as f:
            json.dump({
                "redness": float(sample.get("redness", 0)),
                "blemishes": int(sample.get("blemishes", 0)),
                "texture": float(sample.get("texture", 0)),
                "darkspots": float(sample.get("darkspots", 0)),
            }, f)

    def create_splits(
        self,
        data: List[Dict],
        train_ratio: float = 0.8
    ) -> Tuple[List[Dict], List[Dict]]:
        """Split data into train and validation sets."""
        np.random.shuffle(data)
        split_idx = int(len(data) * train_ratio)

        train_data = data[:split_idx]
        val_data = data[split_idx:]

        print(f"✓ Train: {len(train_data)} samples")
        print(f"✓ Val: {len(val_data)} samples")

        return train_data, val_data

    def save_split_info(
        self,
        train_data: List[Dict],
        val_data: List[Dict]
    ):
        """Save train/val split information."""
        split_info = {
            "train": [d["filename"] for d in train_data],
            "val": [d["filename"] for d in val_data],
        }

        with open(self.output_dir / "split.json", "w") as f:
            json.dump(split_info, f, indent=2)

        print(f"✓ Saved split info to {self.output_dir / 'split.json'}")


def main():
    """Main data preparation pipeline."""
    print("=" * 60)
    print("SKIN ANALYSIS MODEL - DATA PREPARATION")
    print("=" * 60)

    # Configuration
    DB_PATH = "/Users/21cabbage/GlowupAI/backend/skinproof.db"
    OUTPUT_DIR = "/Users/21cabbage/GlowupAI/backend/train_model/data"
    AUGMENTATIONS_PER_IMAGE = 10

    # Initialize preparator
    preparator = SkinDatasetPreparator(output_dir=OUTPUT_DIR)

    # Step 1: Extract from database
    print("\n📊 Step 1: Extracting data from database...")
    samples = preparator.extract_from_database(DB_PATH)

    if len(samples) == 0:
        print("❌ No training data found in database!")
        print("   Take some captures in the app first.")
        return

    # Step 2: Augment dataset
    print(f"\n🔄 Step 2: Augmenting dataset ({AUGMENTATIONS_PER_IMAGE}x)...")
    augmented_data = preparator.augment_dataset(samples, AUGMENTATIONS_PER_IMAGE)

    # Step 3: Create splits
    print("\n✂️  Step 3: Creating train/val splits...")
    train_data, val_data = preparator.create_splits(augmented_data)

    # Step 4: Save split info
    preparator.save_split_info(train_data, val_data)

    print("\n" + "=" * 60)
    print("✅ DATA PREPARATION COMPLETE!")
    print("=" * 60)
    print(f"📁 Output directory: {OUTPUT_DIR}")
    print(f"📊 Total samples: {len(augmented_data)}")
    print(f"   - Train: {len(train_data)}")
    print(f"   - Val: {len(val_data)}")
    print("\n🚀 Ready for training!")


if __name__ == "__main__":
    main()
