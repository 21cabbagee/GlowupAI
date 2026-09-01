# Model Deployment - Quick Start

🎯 **Status**: Training in progress (~90 minutes)
📍 **You are here**: Preparation complete, waiting for training to finish

## What's Been Prepared

All deployment infrastructure is ready:

```
✓ ml_model.py          - ML model integration code
✓ metrics.py           - Updated with model switching (USE_NEW_MODEL flag)
✓ DEPLOY.sh            - One-command deployment script
✓ test_consistency.py  - Model consistency validation
✓ DEPLOYMENT_GUIDE.md  - Complete deployment documentation
✓ PRE_DEPLOY_CHECK.sh  - Pre-deployment validation
```

## When Training Finishes

### Step 1: Validate Readiness (30 seconds)

```bash
cd /Users/21cabbage/GlowupAI/backend/train_model
./PRE_DEPLOY_CHECK.sh
```

This checks:
- ✓ Trained model exists
- ✓ Integration code ready
- ✓ Dependencies available
- ✓ Test data present

### Step 2: Deploy Model (1 minute)

```bash
./DEPLOY.sh
```

This will:
1. Copy trained model to backend
2. Verify model loads correctly
3. Run consistency tests
4. Report deployment status

### Step 3: Enable New Model

```bash
export USE_NEW_MODEL=1
```

## One-Line Deployment (After Training)

```bash
cd /Users/21cabbage/GlowupAI/backend/train_model && ./PRE_DEPLOY_CHECK.sh && ./DEPLOY.sh && export USE_NEW_MODEL=1
```

## Testing

Test with sample image:
```bash
cd /Users/21cabbage/GlowupAI/backend
USE_NEW_MODEL=1 python3 << EOF
from skinproof.ml_model import MLModelInference
model = MLModelInference()
with open("test_image.jpg", 'rb') as f:
    result = model.predict(f.read(), 0.9)
print(f"Redness: {result.redness_score:.4f}")
print(f"Blemishes: {result.blemish_count:.1f}")
EOF
```

## Rollback (If Needed)

```bash
unset USE_NEW_MODEL
# or
export USE_NEW_MODEL=0
```

System automatically falls back to deterministic model.

## Architecture

### File Structure

```
backend/
├── skinproof/
│   ├── ml_model.py           ← NEW: ML model integration
│   ├── metrics.py            ← UPDATED: Model switching logic
│   └── models/               ← Created by DEPLOY.sh
│       └── skin_analysis_v2.pth
└── train_model/
    ├── DEPLOY.sh             ← Main deployment script
    ├── PRE_DEPLOY_CHECK.sh   ← Pre-deployment validation
    ├── test_consistency.py   ← Consistency testing
    ├── DEPLOYMENT_GUIDE.md   ← Full documentation
    └── checkpoints/
        └── best_model.pth    ← From training
```

### How Model Switching Works

```python
# In metrics.py
def analyze(image_bytes, quality_score, baseline):
    use_ml_model = os.getenv("USE_NEW_MODEL", "0") == "1"
    
    if use_ml_model:
        # Try ML model
        try:
            return analyze_with_ml(image_bytes, quality_score, baseline)
        except Exception:
            # Fall back to deterministic
            logging.warning("ML model failed, using deterministic")
    
    # Use deterministic model
    return deterministic_analyze(...)
```

### Safety Features

1. **Automatic Fallback**: If ML model fails, automatically uses deterministic model
2. **Environment Flag**: Easy enable/disable without code changes
3. **Consistency Testing**: Validates predictions are stable
4. **Graceful Degradation**: Service continues even if ML model unavailable

## Timeline

```
Now (15:55):  ✓ Preparation complete
              → Training in progress (started 15:46)
              
~17:16:       → Training completes
              → Run PRE_DEPLOY_CHECK.sh
              → Run DEPLOY.sh
              → Enable with USE_NEW_MODEL=1
              
~17:20:       ✓ Deployment complete
              ✓ ML model active
```

## Documentation

For complete details, see:
- **DEPLOYMENT_GUIDE.md** - Full deployment documentation
- **test_consistency.py** - Consistency validation details
- **ml_model.py** - Model integration code

## Monitoring

Check which model is active:
```python
import os
print(f"ML Model: {os.getenv('USE_NEW_MODEL', '0') == '1'}")
```

Check in logs:
- `"Loading ML model from..."` - ML model loading
- `"ml-v2.0"` - ML model version in results
- `"deterministic-3.0"` - Deterministic model version

## Support

Issues? Check:
1. **DEPLOYMENT_GUIDE.md** - Troubleshooting section
2. **Logs** - Error messages and fallback warnings
3. **Consistency Test** - Run `python3 test_consistency.py`
4. **Rollback** - Set `USE_NEW_MODEL=0`

## Quick Reference

| Task | Command |
|------|---------|
| Check readiness | `./PRE_DEPLOY_CHECK.sh` |
| Deploy model | `./DEPLOY.sh` |
| Enable ML model | `export USE_NEW_MODEL=1` |
| Test consistency | `python3 test_consistency.py` |
| Disable ML model | `export USE_NEW_MODEL=0` |
| Check model status | `echo $USE_NEW_MODEL` |

---

**Ready to deploy once training completes!** 🚀
