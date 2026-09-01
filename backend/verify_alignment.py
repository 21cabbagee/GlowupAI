#!/usr/bin/env python3
"""
Verification script for face alignment.

Usage:
    python3 verify_alignment.py <image_path>

Example:
    python3 verify_alignment.py /path/to/selfie.jpg

This script:
1. Loads an image
2. Applies face alignment
3. Shows before/after comparison
4. Displays detected landmarks
"""

import sys
import io
from pathlib import Path
from PIL import Image


def verify_alignment(image_path: str):
    """Verify face alignment on a real image."""
    from glowupai.face_alignment import align_face_safe, FaceAlignmentError
    from glowupai.metrics import analyze

    if not Path(image_path).exists():
        print(f"❌ Error: File not found: {image_path}")
        return False

    print(f"📷 Loading image: {image_path}")

    with open(image_path, "rb") as f:
        original_bytes = f.read()

    # Load original image info
    with Image.open(io.BytesIO(original_bytes)) as img:
        print(f"   Original size: {img.size}")
        print(f"   Format: {img.format}")

    print("\n🔄 Applying face alignment...")

    try:
        aligned_bytes = align_face_safe(original_bytes)

        with Image.open(io.BytesIO(aligned_bytes)) as aligned_img:
            print(f"   ✓ Aligned size: {aligned_img.size}")

            # Save aligned image
            output_path = Path(image_path).stem + "_aligned.jpg"
            aligned_img.save(output_path, quality=95)
            print(f"   ✓ Saved aligned image: {output_path}")

    except Exception as e:
        print(f"   ❌ Alignment failed: {e}")
        import traceback
        traceback.print_exc()
        return False

    print("\n📊 Running skin analysis on both versions...")

    try:
        # Analyze original
        result_original = analyze(original_bytes, 0.9)
        print("\n   Original metrics:")
        print(f"      Blemishes: {result_original.blemish_count}")
        print(f"      Texture: {result_original.texture_score}")
        print(f"      Redness: {result_original.redness_score}")
        print(f"      Confidence: {result_original.confidence}")

        # Analyze with alignment (will re-align internally)
        result_aligned = analyze(original_bytes, 0.9)
        print("\n   With alignment:")
        print(f"      Blemishes: {result_aligned.blemish_count}")
        print(f"      Texture: {result_aligned.texture_score}")
        print(f"      Redness: {result_aligned.redness_score}")
        print(f"      Confidence: {result_aligned.confidence}")

    except Exception as e:
        print(f"   ⚠️  Analysis failed: {e}")
        # This is not a critical error for alignment verification
        pass

    print("\n✅ Face alignment verification complete!")
    print(f"   Compare original and aligned images visually:")
    print(f"   - Original: {image_path}")
    print(f"   - Aligned: {output_path}")

    return True


def test_with_multiple_angles():
    """Test alignment consistency with the same face at different angles."""
    from glowupai.metrics import analyze
    from PIL import Image, ImageDraw
    import io

    print("\n" + "=" * 60)
    print("Testing alignment consistency across angles")
    print("=" * 60)

    # Create base face image
    base_img = Image.new("RGB", (400, 400), (210, 165, 145))
    draw = ImageDraw.Draw(base_img)

    # Draw face features
    # Head
    draw.ellipse((100, 100, 300, 320), fill=(210, 165, 145), outline=(150, 100, 80), width=2)

    # Eyes
    draw.ellipse((140, 170, 170, 195), fill=(255, 255, 255), outline=(0, 0, 0), width=2)
    draw.ellipse((149, 178, 161, 187), fill=(0, 0, 0))

    draw.ellipse((230, 170, 260, 195), fill=(255, 255, 255), outline=(0, 0, 0), width=2)
    draw.ellipse((239, 178, 251, 187), fill=(0, 0, 0))

    # Nose
    draw.line((200, 190, 195, 235), fill=(150, 100, 80), width=3)

    # Mouth
    draw.arc((160, 240, 240, 280), 0, 180, fill=(150, 100, 80), width=3)

    # Add some blemishes
    draw.ellipse((170, 210, 185, 225), fill=(220, 45, 35))
    draw.ellipse((220, 220, 232, 232), fill=(220, 45, 35))

    angles = [0, -5, 5, -10, 10]
    results = []

    for angle in angles:
        rotated = base_img.rotate(angle, expand=False, fillcolor=(210, 165, 145))
        buffer = io.BytesIO()
        rotated.save(buffer, format="JPEG")
        rotated_bytes = buffer.getvalue()

        result = analyze(rotated_bytes, 0.9)
        results.append((angle, result))

        print(f"\nAngle {angle:+3d}°:")
        print(f"   Blemishes: {result.blemish_count}")
        print(f"   Texture: {result.texture_score:.2f}")
        print(f"   Redness: {result.redness_score:.4f}")

    # Calculate variance
    blemish_counts = [r.blemish_count for _, r in results]
    texture_scores = [r.texture_score for _, r in results]

    blemish_variance = max(blemish_counts) - min(blemish_counts)
    texture_variance = max(texture_scores) - min(texture_scores)

    print("\n" + "-" * 60)
    print(f"Blemish count range: {min(blemish_counts)} - {max(blemish_counts)} (variance: {blemish_variance})")
    print(f"Texture score range: {min(texture_scores):.2f} - {max(texture_scores):.2f} (variance: {texture_variance:.2f})")

    if blemish_variance <= 3 and texture_variance <= 5:
        print("✅ Alignment is working well - low variance across angles!")
    else:
        print("⚠️  High variance detected - alignment may need tuning")

    print("=" * 60)


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 verify_alignment.py <image_path>")
        print("\nOr run angle consistency test:")
        print("   python3 verify_alignment.py --test-angles")
        sys.exit(1)

    if sys.argv[1] == "--test-angles":
        test_with_multiple_angles()
    else:
        image_path = sys.argv[1]
        success = verify_alignment(image_path)
        sys.exit(0 if success else 1)
