#!/usr/bin/env python3
"""Verification script for preprocessing dependencies and functionality.

Run this script to verify that all preprocessing dependencies are installed
and working correctly.
"""

import sys


def check_dependencies():
    """Check if all required dependencies are installed."""
    print("Checking preprocessing dependencies...")
    print("-" * 50)

    errors = []

    # Check NumPy
    try:
        import numpy as np
        print(f"✓ NumPy {np.__version__} - OK")
    except ImportError:
        print("✗ NumPy - NOT INSTALLED")
        errors.append("numpy")

    # Check OpenCV
    try:
        import cv2
        print(f"✓ OpenCV {cv2.__version__} - OK")
    except ImportError:
        print("✗ OpenCV - NOT INSTALLED")
        errors.append("opencv-python")

    # Check PIL (should already be installed)
    try:
        from PIL import Image
        print(f"✓ Pillow {Image.__version__} - OK")
    except ImportError:
        print("✗ Pillow - NOT INSTALLED")
        errors.append("pillow")

    print("-" * 50)

    if errors:
        print("\nMissing dependencies detected!")
        print("\nTo install missing packages, run:")
        print(f"  pip install {' '.join(errors)}")
        print("\nOr install from pyproject.toml:")
        print("  pip install -e .")
        return False
    else:
        print("\n✓ All dependencies installed successfully!")
        return True


def test_preprocessing():
    """Test basic preprocessing functionality."""
    print("\nTesting preprocessing functionality...")
    print("-" * 50)

    try:
        from skinproof.preprocessing import (
            check_preprocessing_available,
            preprocess_for_analysis_pil,
        )

        # Check if OpenCV preprocessing is available
        opencv_available = check_preprocessing_available()
        if opencv_available:
            print("✓ OpenCV preprocessing - AVAILABLE")
        else:
            print("⚠ OpenCV preprocessing - NOT AVAILABLE (will use PIL fallback)")

        # Test PIL fallback
        from PIL import Image
        import io

        # Create a simple test image
        test_image = Image.new("RGB", (100, 100), (128, 128, 128))
        img_bytes = io.BytesIO()
        test_image.save(img_bytes, format="PNG")
        img_bytes = img_bytes.getvalue()

        # Test PIL preprocessing
        result = preprocess_for_analysis_pil(img_bytes)
        print(f"✓ PIL preprocessing - OK (output: {len(result)} bytes)")

        if opencv_available:
            from skinproof.preprocessing import preprocess_for_analysis

            result = preprocess_for_analysis(img_bytes)
            print(f"✓ OpenCV preprocessing - OK (output: {len(result)} bytes)")

        print("-" * 50)
        print("\n✓ Preprocessing tests passed!")
        return True

    except Exception as e:
        print(f"\n✗ Preprocessing test failed: {e}")
        import traceback

        traceback.print_exc()
        return False


def main():
    """Main verification routine."""
    print("\n" + "=" * 50)
    print("PREPROCESSING SETUP VERIFICATION")
    print("=" * 50 + "\n")

    deps_ok = check_dependencies()

    if deps_ok:
        tests_ok = test_preprocessing()
        if tests_ok:
            print("\n" + "=" * 50)
            print("✓ SETUP COMPLETE - Ready to use preprocessing!")
            print("=" * 50 + "\n")
            return 0
        else:
            print("\n" + "=" * 50)
            print("⚠ SETUP INCOMPLETE - Tests failed")
            print("=" * 50 + "\n")
            return 1
    else:
        print("\n" + "=" * 50)
        print("✗ SETUP INCOMPLETE - Install dependencies first")
        print("=" * 50 + "\n")
        return 1


if __name__ == "__main__":
    sys.exit(main())
