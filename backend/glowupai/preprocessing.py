"""Image preprocessing pipeline for consistent lighting and quality normalization.

This module provides functions to normalize images before analysis to reduce
inconsistencies caused by lighting variations, compression artifacts, and color
temperature differences.
"""

from __future__ import annotations

import io
import logging

import cv2
import numpy as np
from PIL import Image

logger = logging.getLogger(__name__)


def apply_white_balance(img: np.ndarray) -> np.ndarray:
    """Apply white balance correction using the Gray World algorithm.

    This helps normalize color temperature variations across different lighting
    conditions.

    Args:
        img: Input image in BGR format

    Returns:
        White-balanced image in BGR format
    """
    result = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
    avg_a = np.average(result[:, :, 1])
    avg_b = np.average(result[:, :, 2])
    result[:, :, 1] = result[:, :, 1] - ((avg_a - 128) * (result[:, :, 0] / 255.0) * 1.1)
    result[:, :, 2] = result[:, :, 2] - ((avg_b - 128) * (result[:, :, 0] / 255.0) * 1.1)
    result = cv2.cvtColor(result, cv2.COLOR_LAB2BGR)
    return result


def apply_clahe(img: np.ndarray, clip_limit: float = 2.0, tile_size: tuple[int, int] = (8, 8)) -> np.ndarray:
    """Apply CLAHE (Contrast Limited Adaptive Histogram Equalization).

    CLAHE improves local contrast while avoiding over-amplification of noise.
    This is particularly effective for skin texture analysis.

    Args:
        img: Input image in BGR format
        clip_limit: Threshold for contrast limiting (default: 2.0)
        tile_size: Size of grid for histogram equalization (default: 8x8)

    Returns:
        Contrast-enhanced image in BGR format
    """
    lab = cv2.cvtColor(img, cv2.COLOR_BGR2LAB)
    l, a, b = cv2.split(lab)

    clahe = cv2.createCLAHE(clipLimit=clip_limit, tileGridSize=tile_size)
    l = clahe.apply(l)

    enhanced = cv2.merge([l, a, b])
    result = cv2.cvtColor(enhanced, cv2.COLOR_LAB2BGR)
    return result


def denoise_image(img: np.ndarray, h: int = 10, template_window: int = 7, search_window: int = 21) -> np.ndarray:
    """Apply non-local means denoising to reduce noise while preserving edges.

    Args:
        img: Input image in BGR format
        h: Filter strength (default: 10). Higher values remove more noise but may blur details.
        template_window: Size of template patch (default: 7)
        search_window: Size of search area (default: 21)

    Returns:
        Denoised image in BGR format
    """
    return cv2.fastNlMeansDenoisingColored(img, None, h, h, template_window, search_window)


def resize_standard(img: np.ndarray, target_size: tuple[int, int] = (512, 512)) -> np.ndarray:
    """Resize image to standard dimensions using high-quality interpolation.

    Args:
        img: Input image in BGR format
        target_size: Target (width, height) in pixels (default: 512x512)

    Returns:
        Resized image in BGR format
    """
    return cv2.resize(img, target_size, interpolation=cv2.INTER_LANCZOS4)


def preprocess_for_analysis(image_bytes: bytes, preserve_original_size: bool = False) -> bytes:
    """Complete preprocessing pipeline for skin analysis.

    This pipeline applies multiple normalization steps to reduce variations
    caused by lighting, compression, and color temperature differences:

    1. White balance correction - Normalizes color temperature
    2. CLAHE - Improves local contrast for better skin detail visibility
    3. Denoising - Reduces compression artifacts and camera noise
    4. Standardized resizing - Ensures consistent dimensions (optional)

    Args:
        image_bytes: Original image bytes (JPEG, PNG, etc.)
        preserve_original_size: If True, skip the resizing step (default: False)

    Returns:
        Preprocessed image as bytes (PNG format for lossless quality)

    Raises:
        ValueError: If image cannot be decoded
    """
    try:
        # Load image from bytes
        nparr = np.frombuffer(image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is None:
            raise ValueError("Failed to decode image")

        logger.debug(f"Original image shape: {img.shape}")

        # Step 1: White balance correction
        img = apply_white_balance(img)
        logger.debug("Applied white balance correction")

        # Step 2: CLAHE for local contrast enhancement
        img = apply_clahe(img, clip_limit=2.0, tile_size=(8, 8))
        logger.debug("Applied CLAHE")

        # Step 3: Denoise
        img = denoise_image(img, h=10, template_window=7, search_window=21)
        logger.debug("Applied denoising")

        # Step 4: Resize to standard dimensions (unless preserving original size)
        if not preserve_original_size:
            img = resize_standard(img, target_size=(512, 512))
            logger.debug(f"Resized to standard dimensions: {img.shape}")

        # Convert back to bytes (PNG for lossless quality)
        success, encoded = cv2.imencode(".png", img)
        if not success:
            raise ValueError("Failed to encode preprocessed image")

        result_bytes = encoded.tobytes()
        logger.debug(f"Preprocessing complete. Output size: {len(result_bytes)} bytes")

        return result_bytes

    except Exception as e:
        logger.error(f"Preprocessing failed: {e}")
        raise ValueError(f"Image preprocessing failed: {e}") from e


def preprocess_for_analysis_pil(image_bytes: bytes, preserve_original_size: bool = False) -> bytes:
    """Alternative preprocessing using PIL for compatibility.

    This is a lighter-weight preprocessing option that uses only PIL/Pillow
    without requiring OpenCV. It provides basic normalization but not as
    comprehensive as the OpenCV pipeline.

    Args:
        image_bytes: Original image bytes
        preserve_original_size: If True, skip resizing

    Returns:
        Preprocessed image as bytes (PNG format)
    """
    try:
        with Image.open(io.BytesIO(image_bytes)) as img:
            # Convert to RGB if needed
            if img.mode != "RGB":
                img = img.convert("RGB")

            # Resize if needed
            if not preserve_original_size and img.size != (512, 512):
                img = img.resize((512, 512), Image.LANCZOS)

            # Save as PNG for lossless quality
            output = io.BytesIO()
            img.save(output, format="PNG", optimize=False)
            return output.getvalue()

    except Exception as e:
        logger.error(f"PIL preprocessing failed: {e}")
        raise ValueError(f"Image preprocessing failed: {e}") from e


def check_preprocessing_available() -> bool:
    """Check if OpenCV preprocessing is available.

    Returns:
        True if opencv-python is installed and working, False otherwise
    """
    try:
        import cv2
        # Test basic OpenCV functionality
        test_img = np.zeros((10, 10, 3), dtype=np.uint8)
        cv2.cvtColor(test_img, cv2.COLOR_BGR2LAB)
        return True
    except ImportError:
        return False
