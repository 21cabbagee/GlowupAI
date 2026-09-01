"""
Test ML model consistency.

Tests the same image multiple times and calculates variance to ensure
the model produces consistent predictions.
"""

import sys
import os
from pathlib import Path
import numpy as np

# Add backend to path
sys.path.insert(0, '/Users/21cabbage/GlowupAI/backend')

from glowupai.ml_model import MLModelInference


def test_consistency(model_path: str = None, num_trials: int = 10):
    """
    Test model consistency by running inference multiple times.

    Args:
        model_path: Path to model checkpoint (None = use default)
        num_trials: Number of times to run inference
    """
    print("=" * 60)
    print("MODEL CONSISTENCY TEST")
    print("=" * 60)

    # Find a test image
    data_dir = Path(__file__).parent / "data"
    test_images = list(data_dir.glob("**/*.jpg")) + list(data_dir.glob("**/*.png"))

    if not test_images:
        print("\nERROR: No test images found in data directory")
        print(f"Searched: {data_dir}")
        print("\nPlease add at least one test image to run consistency tests.")
        return False

    test_image = test_images[0]
    print(f"\nTest image: {test_image}")
    print(f"Trials: {num_trials}")

    # Load model
    print("\nLoading model...")
    try:
        if model_path:
            model = MLModelInference(model_path=model_path)
        else:
            model = MLModelInference()
    except FileNotFoundError as e:
        print(f"\nERROR: {e}")
        return False
    except Exception as e:
        print(f"\nERROR: Failed to load model: {e}")
        return False

    print("✓ Model loaded successfully")

    # Read test image
    with open(test_image, 'rb') as f:
        image_bytes = f.read()

    # Run inference multiple times
    print(f"\nRunning inference {num_trials} times...")
    results = {
        'redness': [],
        'blemishes': [],
        'texture': [],
        'darkspots': []
    }

    for i in range(num_trials):
        try:
            result = model.predict(image_bytes, quality_score=0.9, baseline=None)

            results['redness'].append(result.redness_score)
            results['blemishes'].append(result.blemish_count)
            results['texture'].append(result.texture_score)
            results['darkspots'].append(result.darkspot_area)

            # Show progress
            if (i + 1) % 5 == 0 or i == 0:
                print(f"  Trial {i+1}/{num_trials}: "
                      f"R={result.redness_score:.4f}, "
                      f"B={result.blemish_count:.1f}, "
                      f"T={result.texture_score:.2f}, "
                      f"D={result.darkspot_area:.4f}")

        except Exception as e:
            print(f"\nERROR: Inference failed on trial {i+1}: {e}")
            return False

    # Calculate statistics
    print("\n" + "=" * 60)
    print("CONSISTENCY ANALYSIS")
    print("=" * 60)

    metrics_info = {
        'redness': ('Redness Score', 0.01, 4),  # (name, threshold, decimals)
        'blemishes': ('Blemish Count', 1.0, 1),
        'texture': ('Texture Score', 1.0, 2),
        'darkspots': ('Dark Spot Area', 0.01, 4)
    }

    all_passed = True
    results_table = []

    for metric, (name, threshold, decimals) in metrics_info.items():
        values = np.array(results[metric])
        mean = np.mean(values)
        std = np.std(values)
        variance = np.var(values)
        min_val = np.min(values)
        max_val = np.max(values)
        range_val = max_val - min_val

        # Calculate coefficient of variation (CV)
        # CV = (std / mean) * 100
        # For values near 0, we use absolute std instead
        if mean > 0.001:
            cv = (std / mean) * 100
        else:
            cv = std * 100

        # Determine if passed
        # For low values (near 0), check absolute std instead of percentage
        if mean < threshold:
            passed = std < (threshold * 0.1)  # Allow 10% of threshold as variation
        else:
            passed = cv < 1.0  # Less than 1% coefficient of variation

        status = "✓ PASS" if passed else "✗ FAIL"
        all_passed = all_passed and passed

        results_table.append({
            'name': name,
            'mean': mean,
            'std': std,
            'cv': cv,
            'min': min_val,
            'max': max_val,
            'range': range_val,
            'passed': passed,
            'decimals': decimals
        })

    # Print results table
    print(f"\n{'Metric':<20} {'Mean':<12} {'Std Dev':<12} {'CV %':<10} {'Range':<12} {'Status':<10}")
    print("-" * 76)

    for result in results_table:
        decimals = result['decimals']
        status_symbol = "✓" if result['passed'] else "✗"

        print(
            f"{result['name']:<20} "
            f"{result['mean']:<12.{decimals}f} "
            f"{result['std']:<12.{decimals}f} "
            f"{result['cv']:<10.2f} "
            f"{result['range']:<12.{decimals}f} "
            f"{status_symbol}"
        )

    # Summary
    print("\n" + "=" * 60)
    if all_passed:
        print("✓ SUCCESS: All metrics are consistent (variance < 1%)")
        print("\nThe model produces consistent predictions across multiple runs.")
        print("Safe to deploy to production.")
    else:
        print("✗ FAILURE: Some metrics show high variance")
        print("\nThe model predictions are not consistent enough.")
        print("Recommendations:")
        print("  - Check if model is using dropout/batch norm in eval mode")
        print("  - Verify preprocessing is deterministic")
        print("  - Consider retraining with more data")
        print("  - Review model architecture for sources of randomness")

    print("=" * 60)

    return all_passed


if __name__ == "__main__":
    # Parse arguments
    model_path = None
    if len(sys.argv) > 1:
        model_path = sys.argv[1]

    # Run test
    success = test_consistency(model_path=model_path, num_trials=10)

    # Exit with appropriate code
    sys.exit(0 if success else 1)
