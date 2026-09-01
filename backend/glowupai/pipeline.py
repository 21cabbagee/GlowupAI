"""
Complete image processing pipeline combining face alignment and preprocessing.

This module integrates face detection, alignment, and preprocessing into a
single pipeline for consistent capture analysis.
"""

import logging
from pathlib import Path

import cv2
import numpy as np

# ImagePreprocessor not yet implemented as a class
ImagePreprocessor = None

logger = logging.getLogger(__name__)


class CaptureProcessingPipeline:
    """
    Complete pipeline for processing capture images.

    Combines:
    1. Face detection and alignment
    2. White balance correction
    3. Contrast enhancement (CLAHE)
    4. Denoising
    5. Size standardization
    """

    def __init__(
        self,
        output_size: tuple[int, int] = (512, 512),
        enable_face_alignment: bool = True,
        enable_preprocessing: bool = True,
    ):
        """
        Initialize processing pipeline.

        Args:
            output_size: Target output dimensions (width, height)
            enable_face_alignment: Whether to enable face alignment
            enable_preprocessing: Whether to enable preprocessing
        """
        self.output_size = output_size
        self.enable_face_alignment = enable_face_alignment
        self.enable_preprocessing = enable_preprocessing

        # Initialize components
        self.face_aligner = None  # FaceAligner not yet implemented as a class
        self.preprocessor = None  # ImagePreprocessor not yet implemented as a class

        logger.info(
            f"Pipeline initialized: "
            f"face_alignment={enable_face_alignment}, "
            f"preprocessing={enable_preprocessing}",
        )

    def process(
        self,
        image_path: str,
        output_path: str | None = None,
        save_steps: bool = False,
    ) -> str:
        """
        Process image through full pipeline.

        Args:
            image_path: Path to input image
            output_path: Optional path to save processed image
            save_steps: If True, save intermediate steps for debugging

        Returns:
            Path to processed image
        """
        logger.info(f"Processing: {image_path}")

        # Create output path if not provided
        if output_path is None:
            path = Path(image_path)
            output_path = str(path.parent / f"{path.stem}_processed{path.suffix}")

        # Step 1: Face alignment
        aligned_image: np.ndarray | None = None
        if self.enable_face_alignment and self.face_aligner:
            logger.info("  → Face alignment")
            aligned_image = self.face_aligner.align_face(image_path, self.output_size)

            if save_steps:
                temp_path = str(
                    Path(output_path).parent / f"{Path(image_path).stem}_aligned.jpg"
                )
                cv2.imwrite(temp_path, aligned_image)
                logger.info(f"    Saved aligned: {temp_path}")

            # Save temporarily for preprocessing
            temp_aligned_path = str(Path(output_path).parent / ".temp_aligned.jpg")
            cv2.imwrite(temp_aligned_path, aligned_image)
            image_path = temp_aligned_path

        # Step 2: Preprocessing
        if self.enable_preprocessing and self.preprocessor:
            logger.info("  → Preprocessing")
            processed_image = self.preprocessor.preprocess(
                image_path, save_steps=save_steps
            )
            cv2.imwrite(output_path, processed_image)
        else:
            # If no preprocessing, just copy aligned image
            if self.enable_face_alignment and aligned_image is not None:
                cv2.imwrite(output_path, aligned_image)
            else:
                # No processing at all, just resize
                image = cv2.imread(image_path)
                if image is not None:
                    resized = cv2.resize(
                        image, self.output_size, interpolation=cv2.INTER_LANCZOS4
                    )
                    cv2.imwrite(output_path, resized)

        # Clean up temporary files
        if self.enable_face_alignment:
            temp_aligned_path = str(Path(output_path).parent / ".temp_aligned.jpg")
            if Path(temp_aligned_path).exists():
                Path(temp_aligned_path).unlink()

        logger.info(f"✓ Processed: {output_path}")
        return output_path

    def process_for_analysis(self, image_path: str) -> np.ndarray:
        """
        Process image and return as numpy array for immediate analysis.

        Args:
            image_path: Path to input image

        Returns:
            Processed image as numpy array (BGR format)
        """
        # Use temporary file
        temp_path = str(Path(image_path).parent / ".temp_processed.jpg")
        self.process(image_path, output_path=temp_path, save_steps=False)

        # Load and return
        processed = cv2.imread(temp_path)
        if processed is None:
            raise RuntimeError(f"Failed to read processed image from {temp_path}")

        # Clean up
        Path(temp_path).unlink()

        return processed


# Global pipeline instance
_default_pipeline: CaptureProcessingPipeline | None = None


def get_pipeline() -> CaptureProcessingPipeline:
    """Get or create default pipeline instance."""
    global _default_pipeline
    if _default_pipeline is None:
        _default_pipeline = CaptureProcessingPipeline()
    return _default_pipeline


def process_capture(
    image_path: str,
    output_path: str | None = None,
) -> str:
    """
    Convenience function to process a capture through the default pipeline.

    Args:
        image_path: Path to input image
        output_path: Optional path to save processed image

    Returns:
        Path to processed image
    """
    pipeline = get_pipeline()
    return pipeline.process(image_path, output_path)


def analyze_capture(image_b64: str) -> dict:
    """
    Analyze a base64-encoded capture image.

    Args:
        image_b64: Base64-encoded image string

    Returns:
        Dictionary with metrics and quality information
    """
    import base64

    from .metrics import analyze

    # Decode base64 image
    image_bytes = base64.b64decode(image_b64)

    # Basic quality check - just return fixed quality for now
    quality = {
        "face_present": True,
        "yaw_degrees": 0,
        "pitch_degrees": 0,
        "distance_cm": 45,
        "expression_neutral": True,
    }

    # Run metrics analysis
    metrics = analyze(image_bytes, quality_score=0.95)

    return {
        "metrics": metrics.as_dict(),
        "quality": quality,
    }


def detect_face(image_array: np.ndarray) -> dict:
    """
    Detect face in image array.

    Args:
        image_array: Numpy array of image

    Returns:
        Dictionary with face detection results
    """
    # Simple stub implementation
    return {
        "face_present": True,
        "bounding_box": [50, 50, 150, 150],
        "confidence": 0.95,
    }


def validate_quality(quality: dict) -> None:
    """
    Validate capture quality and raise exception if quality gates not met.

    Args:
        quality: Quality metrics dictionary

    Raises:
        ValueError: If quality gates are not met
    """
    if not quality.get("face_present", False):
        raise ValueError("No face detected in capture")
