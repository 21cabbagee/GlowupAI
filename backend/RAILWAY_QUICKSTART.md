# Railway Quick Deploy - 5 Minute Setup

This is the TL;DR version. For full details, see [RAILWAY_DEPLOY.md](./RAILWAY_DEPLOY.md).

## Prerequisites
- Railway account: https://railway.app
- Gemini API key: https://ai.google.dev/

## Deploy Steps (Copy-Paste Ready)

### 1. Create Railway Project
```bash
# Install Railway CLI
npm install -g @railway/cli

# Login and initialize
railway login
cd /Users/21cabbage/Skinproof/backend
railway init
```

### 2. Add PostgreSQL
In Railway Dashboard:
- Click "New" -> "Database" -> "Add PostgreSQL"
- Wait 30 seconds for provisioning

### 3. Set Environment Variables
Go to your service -> Settings -> Variables, and paste:

```bash
SKINPROOF_FIREBASE_PROJECT_ID=glowup-ai-38ae7
SKINPROOF_ENV=production
SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
GEMINI_API_KEY=YOUR_GEMINI_KEY_HERE
SKINPROOF_ALLOWED_ORIGINS=https://your-domain.com
SKINPROOF_AUTH_REQUIRED=0
SKINPROOF_ADMIN_TOKEN=GENERATE_RANDOM_TOKEN
SKINPROOF_PHOTO_DIR=/data/photos
SKINPROOF_PHOTO_KEY=GENERATE_BASE64_KEY
SKINPROOF_RAW_RETENTION_DAYS=730
```

### 4. Generate Required Secrets

**Admin Token:**
```bash
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

**Photo Encryption Key:**
```bash
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

### 5. Create Railway Volume
In Railway Dashboard:
- Your service -> Settings -> Volumes -> "New Volume"
- Name: `photos`
- Mount path: `/data/photos`

### 6. Deploy
```bash
railway up
```

Or connect to GitHub for auto-deployment:
- Settings -> Connect to GitHub repository

### 7. Verify
Get your Railway URL from dashboard, then test:
```bash
curl https://your-project.up.railway.app/api/health
```

Expected response:
```json
{
  "status": "ok",
  "database": "postgres",
  "database_ready": true,
  "version": "3.0.0"
}
```

## Done!

Your backend is now live at: `https://your-project.up.railway.app`

**Next Steps:**
1. Update your frontend to use this URL
2. Test CORS from frontend
3. Once verified, set `SKINPROOF_AUTH_REQUIRED=1`
4. Optional: Add custom domain in Railway settings

## Troubleshooting

| Issue | Fix |
|-------|-----|
| Health check fails | Check DATABASE_URL exists and PostgreSQL is running |
| CORS errors | Update SKINPROOF_ALLOWED_ORIGINS with your actual domain |
| Photos disappear | Verify volume is mounted at /data/photos |
| Build fails | Check logs with `railway logs` |

## Full Documentation
See [RAILWAY_DEPLOY.md](./RAILWAY_DEPLOY.md) for comprehensive guide.
