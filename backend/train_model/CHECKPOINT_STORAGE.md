# 🗄️ ML Checkpoint External Storage Guide

## Current Setup

**Best Model on GitHub**: ✅ `best_model.pth` (36 MB)  
**All Checkpoints Locally**: 📦 182 MB total (5 files)

---

## 🎯 Option 4: External Storage for Full Checkpoints

### Recommended: Hugging Face Hub (Free, ML-Optimized)

**Why Hugging Face?**
- ✅ Free unlimited public models
- ✅ Built for ML models
- ✅ Fast CDN download
- ✅ Version control included
- ✅ Easy sharing with team
- ✅ Industry standard

#### Setup (5 minutes)

```bash
# 1. Install huggingface-cli
pip install huggingface_hub

# 2. Create account and get token
# Visit: https://huggingface.co/settings/tokens

# 3. Login
huggingface-cli login

# 4. Upload checkpoints
python3 upload_to_huggingface.py
```

---

### Alternative: Google Drive (Simple, No Setup)

**For quick sharing:**

```bash
# Manual: Just upload to Google Drive and share link
# - Create folder: "GlowupAI ML Checkpoints"
# - Upload all .pth files
- Set sharing: "Anyone with link can view"
# - Document link below
```

**Google Drive Link**: `[PASTE YOUR LINK HERE]`

---

### Alternative: AWS S3 / Google Cloud Storage (Production Scale)

**For production deployment:**

```bash
# AWS S3
aws s3 cp backend/train_model/checkpoints/ s3://glowupai-ml-models/ --recursive

# Google Cloud Storage
gsutil -m cp -r backend/train_model/checkpoints/ gs://glowupai-ml-models/
```

---

## 📦 Current Checkpoints

| Checkpoint | Size | Epoch | Val Loss | Status |
|-----------|------|-------|----------|--------|
| `best_model.pth` | 36 MB | 15 | 0.7547 | ✅ On GitHub |
| `checkpoint_epoch_10.pth` | 36 MB | 10 | - | 📦 Local only |
| `checkpoint_epoch_20.pth` | 36 MB | 20 | - | 📦 Local only |
| `checkpoint_epoch_30.pth` | 36 MB | 30 | - | 📦 Local only |
| `checkpoint_epoch_40.pth` | 36 MB | 40 | - | 📦 Local only |
| `checkpoint_epoch_50.pth` | 36 MB | 50 | TBD | 🔥 Training... |

**Total**: 216 MB (when training completes)

---

## 🚀 Quick Actions

### Download Best Model (For Cofounder)

```bash
# From GitHub
cd backend/train_model/checkpoints/
git pull origin staging  # Gets best_model.pth automatically
```

### Download All Checkpoints (From External Storage)

```bash
# From Hugging Face (after setup)
python3 download_from_huggingface.py

# From Google Drive (manual)
# 1. Open link above
# 2. Download all files
# 3. Place in backend/train_model/checkpoints/
```

---

## 📝 Backup Strategy

1. **GitHub**: `best_model.pth` only (production model)
2. **External Storage**: All checkpoints (full backup)
3. **Local**: Keep all until uploaded to external storage

**Current Status**:
- ✅ GitHub: best_model.pth pushed
- ⏳ External: Waiting for your preferred platform
- ✅ Local: All 5 checkpoints available

---

## 🔄 Auto-Sync Script

Run this after training completes to sync everything:

```bash
./sync_checkpoints.sh
```

This will:
1. ✅ Push best_model.pth to GitHub (if updated)
2. 📦 Upload all checkpoints to external storage
3. 🧹 Clean up old checkpoints (keep last 5)
4. 📊 Generate backup report

---

**Next**: Choose your external storage platform and run the setup!
