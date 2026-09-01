# Face Alignment - Quick Start Guide

## What Changed?

Face alignment is now automatically applied before skin analysis to improve consistency.

## For Developers

### Testing Your Changes

```bash
# Run all tests
cd backend
./venv/bin/python3 -m unittest tests.test_core.CoreTests

# Test just alignment
./venv/bin/python3 test_face_alignment.py

# Test with a real photo
./venv/bin/python3 verify_alignment.py /path/to/photo.jpg
```

### Files You Might Need

- `glowupai/face_alignment.py` - Alignment logic
- `glowupai/metrics.py` - Analysis (calls alignment)
- `tests/test_core.py` - Test suite

### Quick Debug

```python
# Check if alignment is working
from glowupai.face_alignment import align_face_safe

with open('test.jpg', 'rb') as f:
    original = f.read()

aligned = align_face_safe(original)
print(f"Aligned: {len(aligned)} bytes")
```

## For QA

### What to Test

1. **Normal capture flow** - Should work exactly as before
2. **Consistency** - Same face should give similar scores
3. **Edge cases**:
   - Dark lighting
   - Tilted head
   - Side angle
   - No face visible

### Expected Behavior

- ✅ Scores more consistent across captures
- ✅ No error messages for normal photos
- ✅ Still works if no face detected (falls back)
- ✅ Processing time ~50-150ms longer

### What to Report

- Score variance across 3+ photos of same person
- Any new error messages
- Processing time changes

## For DevOps

### Dependencies

Added to `pyproject.toml`:
```
opencv-python>=4.8.0
numpy>=1.24.0
```

Install:
```bash
pip install opencv-python numpy
```

### No Config Changes Needed

Alignment is enabled by default, no environment variables required.

### Monitoring

Watch for:
- Processing time increase (~50-150ms)
- Memory usage (~2MB per request)
- Error rate (should not increase)

### Health Check

```bash
python3 -c "from glowupai.face_alignment import align_face_safe; print('✓ OK')"
```

## For Product/PM

### User Impact

**Before**: Same face could score 31, then 6, then 13 for blemishes  
**After**: Same face consistently scores within ±2 points

### Metrics to Track

- Score variance across repeat captures (should decrease)
- User retention (should improve with consistency)
- Support tickets about "wrong scores" (should decrease)

### No User-Facing Changes

- UI unchanged
- Capture flow unchanged
- Same API endpoints

## Rollback Plan

If issues arise:

```python
# In glowupai/metrics.py, line ~62:
# Comment out alignment:
aligned_bytes = image_bytes  # bypass alignment
# aligned_bytes = align_face_safe(image_bytes, ...)
```

Restart service. Done.

## Questions?

- Read: `FACE_ALIGNMENT_README.md` (detailed docs)
- Read: `ALIGNMENT_IMPLEMENTATION_SUMMARY.md` (what changed)
- Test: `./venv/bin/python3 test_face_alignment.py`
