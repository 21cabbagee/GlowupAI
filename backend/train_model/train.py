"""
Training script for skin analysis model.
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
    """Main training loop."""
    print("=" * 60)
    print("TRAINING SKIN ANALYSIS MODEL")
    print("=" * 60)

    # Configuration
    DATA_DIR = "/Users/21cabbage/GlowupAI/backend/train_model/data"
    OUTPUT_DIR = Path("/Users/21cabbage/GlowupAI/backend/train_model/checkpoints")
    OUTPUT_DIR.mkdir(exist_ok=True)

    BATCH_SIZE = 16
    NUM_EPOCHS = 50
    LEARNING_RATE = 0.001

    # Device
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"\n🖥️  Device: {device}")

    # Create datasets
    print(f"\n📊 Loading datasets...")
    train_dataset = SkinDataset(DATA_DIR, split="train")
    val_dataset = SkinDataset(DATA_DIR, split="val")

    train_loader = DataLoader(train_dataset, batch_size=BATCH_SIZE, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=BATCH_SIZE, shuffle=False)

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

    best_val_loss = float("inf")

    print(f"\n🚀 Starting training...")
    print(f"   - Epochs: {NUM_EPOCHS}")
    print(f"   - Batch size: {BATCH_SIZE}")
    print(f"   - Learning rate: {LEARNING_RATE}")

    for epoch in range(NUM_EPOCHS):
        print(f"\n📈 Epoch {epoch + 1}/{NUM_EPOCHS}")

        # Train
        train_losses = train_epoch(model, train_loader, criterion, optimizer, device)

        # Validate
        val_losses = validate(model, val_loader, criterion, device)

        # Update scheduler
        scheduler.step(val_losses["total"])

        # Log
        print(f"   Train Loss: {train_losses['total']:.4f}")
        print(f"   Val Loss:   {val_losses['total']:.4f}")

        # Save history
        history["train_loss"].append(train_losses["total"])
        history["val_loss"].append(val_losses["total"])

        # Save best model
        if val_losses["total"] < best_val_loss:
            best_val_loss = val_losses["total"]
            torch.save(
                {
                    "epoch": epoch,
                    "model_state_dict": model.state_dict(),
                    "optimizer_state_dict": optimizer.state_dict(),
                    "val_loss": val_losses["total"],
                },
                OUTPUT_DIR / "best_model.pth"
            )
            print(f"   ✓ Saved best model (val_loss: {best_val_loss:.4f})")

    # Save final model
    torch.save(model.state_dict(), OUTPUT_DIR / "final_model.pth")

    # Plot training curves
    plt.figure(figsize=(10, 6))
    plt.plot(history["train_loss"], label="Train Loss")
    plt.plot(history["val_loss"], label="Val Loss")
    plt.xlabel("Epoch")
    plt.ylabel("Loss")
    plt.legend()
    plt.title("Training History")
    plt.savefig(OUTPUT_DIR / "training_history.png")

    print("\n" + "=" * 60)
    print("✅ TRAINING COMPLETE!")
    print("=" * 60)
    print(f"📁 Checkpoints saved to: {OUTPUT_DIR}")
    print(f"📊 Best val loss: {best_val_loss:.4f}")


if __name__ == "__main__":
    main()
