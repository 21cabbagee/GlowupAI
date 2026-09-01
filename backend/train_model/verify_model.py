"""
Comprehensive model verification script.

Tests:
1. Model loading
2. Architecture verification (MobileNetV2, 4 outputs)
3. Inference on test images
4. Prediction consistency (same input → same output)
5. Output range validation
6. Comparison with placeholder/baseline predictions
7. Multi-image variance testing
"""

import torch
import numpy as np
import cv2
from pathlib import Path
from typing import Dict, List
import json
import sys

from model import SkinAnalysisModel
from deploy_model import ProductionModel


class ModelVerifier:
    """Comprehensive model verification."""

    def __init__(self, model_path: str):
        self.model_path = model_path
        self.model = None
        self.test_results = {}

    def verify_loading(self) -> bool:
        """Test 1: Verify model can be loaded."""
        print("\n" + "=" * 60)
        print("TEST 1: MODEL LOADING")
        print("=" * 60)

        try:
            # Check file exists
            if not Path(self.model_path).exists():
                print(f"❌ Model file not found: {self.model_path}")
                return False

            file_size = Path(self.model_path).stat().st_size / (1024 * 1024)
            print(f"📁 Model file: {self.model_path}")
            print(f"📊 File size: {file_size:.2f} MB")

            # Load checkpoint
            print(f"\n📦 Loading checkpoint...")
            checkpoint = torch.load(self.model_path, map_location="cpu")

            # Check checkpoint structure
            if "model_state_dict" in checkpoint:
                print(f"✓ Checkpoint contains model_state_dict")
                if "val_loss" in checkpoint:
                    print(f"✓ Validation loss: {checkpoint['val_loss']:.4f}")
                if "epoch" in checkpoint:
                    print(f"✓ Trained for {checkpoint['epoch']} epochs")
            else:
                print(f"⚠️  Checkpoint is state_dict only (no metadata)")

            # Load into model
            print(f"\n🔧 Loading into model architecture...")
            self.model = ProductionModel(self.model_path)

            print(f"\n✅ Model loaded successfully!")
            self.test_results["loading"] = True
            return True

        except Exception as e:
            print(f"❌ Loading failed: {e}")
            self.test_results["loading"] = False
            return False

    def verify_architecture(self) -> bool:
        """Test 2: Verify architecture matches expected structure."""
        print("\n" + "=" * 60)
        print("TEST 2: ARCHITECTURE VERIFICATION")
        print("=" * 60)

        try:
            # Check model components
            model = self.model.model  # Get underlying PyTorch model

            print(f"\n🔍 Checking model components...")

            # 1. Check backbone
            if hasattr(model, 'features'):
                print(f"✓ MobileNetV2 backbone present")
            else:
                print(f"❌ Missing features/backbone")
                return False

            # 2. Check output heads
            required_heads = ['redness_head', 'blemish_head', 'texture_head', 'darkspot_head']
            for head in required_heads:
                if hasattr(model, head):
                    print(f"✓ {head} present")
                else:
                    print(f"❌ Missing {head}")
                    return False

            # 3. Count parameters
            total_params = sum(p.numel() for p in model.parameters())
            trainable_params = sum(p.numel() for p in model.parameters() if p.requires_grad)

            print(f"\n📊 Model statistics:")
            print(f"   Total parameters: {total_params:,}")
            print(f"   Trainable parameters: {trainable_params:,}")

            # 4. Test forward pass
            print(f"\n🧪 Testing forward pass...")
            test_input = torch.randn(1, 3, 224, 224)
            with torch.no_grad():
                output = model(test_input)

            # Check outputs
            expected_outputs = ['redness', 'blemishes', 'texture', 'darkspots']
            for key in expected_outputs:
                if key in output:
                    print(f"✓ Output '{key}' present with shape {output[key].shape}")
                else:
                    print(f"❌ Missing output '{key}'")
                    return False

            print(f"\n✅ Architecture verified!")
            self.test_results["architecture"] = True
            return True

        except Exception as e:
            print(f"❌ Architecture verification failed: {e}")
            self.test_results["architecture"] = False
            return False

    def verify_inference(self, test_images: List[str]) -> bool:
        """Test 3: Verify inference works on test images."""
        print("\n" + "=" * 60)
        print("TEST 3: INFERENCE VERIFICATION")
        print("=" * 60)

        try:
            if not test_images:
                print(f"❌ No test images provided")
                return False

            test_image = test_images[0]
            print(f"\n📸 Test image: {Path(test_image).name}")

            # Run inference
            print(f"🔄 Running inference...")
            prediction = self.model.predict(test_image)

            print(f"\n📊 Predictions:")
            print(f"   Redness: {prediction['redness_score']:.4f}")
            print(f"   Blemishes: {prediction['blemish_count']}")
            print(f"   Texture: {prediction['texture_score']:.2f}")
            print(f"   Dark spots: {prediction['darkspot_area']:.4f}")

            print(f"\n✅ Inference successful!")
            self.test_results["inference"] = True
            self.test_results["sample_prediction"] = prediction
            return True

        except Exception as e:
            print(f"❌ Inference failed: {e}")
            self.test_results["inference"] = False
            return False

    def verify_consistency(self, test_image: str, num_runs: int = 5) -> bool:
        """Test 4: Verify predictions are consistent."""
        print("\n" + "=" * 60)
        print("TEST 4: PREDICTION CONSISTENCY")
        print("=" * 60)

        try:
            print(f"\n📸 Test image: {Path(test_image).name}")
            print(f"🔄 Running {num_runs} predictions...")

            predictions = []
            for i in range(num_runs):
                pred = self.model.predict(test_image)
                predictions.append(pred)
                print(f"   Run {i+1}: R={pred['redness_score']:.4f}, "
                      f"B={pred['blemish_count']}, "
                      f"T={pred['texture_score']:.2f}, "
                      f"D={pred['darkspot_area']:.4f}")

            # Calculate variance for each metric
            print(f"\n📊 Consistency Analysis:")
            metrics = ['redness_score', 'blemish_count', 'texture_score', 'darkspot_area']
            all_consistent = True

            for metric in metrics:
                values = [p[metric] for p in predictions]
                variance = np.var(values)
                std = np.std(values)

                print(f"\n   {metric}:")
                print(f"      Mean: {np.mean(values):.4f}")
                print(f"      Std:  {std:.6f}")
                print(f"      Var:  {variance:.8f}")

                # Check consistency (should have 0 variance for deterministic model)
                if variance < 1e-8:
                    print(f"      ✅ Perfect consistency (deterministic)")
                elif variance < 0.01:
                    print(f"      ✓ Excellent consistency")
                else:
                    print(f"      ⚠️  High variance detected")
                    all_consistent = False

            if all_consistent:
                print(f"\n✅ Consistency verified!")
            else:
                print(f"\n⚠️  Some metrics show variance (may be due to GPU non-determinism)")

            self.test_results["consistency"] = all_consistent
            self.test_results["consistency_details"] = {
                metric: {
                    "variance": float(np.var([p[metric] for p in predictions])),
                    "std": float(np.std([p[metric] for p in predictions]))
                }
                for metric in metrics
            }
            return True

        except Exception as e:
            print(f"❌ Consistency check failed: {e}")
            self.test_results["consistency"] = False
            return False

    def verify_output_ranges(self, test_images: List[str]) -> bool:
        """Test 5: Verify output values are within expected ranges."""
        print("\n" + "=" * 60)
        print("TEST 5: OUTPUT RANGE VALIDATION")
        print("=" * 60)

        try:
            print(f"\n🔄 Testing on {len(test_images)} images...")

            all_valid = True
            violations = []

            for img_path in test_images:
                pred = self.model.predict(img_path)

                # Check ranges
                if not (0 <= pred['redness_score'] <= 1):
                    violations.append(f"{Path(img_path).name}: Redness {pred['redness_score']:.4f} not in [0, 1]")
                    all_valid = False

                if not (0 <= pred['blemish_count'] <= 100):
                    violations.append(f"{Path(img_path).name}: Blemishes {pred['blemish_count']} not in [0, 100]")
                    all_valid = False

                if not (0 <= pred['texture_score'] <= 30):
                    violations.append(f"{Path(img_path).name}: Texture {pred['texture_score']:.2f} not in [0, 30]")
                    all_valid = False

                if not (0 <= pred['darkspot_area'] <= 1):
                    violations.append(f"{Path(img_path).name}: Dark spots {pred['darkspot_area']:.4f} not in [0, 1]")
                    all_valid = False

            print(f"\n📊 Expected Ranges:")
            print(f"   ✓ Redness: [0, 1] (sigmoid)")
            print(f"   ✓ Blemishes: [0, 100+] (count)")
            print(f"   ✓ Texture: [0, 30+] (score)")
            print(f"   ✓ Dark spots: [0, 1] (area ratio)")

            if violations:
                print(f"\n⚠️  Range violations found:")
                for v in violations[:10]:  # Show first 10
                    print(f"   - {v}")
                if len(violations) > 10:
                    print(f"   ... and {len(violations) - 10} more")
            else:
                print(f"\n✅ All outputs within expected ranges!")

            self.test_results["range_validation"] = all_valid
            self.test_results["range_violations"] = len(violations)
            return all_valid

        except Exception as e:
            print(f"❌ Range validation failed: {e}")
            self.test_results["range_validation"] = False
            return False

    def verify_vs_placeholder(self, test_image: str) -> bool:
        """Test 6: Compare with placeholder/baseline predictions."""
        print("\n" + "=" * 60)
        print("TEST 6: PLACEHOLDER COMPARISON")
        print("=" * 60)

        try:
            # Placeholder predictions (typical baseline values)
            placeholder = {
                'redness_score': 0.5,
                'blemish_count': 10,
                'texture_score': 15.0,
                'darkspot_area': 0.2
            }

            print(f"\n📊 Placeholder/Baseline predictions:")
            print(f"   Redness: {placeholder['redness_score']:.4f}")
            print(f"   Blemishes: {placeholder['blemish_count']}")
            print(f"   Texture: {placeholder['texture_score']:.2f}")
            print(f"   Dark spots: {placeholder['darkspot_area']:.4f}")

            # Get trained model predictions
            trained_pred = self.model.predict(test_image)

            print(f"\n📊 Trained model predictions:")
            print(f"   Redness: {trained_pred['redness_score']:.4f}")
            print(f"   Blemishes: {trained_pred['blemish_count']}")
            print(f"   Texture: {trained_pred['texture_score']:.2f}")
            print(f"   Dark spots: {trained_pred['darkspot_area']:.4f}")

            # Calculate differences
            print(f"\n📊 Differences (trained - placeholder):")
            diff = {
                'redness_score': trained_pred['redness_score'] - placeholder['redness_score'],
                'blemish_count': trained_pred['blemish_count'] - placeholder['blemish_count'],
                'texture_score': trained_pred['texture_score'] - placeholder['texture_score'],
                'darkspot_area': trained_pred['darkspot_area'] - placeholder['darkspot_area']
            }

            for metric, d in diff.items():
                print(f"   {metric}: {d:+.4f}")

            # Check if predictions are different from placeholder
            is_different = any(abs(d) > 0.01 for d in diff.values())

            if is_different:
                print(f"\n✅ Model predictions differ from placeholder (trained model is active)")
            else:
                print(f"\n⚠️  Model predictions are very close to placeholder")

            self.test_results["placeholder_comparison"] = {
                "is_different": is_different,
                "differences": diff
            }
            return is_different

        except Exception as e:
            print(f"❌ Placeholder comparison failed: {e}")
            return False

    def verify_variance(self, test_images: List[str]) -> bool:
        """Test 7: Verify predictions vary across different images."""
        print("\n" + "=" * 60)
        print("TEST 7: MULTI-IMAGE VARIANCE")
        print("=" * 60)

        try:
            print(f"\n🔄 Testing on {len(test_images)} different images...")

            predictions = []
            for i, img_path in enumerate(test_images, 1):
                pred = self.model.predict(img_path)
                predictions.append(pred)
                print(f"   Image {i}: R={pred['redness_score']:.4f}, "
                      f"B={pred['blemish_count']}, "
                      f"T={pred['texture_score']:.2f}, "
                      f"D={pred['darkspot_area']:.4f}")

            # Calculate variance across images
            print(f"\n📊 Cross-image Variance Analysis:")
            metrics = ['redness_score', 'blemish_count', 'texture_score', 'darkspot_area']
            all_varied = True

            for metric in metrics:
                values = [p[metric] for p in predictions]
                variance = np.var(values)
                std = np.std(values)
                min_val = np.min(values)
                max_val = np.max(values)

                print(f"\n   {metric}:")
                print(f"      Range: [{min_val:.4f}, {max_val:.4f}]")
                print(f"      Mean: {np.mean(values):.4f}")
                print(f"      Std:  {std:.4f}")
                print(f"      Var:  {variance:.4f}")

                # Check for reasonable variance (model should differentiate images)
                if variance < 0.001:
                    print(f"      ⚠️  Very low variance - model may not be differentiating well")
                    all_varied = False
                elif variance < 0.01:
                    print(f"      ✓ Low variance (images may be similar)")
                else:
                    print(f"      ✅ Good variance (model differentiates images)")

            if all_varied:
                print(f"\n✅ Model shows reasonable variance across images!")
            else:
                print(f"\n⚠️  Model shows limited variance - may need more diverse training data")

            self.test_results["variance"] = {
                metric: {
                    "variance": float(np.var([p[metric] for p in predictions])),
                    "std": float(np.std([p[metric] for p in predictions])),
                    "range": [float(np.min([p[metric] for p in predictions])),
                             float(np.max([p[metric] for p in predictions]))]
                }
                for metric in metrics
            }
            return True

        except Exception as e:
            print(f"❌ Variance check failed: {e}")
            return False

    def generate_report(self):
        """Generate final verification report."""
        print("\n" + "=" * 60)
        print("VERIFICATION REPORT")
        print("=" * 60)

        print(f"\n📋 Test Results:")
        passed = sum(1 for v in self.test_results.values() if isinstance(v, bool) and v)
        total = sum(1 for v in self.test_results.values() if isinstance(v, bool))

        print(f"   Passed: {passed}/{total} tests")

        for test, result in self.test_results.items():
            if isinstance(result, bool):
                status = "✅ PASS" if result else "❌ FAIL"
                print(f"   {status}: {test}")

        # Overall assessment
        print(f"\n🎯 Overall Assessment:")

        if passed == total:
            print(f"   ✅ All tests passed - model is production ready!")
        elif passed >= total * 0.8:
            print(f"   ✓ Most tests passed - model is functional")
        else:
            print(f"   ⚠️  Several tests failed - model needs attention")

        # Save results
        results_file = "/Users/21cabbage/GlowupAI/backend/train_model/verification_results.json"
        with open(results_file, 'w') as f:
            json.dump(self.test_results, f, indent=2)
        print(f"\n📁 Detailed results saved to: {results_file}")


def main():
    """Run all verification tests."""
    print("=" * 60)
    print("MODEL VERIFICATION SUITE")
    print("=" * 60)

    MODEL_PATH = "/Users/21cabbage/GlowupAI/backend/train_model/checkpoints/best_model.pth"
    IMAGES_DIR = "/Users/21cabbage/GlowupAI/backend/train_model/data/images"

    # Get test images (select diverse ones)
    all_images = sorted(Path(IMAGES_DIR).glob("*.jpg"))
    if len(all_images) < 5:
        print(f"❌ Not enough test images found (need at least 5)")
        sys.exit(1)

    # Select 5 images at regular intervals for diversity
    step = len(all_images) // 5
    test_images = [str(all_images[i * step]) for i in range(5)]

    print(f"\n📁 Model: {MODEL_PATH}")
    print(f"📸 Test images: {len(test_images)} selected from {len(all_images)} available")

    # Run verification
    verifier = ModelVerifier(MODEL_PATH)

    # Test 1: Loading
    if not verifier.verify_loading():
        print(f"\n❌ Model loading failed - cannot continue")
        sys.exit(1)

    # Test 2: Architecture
    verifier.verify_architecture()

    # Test 3: Inference
    verifier.verify_inference(test_images)

    # Test 4: Consistency
    verifier.verify_consistency(test_images[0], num_runs=5)

    # Test 5: Output ranges
    verifier.verify_output_ranges(test_images)

    # Test 6: Placeholder comparison
    verifier.verify_vs_placeholder(test_images[0])

    # Test 7: Variance
    verifier.verify_variance(test_images)

    # Final report
    verifier.generate_report()

    print("\n" + "=" * 60)
    print("VERIFICATION COMPLETE")
    print("=" * 60)


if __name__ == "__main__":
    main()
