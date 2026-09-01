"""Face landmark detection and alignment for consistent skin analysis."""

from __future__ import annotations

import io
import math

import cv2
import numpy as np
from PIL import Image


class FaceAlignmentError(Exception):
    """Raised when face alignment fails (no face or landmarks detected)."""


def _pil_to_cv2(pil_image: Image.Image) -> np.ndarray:
    """Convert PIL Image to OpenCV format (BGR)."""
    rgb = np.array(pil_image.convert("RGB"))
    return cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)


def _cv2_to_pil(cv_image: np.ndarray) -> Image.Image:
    """Convert OpenCV BGR image to PIL Image."""
    rgb = cv2.cvtColor(cv_image, cv2.COLOR_BGR2RGB)
    return Image.fromarray(rgb)


def _detect_eyes_with_cascade(
    gray: np.ndarray,
) -> tuple[tuple[int, int], tuple[int, int]] | None:
    """Detect left and right eye centers using Haar Cascade."""
    eye_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + "haarcascade_eye.xml")  # type: ignore[attr-defined]

    eyes = eye_cascade.detectMultiScale(
        gray, scaleFactor=1.1, minNeighbors=5, minSize=(20, 20)
    )

    if len(eyes) < 2:
        return None

    # Sort eyes by x-coordinate (left to right)
    eyes_sorted = sorted(eyes, key=lambda e: e[0])

    # Get the two leftmost eyes (assuming frontal face)
    left_eye = eyes_sorted[0]
    right_eye = eyes_sorted[1]

    # Calculate eye centers
    left_eye_center = (left_eye[0] + left_eye[2] // 2, left_eye[1] + left_eye[3] // 2)
    right_eye_center = (
        right_eye[0] + right_eye[2] // 2,
        right_eye[1] + right_eye[3] // 2,
    )

    return left_eye_center, right_eye_center


def _detect_face_region(image: np.ndarray) -> tuple[int, int, int, int] | None:
    """Detect face bounding box using Haar Cascade."""
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    face_cascade = cv2.CascadeClassifier(  # type: ignore[attr-defined]
        cv2.data.haarcascades + "haarcascade_frontalface_default.xml"  # type: ignore[attr-defined]
    )

    faces = face_cascade.detectMultiScale(
        gray,
        scaleFactor=1.1,
        minNeighbors=5,
        minSize=(30, 30),
    )

    if len(faces) == 0:
        return None

    # Return the largest face
    largest_face = max(faces, key=lambda f: f[2] * f[3])
    return tuple(largest_face)


def align_face(
    image_bytes: bytes,
    target_eye_distance: int = 80,
    output_size: tuple[int, int] = (256, 256),
) -> bytes:
    """
    Align face in image to canonical position.

    Steps:
    1. Detect facial landmarks (eyes)
    2. Rotate image to level the eyes horizontally
    3. Scale to consistent inter-eye distance
    4. Crop to centered face region

    Args:
        image_bytes: Input image as bytes
        target_eye_distance: Desired distance between eyes in pixels (default: 80)
        output_size: Output image dimensions (width, height)

    Returns:
        Aligned face image as bytes (JPEG format)

    Raises:
        FaceAlignmentError: If no face or eyes are detected
    """
    # Load image
    with Image.open(io.BytesIO(image_bytes)) as pil_img:
        cv_img = _pil_to_cv2(pil_img)

    gray = cv2.cvtColor(cv_img, cv2.COLOR_BGR2GRAY)

    # Detect eyes
    eye_result = _detect_eyes_with_cascade(gray)

    if eye_result is None:
        # Fallback: Try to detect face and estimate eye positions
        face_bbox = _detect_face_region(cv_img)
        if face_bbox is None:
            raise FaceAlignmentError("No face detected in image")

        # Estimate eye positions based on face bbox (approximate)
        x, y, w, h = face_bbox
        left_eye_center = (int(x + w * 0.3), int(y + h * 0.4))
        right_eye_center = (int(x + w * 0.7), int(y + h * 0.4))
    else:
        left_eye_center, right_eye_center = eye_result

    # Calculate angle to rotate eyes horizontal
    dy = right_eye_center[1] - left_eye_center[1]
    dx = right_eye_center[0] - left_eye_center[0]
    angle = math.degrees(math.atan2(dy, dx))

    # Calculate current eye distance
    current_eye_distance = math.sqrt(dx * dx + dy * dy)

    if current_eye_distance < 10:
        raise FaceAlignmentError("Eyes too close or detection error")

    # Calculate scale to achieve target eye distance
    scale = target_eye_distance / current_eye_distance

    # Calculate center point between eyes
    eyes_center_x = (left_eye_center[0] + right_eye_center[0]) // 2
    eyes_center_y = (left_eye_center[1] + right_eye_center[1]) // 2
    eyes_center = (eyes_center_x, eyes_center_y)

    # Create rotation matrix (rotate + scale)
    rotation_matrix = cv2.getRotationMatrix2D(eyes_center, angle, scale)

    # Adjust translation to center the eyes in output image
    # Eyes should be at approximately 40% from top
    rotation_matrix[0, 2] += (output_size[0] / 2) - eyes_center_x
    rotation_matrix[1, 2] += (output_size[1] * 0.4) - eyes_center_y

    # Apply transformation
    aligned = cv2.warpAffine(
        cv_img,
        rotation_matrix,
        output_size,
        flags=cv2.INTER_LINEAR,
        borderMode=cv2.BORDER_CONSTANT,
        borderValue=(0, 0, 0),
    )

    # Convert back to PIL and encode as JPEG
    pil_aligned = _cv2_to_pil(aligned)
    output_buffer = io.BytesIO()
    pil_aligned.save(output_buffer, format="JPEG", quality=95)
    return output_buffer.getvalue()


def align_face_safe(
    image_bytes: bytes,
    target_eye_distance: int = 80,
    output_size: tuple[int, int] = (256, 256),
) -> bytes:
    """
    Attempt face alignment, return original image if alignment fails.

    This is a safe wrapper around align_face() that returns the original
    image (resized to output_size) if face detection/alignment fails.

    Args:
        image_bytes: Input image as bytes
        target_eye_distance: Desired distance between eyes in pixels
        output_size: Output image dimensions (width, height)

    Returns:
        Aligned face image bytes, or resized original if alignment fails
    """
    try:
        return align_face(image_bytes, target_eye_distance, output_size)
    except (FaceAlignmentError, OSError, ValueError, RuntimeError):
        # If alignment fails, return resized original image
        with Image.open(io.BytesIO(image_bytes)) as img:
            resized = img.convert("RGB").resize(output_size, Image.Resampling.LANCZOS)
            output_buffer = io.BytesIO()
            resized.save(output_buffer, format="JPEG", quality=95)
            return output_buffer.getvalue()
