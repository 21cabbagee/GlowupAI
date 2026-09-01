"""
Training script for skin analysis model with resume capability.
"""

import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from pathlib import Path
import json
import cv2
import numpy as np
from tqdm import tqdm
from typing import Dict, Tuple
import matplotlib.pyplot as plt
import argparse
import sys

from model import SkinAnalysisModel


class SkinDataset(Dataset):
    """Dataset for skin analysis training."""

    def __init__(self, data_dir: str, split: str = "train"):
        self.data_dir = Path(data_dir)
        self.split = split

        # Load split info
        with open(self.data_dir / "split.json") as f:
            split_info = json.load(f)

        self.filenames = split_info[split]
        print(f"✓ Loaded {len(self.filenames)} {split} samples")

    def __len__(self) -> int:
        return len(self.filenames)

    def __getitem__(self, idx: int) -> Tuple[torch.Tensor, Dict[str, float]]:
        filename = self.filenames[idx]

        # Load image
        image_path = self.data_dir / "images" / filename
        image = cv2.imread(str(image_path))
        image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

        # Resize to 224x224 (MobileNetV2 input size)
        image = cv2.resize(image, (224, 224))

        # Normalize to [0, 1]
        image = image.astype(np.float32) / 255.0

        # Convert to tensor (C, H, W)
        image = torch.from_numpy(image).permute(2, 0, 1)

        # Load labels
        label_path = self.data_dir / "labels" / f"{Path(filename).stem}.json"
        with open(label_path) as f:
            labels = json.load(f)

        # Convert labels to tensors
        labels_tensor = {
            "redness": torch.tensor(labels["redness"], dtype=torch.float32),
            "blemishes": torch.tensor(labels["blemishes"], dtype=torch.float32),
            "texture": torch.tensor(labels["texture"], dtype=torch.float32),
            "darkspots": torch.tensor(labels["darkspots"], dtype=torch.float32),
        }

        return image, labels_tensor


class MultiTaskLoss(nn.Module):
    """Multi-task loss for skin analysis."""

    def __init__(self):
        super().__init__()
        self.mse = nn.MSELoss()
        self.mae = nn.L1Loss()

    def forward(
        self,
        predictions: Dict[str, torch.Tensor],
        targets: Dict[str, torch.Tensor]
    ) -> Tuple[torch.Tensor, Dict[str, float]]:
        """
        Compute multi-task loss.

        Returns:
            Total loss and individual losses dict
        """
        # Redness: MSE (regression, [0, 1])
        loss_redness = self.mse(predictions["redness"], targets["redness"])

        # Blemishes: MAE (count regression)
        loss_blemishes = self.mae(predictions["blemishes"], targets["blemishes"])

        # Texture: MSE (regression)
        loss_texture = self.mse(predictions["texture"], targets["texture"])

        # Darkspots: MSE (regression, [0, 1])
        loss_darkspots = self.mse(predictions["darkspots"], targets["darkspots"])

        # Total loss (weighted sum)
        total_loss = (
            loss_redness * 1.0 +
            loss_blemishes * 0.1 +  # Scale down count loss
            loss_texture * 0.5 +
            loss_darkspots * 1.0
        )

        losses = {
            "total": total_loss.item(),
            "redness": loss_redness.item(),
            "blemishes": loss_blemishes.item(),
            "texture": loss_texture.item(),
            "darkspots": loss_darkspots.item(),
        }

        return total_loss, losses


def train_epoch(
    model: nn.Module,
    dataloader: DataLoader,
    criterion: nn.Module,
    optimizer: optim.Optimizer,
    device: torch.device
) -> Dict[str, float]:
    """Train for one epoch."""
    model.train()
    epoch_losses = {k: 0.0 for k in ["total", "redness", "blemishes", "texture", "darkspots"]}

    pbar = tqdm(dataloader, desc="Training")
    for images, labels in pbar:
        images = images.to(device)
        labels = {k: v.to(device) for k, v in labels.items()}

        # Forward pass
        predictions = model(images)
        loss, losses = criterion(predictions, labels)

        # Backward pass
        optimizer.zero_grad()
        loss.backward()
        optimizer.step()

        # Accumulate losses
        for k, v in losses.items():
            epoch_losses[k] += v

        # Update progress bar
        pbar.set_postfix({"loss": f"{losses['total']:.4f}"})

    # Average losses
    num_batches = len(dataloader)
    return {k: v / num_batches for k, v in epoch_losses.items()}


def validate(
    model: nn.Module,
    dataloader: DataLoader,
    criterion: nn.Module,
    device: torch.device
) -> Dict[str, float]:
    """Validate model."""
    model.eval()
    epoch_losses = {k: 0.0 for k in ["total", "redness", "blemishes", "texture", "darkspots"]}

    with torch.no_grad():
        for images, labels in tqdm(dataloader, desc="Validation"):
            images = images.to(device)
            labels = {k: v.to(device) for k, v in labels.items()}

            # Forward pass
            predictions = model(images)
            _, losses = criterion(predictions, labels)

            # Accumulate losses
            for k, v in losses.items():
                epoch_losses[k] += v

    # Average losses
    num_batches = len(dataloader)
    return {k: v / num_batches for k, v in epoch_losses.items()}


def main():
    """Main training loop with resume capability."""
    parser = argparse.ArgumentParser(description="Train skin analysis model")
    parser.add_argument("--resume", type=str, default=None, help="Path to checkpoint to resume from")
    parser.add_argument("--start-epoch", type=int, default=0, help="Starting epoch number")
    args = parser.parse_args()

    print("=" * 60)
    print("TRAINING SKIN ANALYSIS MODEL (RESUME MODE)")
    print("=" * 60)

    # Configuration
    DATA_DIR = "/Users/21cabbage/GlowupAI/backend/train_model/data"
    OUTPUT_DIR = Path("/Users/21cabbage/GlowupAI/backend/train_model/checkpoints")
    OUTPUT_DIR.mkdir(exist_ok=True)

    BATCH_SIZE = 16
    NUM_EPOCHS = 50
    LEARNING_RATE = 0.001
    PATIENCE = 10  # Early stopping patience

    # Device (prioritize GPU: CUDA > MPS > CPU)
    if torch.cuda.is_available():
        device = torch.device("cuda")
        print(f"\n🖥️  Device: CUDA GPU")
        print(f"   GPU: {torch.cuda.get_device_name(0)}")
    elif hasattr(torch.backends, "mps") and torch.backends.mps.is_available():
        device = torch.device("mps")
        print(f"\n🖥️  Device: Apple Silicon GPU (MPS)")
    else:
        device = torch.device("cpu")
        print(f"\n🖥️  Device: CPU")

    # Create datasets
    print(f"\n📊 Loading datasets...")
    train_dataset = SkinDataset(DATA_DIR, split="train")
    val_dataset = SkinDataset(DATA_DIR, split="val")

    train_loader = DataLoader(train_dataset, batch_size=BATCH_SIZE, shuffle=True, num_workers=0)
    val_loader = DataLoader(val_dataset, batch_size=BATCH_SIZE, shuffle=False, num_workers=0)

    # Create model
    print(f"\n🏗️  Building model...")
    model = SkinAnalysisModel(pretrained=True).to(device)

    # Loss and optimizer
    criterion = MultiTaskLoss()
    optimizer = optim.Adam(model.parameters(), lr=LEARNING_RATE)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode="min", patience=5, factor=0.5
    )

    # Training history
    history = {
        "train_loss": [],
        "val_loss": [],
    }

    start_epoch = 0
    best_val_loss = float("inf")
    epochs_without_improvement = 0

    # Resume from checkpoint if provided
    if args.resume:
        print(f"\n📂 Loading checkpoint: {args.resume}")
        checkpoint = torch.load(args.resume, map_location=device)

        model.load_state_dict(checkpoint["model_state_dict"])
        optimizer.load_state_dict(checkpoint["optimizer_state_dict"])

        start_epoch = checkpoint["epoch"] + 1
        best_val_loss = checkpoint["val_loss"]

        print(f"   ✓ Resumed from epoch {checkpoint['epoch']}")
        print(f"   ✓ Best val loss: {best_val_loss:.4f}")
        print(f"   ✓ Starting from epoch {start_epoch}")

    print(f"\n🚀 Starting training...")
    print(f"   - Epochs: {start_epoch} to {NUM_EPOCHS}")
    print(f"   - Batch size: {BATCH_SIZE}")
    print(f"   - Learning rate: {LEARNING_RATE}")
    print(f"   - Early stopping patience: {PATIENCE}")
    print(f"   - Current best val loss: {best_val_loss:.4f}")

    try:
        for epoch in range(start_epoch, NUM_EPOCHS):
            print(f"\n{'='*60}")
            print(f"📈 Epoch {epoch + 1}/{NUM_EPOCHS}")
            print(f"{'='*60}")

            # Train
            train_losses = train_epoch(model, train_loader, criterion, optimizer, device)

            # Validate
            val_losses = validate(model, val_loader, criterion, device)

            # Update scheduler
            current_lr = optimizer.param_groups[0]['lr']
            scheduler.step(val_losses["total"])
            new_lr = optimizer.param_groups[0]['lr']

            if new_lr != current_lr:
                print(f"   📉 Learning rate reduced: {current_lr:.6f} -> {new_lr:.6f}")

            # Log
            print(f"\n   📊 Results:")
            print(f"      Train Loss: {train_losses['total']:.4f}")
            print(f"      Val Loss:   {val_losses['total']:.4f}")
            print(f"      Best Val Loss: {best_val_loss:.4f}")

            # Save history
            history["train_loss"].append(train_losses["total"])
            history["val_loss"].append(val_losses["total"])

            # Save best model
            if val_losses["total"] < best_val_loss:
                improvement = best_val_loss - val_losses["total"]
                best_val_loss = val_losses["total"]
                epochs_without_improvement = 0

                torch.save(
                    {
                        "epoch": epoch,
                        "model_state_dict": model.state_dict(),
                        "optimizer_state_dict": optimizer.state_dict(),
                        "val_loss": val_losses["total"],
                        "train_loss": train_losses["total"],
                    },
                    OUTPUT_DIR / "best_model.pth"
                )
                print(f"      ✓ NEW BEST MODEL! Improved by {improvement:.4f}")
                print(f"      ✓ Saved to: {OUTPUT_DIR / 'best_model.pth'}")
            else:
                epochs_without_improvement += 1
                print(f"      No improvement for {epochs_without_improvement} epochs")

            # Save periodic checkpoint every 10 epochs
            if (epoch + 1) % 10 == 0:
                checkpoint_path = OUTPUT_DIR / f"checkpoint_epoch_{epoch + 1}.pth"
                torch.save(
                    {
                        "epoch": epoch,
                        "model_state_dict": model.state_dict(),
                        "optimizer_state_dict": optimizer.state_dict(),
                        "val_loss": val_losses["total"],
                        "train_loss": train_losses["total"],
                        "history": history,
                    },
                    checkpoint_path
                )
                print(f"      ✓ Checkpoint saved: {checkpoint_path}")

            # Early stopping
            if epochs_without_improvement >= PATIENCE:
                print(f"\n⚠️  Early stopping triggered! No improvement for {PATIENCE} epochs.")
                print(f"   Best val loss: {best_val_loss:.4f} at epoch {epoch - PATIENCE + 1}")
                break

            # Report every 10 epochs
            if (epoch + 1) % 10 == 0 or epoch == 0:
                print(f"\n   {'='*50}")
                print(f"   📈 PROGRESS REPORT - Epoch {epoch + 1}/{NUM_EPOCHS}")
                print(f"   {'='*50}")
                print(f"   Completion: {((epoch + 1) / NUM_EPOCHS * 100):.1f}%")
                print(f"   Current Val Loss: {val_losses['total']:.4f}")
                print(f"   Best Val Loss: {best_val_loss:.4f}")
                print(f"   Epochs without improvement: {epochs_without_improvement}/{PATIENCE}")
                print(f"   {'='*50}")

    except KeyboardInterrupt:
        print("\n\n⚠️  Training interrupted by user!")
        print(f"   Progress saved. You can resume from the last checkpoint.")

    # Save final model
    final_path = OUTPUT_DIR / "final_model.pth"
    torch.save(model.state_dict(), final_path)
    print(f"\n✓ Final model saved to: {final_path}")

    # Plot training curves
    if len(history["train_loss"]) > 0:
        plt.figure(figsize=(12, 6))
        epochs_range = range(start_epoch, start_epoch + len(history["train_loss"]))
        plt.plot(epochs_range, history["train_loss"], label="Train Loss", marker='o')
        plt.plot(epochs_range, history["val_loss"], label="Val Loss", marker='s')
        plt.axhline(y=best_val_loss, color='r', linestyle='--', label=f'Best Val Loss: {best_val_loss:.4f}')
        plt.xlabel("Epoch")
        plt.ylabel("Loss")
        plt.legend()
        plt.title("Training History (Resumed)")
        plt.grid(True, alpha=0.3)
        plt.savefig(OUTPUT_DIR / "training_history_resumed.png", dpi=150)
        print(f"✓ Training curve saved to: {OUTPUT_DIR / 'training_history_resumed.png'}")

    print("\n" + "=" * 60)
    print("✅ TRAINING COMPLETE!")
    print("=" * 60)
    print(f"📁 Checkpoints saved to: {OUTPUT_DIR}")
    print(f"📊 Best val loss: {best_val_loss:.4f}")
    print(f"📊 Final val loss: {val_losses['total']:.4f}")
    print("=" * 60)


if __name__ == "__main__":
    main()
