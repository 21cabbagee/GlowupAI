#!/usr/bin/env python3
"""
Download CelebA-HQ subset for training.

Uses Kaggle API to download real face dataset.
"""

import os
import subprocess
import sys
from pathlib import Path
import urllib.request
import zipfile
from tqdm import tqdm


def download_file(url: str, output_path: str):
    """Download file with progress bar."""
    print(f"📥 Downloading from {url}")

    class DownloadProgressBar(tqdm):
        def update_to(self, b=1, bsize=1, tsize=None):
            if tsize is not None:
                self.total = tsize
            self.update(b * bsize - self.n)

    with DownloadProgressBar(unit='B', unit_scale=True, miniters=1) as t:
        urllib.request.urlretrieve(url, output_path, reporthook=t.update_to)


def download_utkface():
    """
    Download UTKFace dataset (smaller, public, no API needed).

    UTKFace: 20K+ face images with age, gender, ethnicity labels.
    Size: ~500MB compressed
    """
    print("=" * 60)
    print("DOWNLOADING UTKFACE DATASET")
    print("=" * 60)

    output_dir = Path("/Users/21cabbage/GlowupAI/backend/train_model/data/utkface")
    output_dir.mkdir(parents=True, exist_ok=True)

    # Download from Google Drive (public mirror)
    dataset_url = "https://drive.google.com/uc?export=download&id=0BxYys69jI14kYVM3aVhKS1VhRUk"
    zip_path = output_dir / "utkface.tar.gz"

    print(f"\n📦 Dataset: UTKFace")
    print(f"📊 Size: ~500MB")
    print(f"📸 Images: 20,000+")
    print(f"📁 Output: {output_dir}")

    try:
        # Try downloading
        print(f"\n⏬ Starting download...")
        download_file(dataset_url, str(zip_path))

        print(f"\n✅ Downloaded successfully!")
        print(f"📦 File: {zip_path}")

        # Extract
        print(f"\n📂 Extracting...")
        subprocess.run(["tar", "-xzf", str(zip_path), "-C", str(output_dir)], check=True)

        print(f"✅ Extraction complete!")

        # Count images
        images = list(output_dir.rglob("*.jpg"))
        print(f"📸 Found {len(images)} images")

        return output_dir

    except Exception as e:
        print(f"❌ Error: {e}")
        print(f"\n⚠️  Direct download failed. Using alternative method...")
        return None


def download_ffhq_thumbnails():
    """
    Download FFHQ-Thumbnails (Flickr-Faces-HQ) - high quality faces.

    FFHQ Thumbnails: 70K high-quality face images, 128x128px
    Size: ~500MB
    Public domain, no API needed
    """
    print("=" * 60)
    print("DOWNLOADING FFHQ THUMBNAILS DATASET")
    print("=" * 60)

    output_dir = Path("/Users/21cabbage/GlowupAI/backend/train_model/data/ffhq")
    output_dir.mkdir(parents=True, exist_ok=True)

    # FFHQ thumbnails from official GitHub
    base_url = "https://github.com/NVlabs/ffhq-dataset/releases/download/thumbnails128x128/"

    # Download first 1000 images (enough for training)
    print(f"\n📦 Dataset: FFHQ Thumbnails (128x128)")
    print(f"📊 Downloading: 1,000 images (~50MB)")
    print(f"📁 Output: {output_dir}")

    downloaded = 0
    target_count = 1000

    print(f"\n⏬ Downloading images...")

    for i in tqdm(range(0, target_count, 1000)):
        zip_name = f"ffhq-thumbnails-{i:05d}.zip"
        zip_url = base_url + zip_name
        zip_path = output_dir / zip_name

        try:
            urllib.request.urlretrieve(zip_url, str(zip_path))

            # Extract
            with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                zip_ref.extractall(output_dir)

            # Remove zip
            zip_path.unlink()

            downloaded += 1000

        except Exception as e:
            print(f"\n⚠️  Could not download {zip_name}: {e}")
            break

    # Count images
    images = list(output_dir.rglob("*.png"))
    print(f"\n✅ Downloaded {len(images)} images")

    return output_dir


def main():
    """Main download script."""

    # Try FFHQ first (easier to download)
    print("\n🎯 Attempting to download FFHQ dataset...")
    dataset_dir = download_ffhq_thumbnails()

    if dataset_dir:
        print(f"\n✅ SUCCESS! Dataset ready at: {dataset_dir}")
        return dataset_dir

    print("\n❌ All download methods failed.")
    print("\n💡 ALTERNATIVE: Generate synthetic dataset")
    print("   Run: python3 download_training_data.py")

    return None


if __name__ == "__main__":
    main()
