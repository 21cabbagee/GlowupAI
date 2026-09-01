#!/usr/bin/env python3
"""
Download ML checkpoints from Hugging Face Hub.

Usage:
    python3 download_from_huggingface.py [repo_id]

Example:
    python3 download_from_huggingface.py username/glowupai-skin-analysis
"""

import sys
from pathlib import Path
from huggingface_hub import hf_hub_download, snapshot_download

def download_checkpoints(repo_id=None):
    """Download all checkpoints from Hugging Face Hub."""

    if not repo_id:
        print("Usage: python3 download_from_huggingface.py <repo_id>")
        print("Example: python3 download_from_huggingface.py username/glowupai-skin-analysis")
        sys.exit(1)

    print("=" * 60)
    print("DOWNLOADING CHECKPOINTS FROM HUGGING FACE")
    print("=" * 60)

    checkpoint_dir = Path(__file__).parent / "checkpoints"
    checkpoint_dir.mkdir(exist_ok=True)

    print(f"\n📦 Repository: {repo_id}")
    print(f"📁 Download to: {checkpoint_dir}")

    try:
        # Download all files from the repo
        print(f"\n⬇️  Downloading all checkpoints...")

        snapshot_download(
            repo_id=repo_id,
            repo_type="model",
            local_dir=str(checkpoint_dir.parent),
            allow_patterns="checkpoints/*.pth",
        )

        print(f"\n✅ Download complete!")

        # List downloaded files
        checkpoints = list(checkpoint_dir.glob("*.pth"))
        print(f"\n📊 Downloaded {len(checkpoints)} checkpoints:")
        for cp in checkpoints:
            size_mb = cp.stat().st_size / (1024 * 1024)
            print(f"   ✅ {cp.name} ({size_mb:.1f} MB)")

    except Exception as e:
        print(f"\n❌ Download failed: {e}")
        print("\n💡 Make sure:")
        print("   1. Repository exists: https://huggingface.co/{repo_id}")
        print("   2. You have access (run: huggingface-cli login)")
        sys.exit(1)

    print(f"\n🚀 Checkpoints ready to use!")


if __name__ == "__main__":
    repo_id = sys.argv[1] if len(sys.argv) > 1 else None
    download_checkpoints(repo_id)
