# Model Deployment Guide

This guide covers deploying the trained ML model to production.

## Overview

The deployment process:
1. Copies trained model to backend
2. Verifies model loads correctly
3. Tests consistency
4. Enables with environment variable

## Prerequisites

- Training completed successfully
- Model checkpoint exists at: `checkpoints/best_model.pth`
- Backend environment configured

## Deployment Steps

### 1. Automated Deployment (Recommended)

Run the deployment script:

```bash
cd /Users/21cabbage/GlowupAI/backend/train_model
./DEPLOY.sh
```

This script will:
- ✓ Verify trained model exists
- ✓ Create models directory in backend
- ✓ Copy model to production location
- ✓ Test model loading
- ✓ Run consistency tests

### 2. Manual Deployment

If you need to deploy manually:

```bash
# 1. Create models directory
mkdir -p /Users/21cabbage/GlowupAI/backend/skinproof/models

# 2. Copy model
cp checkpoints/best_model.pth \
   /Users/21cabbage/GlowupAI/backend/skinproof/models/skin_analysis_v2.pth

# 3. Test model loading
cd /Users/21cabbage/GlowupAI/backend/train_model
python3 test_consistency.py
```

## Enabling the Model

The model is controlled by the `USE_NEW_MODEL` environment variable:

### Option A: Temporary (Current Session)

```bash
export USE_NEW_MODEL=1
```

### Option B: Permanent (Add to .env or environment config)

```bash
# Add to backend/.env
USE_NEW_MODEL=1
```

### Option C: Test Single Request

```bash
USE_NEW_MODEL=1 python3 -m skinproof.cli analyze test_image.jpg
```

## Testing

### 1. Consistency Test

Tests the same image 10 times to verify consistent predictions:

```bash
cd /Users/21cabbage/GlowupAI/backend/train_model
python3 test_consistency.py
```

**Expected output:**
- ✓ All metrics show variance < 1%
- ✓ Model produces consistent predictions

**If test fails:**
- Review model architecture for sources of randomness
- Check preprocessing is deterministic
- Consider retraining

### 2. Single Image Test

Test with a specific image:

```bash
cd /Users/21cabbage/GlowupAI/backend
USE_NEW_MODEL=1 python3 << EOF
from pathlib import Path
from skinproof.ml_model import MLModelInference

model = MLModelInference()
image_path = "path/to/test/image.jpg"

with open(image_path, 'rb') as f:
    image_bytes = f.read()

result = model.predict(image_bytes, quality_score=0.9, baseline=None)
print(f"Redness: {result.redness_score:.4f}")
print(f"Blemishes: {result.blemish_count:.1f}")
print(f"Texture: {result.texture_score:.2f}")
print(f"Dark spots: {result.darkspot_area:.4f}")
EOF
```

### 3. A/B Comparison

Compare old vs new model on same image:

```bash
cd /Users/21cabbage/GlowupAI/backend

# Test with deterministic model
USE_NEW_MODEL=0 python3 -m skinproof.cli analyze test_image.jpg > old_result.json

# Test with ML model
USE_NEW_MODEL=1 python3 -m skinproof.cli analyze test_image.jpg > new_result.json

# Compare results
diff old_result.json new_result.json
```

## Rollback

### Quick Rollback

Disable the ML model immediately:

```bash
unset USE_NEW_MODEL
# or
export USE_NEW_MODEL=0
```

The system will automatically fall back to the deterministic model.

### Remove Model Files

If you want to completely remove the ML model:

```bash
rm -f /Users/21cabbage/GlowupAI/backend/skinproof/models/skin_analysis_v2.pth
```

## Monitoring

### Check Which Model is Active

```python
import os
print(f"Using ML model: {os.getenv('USE_NEW_MODEL', '0') == '1'}")
```

### Model Version in Results

Check the `model_version` field in analysis results:
- `"deterministic-3.0"` = Old model
- `"ml-v2.0"` = New ML model

### Logs

The ML model logs key events:
- Model loading: `"Loading ML model from..."`
- Fallback: `"ML model failed, falling back to deterministic"`

Monitor logs for:
- Loading errors
- Inference failures
- Automatic fallbacks

## Troubleshooting

### Model Not Found Error

```
FileNotFoundError: Model file not found: .../skin_analysis_v2.pth
```

**Solution:**
```bash
cd /Users/21cabbage/GlowupAI/backend/train_model
./DEPLOY.sh
```

### Model Loading Fails

```
RuntimeError: Failed to load ML model
```

**Checks:**
1. Model file exists and isn't corrupted
2. PyTorch version matches training version
3. Model architecture matches trained model

**Solution:**
```bash
# Verify file
ls -lh /Users/21cabbage/GlowupAI/backend/skinproof/models/skin_analysis_v2.pth

# Re-deploy
cd /Users/21cabbage/GlowupAI/backend/train_model
./DEPLOY.sh
```

### Inconsistent Predictions

If consistency test fails:

1. **Check dropout is disabled:**
   - Model uses `model.eval()` automatically
   
2. **Verify preprocessing:**
   - Face alignment should be deterministic
   - Image normalization should be consistent

3. **Test with multiple images:**
   ```bash
   python3 test_consistency.py
   ```

### Performance Issues

If inference is slow:

1. **Check device:**
   ```python
   from skinproof.ml_model import get_ml_model
   model = get_ml_model()
   print(model.device)  # Should be 'cpu' or 'cuda'
   ```

2. **Use GPU if available:**
   ```python
   model = MLModelInference(device="cuda")
   ```

3. **Profile inference:**
   ```bash
   python3 -m cProfile -s cumtime test_model.py
   ```

## Production Checklist

Before enabling in production:

- [ ] Model trained successfully
- [ ] Deployment script completed without errors
- [ ] Consistency test passed
- [ ] Tested on sample images
- [ ] A/B comparison looks reasonable
- [ ] Monitoring/logging configured
- [ ] Rollback plan tested
- [ ] Team notified of deployment

## Architecture Notes

### Model Pipeline

```
Image bytes
  ↓
Face alignment (80px eye distance, 224x224)
  ↓
Normalization (0-1 range)
  ↓
ML Model (MobileNetV2 + multi-task heads)
  ↓
MetricResult
```

### Model Switching Logic

The `analyze()` function in `metrics.py`:
1. Checks `USE_NEW_MODEL` environment variable
2. If `"1"`, uses ML model
3. If ML model fails, falls back to deterministic
4. If `"0"` or unset, uses deterministic model

### File Locations

| File | Location |
|------|----------|
| Trained checkpoint | `train_model/checkpoints/best_model.pth` |
| Production model | `backend/skinproof/models/skin_analysis_v2.pth` |
| ML integration | `backend/skinproof/ml_model.py` |
| Analysis function | `backend/skinproof/metrics.py` |
| Deployment script | `train_model/DEPLOY.sh` |
| Consistency test | `train_model/test_consistency.py` |

## Support

For issues or questions:
1. Check logs for error messages
2. Review this guide's troubleshooting section
3. Test with consistency script
4. Roll back if needed

## Version History

- **v2.0**: ML model with MobileNetV2 architecture
- **v3.0**: Deterministic pixel-based analysis (fallback)
