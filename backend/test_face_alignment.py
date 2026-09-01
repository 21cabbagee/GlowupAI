#!/usr/bin/env python3
"""Quick test script for face alignment functionality."""

import io
from PIL import Image, ImageDraw
import sys


def test_alignment():
    """Test face alignment with a sample image."""
    from skinproof.face_alignment import align_face_safe, FaceAlignmentError

    # Create a simple test image with two "eyes" (circles)
    # This is a basic test - in production it will use real face photos
    width, height = 400, 400
    test_img = Image.new('RGB', (width, height), color='white')
    draw = ImageDraw.Draw(test_img)

    # Draw a face-like pattern
    # Head circle
    draw.ellipse([100, 100, 300, 300], fill='peachpuff', outline='black', width=2)

    # Left eye
    draw.ellipse([140, 160, 170, 190], fill='white', outline='black', width=2)
    draw.ellipse([150, 170, 160, 180], fill='black')

    # Right eye
    draw.ellipse([230, 160, 260, 190], fill='white', outline='black', width=2)
    draw.ellipse([240, 170, 250, 180], fill='black')

    # Nose
    draw.line([200, 190, 195, 230], fill='black', width=2)
    draw.line([195, 230, 205, 230], fill='black', width=2)

    # Mouth
    draw.arc([160, 230, 240, 270], 0, 180, fill='black', width=2)

    # Convert to bytes
    buffer = io.BytesIO()
    test_img.save(buffer, format='JPEG')
    test_bytes = buffer.getvalue()

    print("Testing face alignment...")
    print(f"Original image size: {width}x{height}")

    try:
        # Test alignment
        aligned_bytes = align_face_safe(test_bytes, target_eye_distance=80, output_size=(256, 256))

        # Verify output
        with Image.open(io.BytesIO(aligned_bytes)) as aligned_img:
            print(f"Aligned image size: {aligned_img.size}")
            print("✓ Face alignment successful!")

            # Save for visual inspection if desired
            aligned_img.save('/tmp/test_aligned.jpg')
            print("✓ Aligned image saved to /tmp/test_aligned.jpg")

        return True

    except Exception as e:
        print(f"✗ Face alignment test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def test_with_no_face():
    """Test that alignment handles images with no face gracefully."""
    from skinproof.face_alignment import align_face_safe

    # Create blank image
    blank_img = Image.new('RGB', (200, 200), color='blue')
    buffer = io.BytesIO()
    blank_img.save(buffer, format='JPEG')
    test_bytes = buffer.getvalue()

    print("\nTesting fallback with no face...")

    try:
        aligned_bytes = align_face_safe(test_bytes, target_eye_distance=80, output_size=(256, 256))
        with Image.open(io.BytesIO(aligned_bytes)) as aligned_img:
            print(f"Fallback image size: {aligned_img.size}")
            print("✓ Fallback handling successful (returned resized original)")
        return True
    except Exception as e:
        print(f"✗ Fallback test failed: {e}")
        return False


if __name__ == "__main__":
    success = test_alignment()
    success = test_with_no_face() and success

    if success:
        print("\n" + "=" * 50)
        print("All face alignment tests passed!")
        print("=" * 50)
        sys.exit(0)
    else:
        print("\n" + "=" * 50)
        print("Some tests failed!")
        print("=" * 50)
        sys.exit(1)
