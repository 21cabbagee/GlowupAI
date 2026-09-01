# 🚀 START HERE - Railway Deployment for GlowUp AI Backend

## You Have Everything You Need!

This directory now contains a complete Railway deployment package. Everything is ready for you to copy-paste and deploy.

---

## 📍 Where to Start

### If you've never deployed to Railway before:
👉 **Read:** `RAILWAY_QUICKSTART.md` (5 minutes)

### If you want the full guide:
👉 **Read:** `RAILWAY_DEPLOY.md` (comprehensive)

### If you want a step-by-step checklist:
👉 **Follow:** `DEPLOYMENT_CHECKLIST.md`

---

## 📦 What You Got

### Documentation (7 files)
1. **START_HERE.md** ← You are here
2. **RAILWAY_README.md** - Package overview
3. **RAILWAY_DEPLOY.md** - Complete deployment guide (13KB)
4. **RAILWAY_QUICKSTART.md** - 5-minute quick start (2.5KB)
5. **DEPLOYMENT_CHECKLIST.md** - Step-by-step verification (11KB)
6. **ENV_VARS_REFERENCE.md** - All variables explained (14KB)
7. **.env.production.template** - Copy-paste template (4.6KB)

### Tools
8. **railway-commands.sh** - Helper commands (9.9KB, executable)

### Config (already exists)
9. **railway.json** - Railway configuration ✅
10. **Dockerfile** - Docker build config ✅

---

## ⚡ Quick Deploy (Copy-Paste Ready)

### 1. Install Railway CLI
```bash
npm install -g @railway/cli
railway login
```

### 2. Go to Backend Directory
```bash
cd /Users/21cabbage/GlowupAI/backend
```

### 3. Initialize Railway Project
```bash
railway init
```

### 4. Add PostgreSQL
- Open Railway Dashboard
- Click "New" → "Database" → "Add PostgreSQL"

### 5. Generate Secrets
```bash
# Admin token
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Photo encryption key
python3 -c "import secrets, base64; print(base64.b64encode(secrets.token_bytes(32)).decode())"
```

### 6. Set Environment Variables
Copy this into Railway Dashboard → Variables:
```bash
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_ENV=production
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com
GEMINI_API_KEY=your_gemini_key_here
GLOWUPAI_ADMIN_TOKEN=paste_generated_token_here
GLOWUPAI_AUTH_REQUIRED=0
GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=paste_generated_key_here
GLOWUPAI_RAW_RETENTION_DAYS=730
```

### 7. Create Volume for Photos
- Railway Dashboard → Your Service → Settings → Volumes
- Click "New Volume"
- Name: `photos`
- Mount path: `/data/photos`

### 8. Deploy
```bash
railway up
```

### 9. Verify
```bash
# Get your Railway URL from the dashboard, then:
curl https://your-project.up.railway.app/api/health
```

Expected response:
```json
{"status": "ok", "database": "postgres", "database_ready": true}
```

---

## 🎯 What You Need

### Before Starting
- [ ] Railway account (https://railway.app)
- [ ] Gemini API key (https://ai.google.dev/)
- [ ] Your production domain name

### During Setup
- [ ] 10 minutes of focused time
- [ ] Terminal access
- [ ] Browser for Railway Dashboard

---

## 📚 File Guide

### Read These First
1. **RAILWAY_QUICKSTART.md** - Fast track to deployment
2. **DEPLOYMENT_CHECKLIST.md** - Follow step-by-step

### Reference When Needed
3. **RAILWAY_DEPLOY.md** - Deep dive on any topic
4. **ENV_VARS_REFERENCE.md** - Variable documentation

### Use These Tools
5. **railway-commands.sh** - Helper functions
   ```bash
   source railway-commands.sh
   show_help
   ```

6. **.env.production.template** - Variable template
   - Fill in your values
   - Paste into Railway Dashboard

---

## 🛠️ Helper Commands

### Load Helper Functions
```bash
cd /Users/21cabbage/GlowupAI/backend
source railway-commands.sh
```

### Common Commands
```bash
generate_all_secrets    # Generate required secrets
railway_deploy          # Deploy to Railway
railway_logs           # View logs
check_health           # Test health endpoint
railway_status         # Check deployment status
show_help              # See all commands
```

---

## ✅ Success Checklist

After deployment, verify:
- [ ] Health endpoint returns 200 OK
- [ ] Response includes `"database": "postgres"`
- [ ] Can create a test user
- [ ] CORS works from your frontend
- [ ] No errors in Railway logs

---

## 🆘 Need Help?

### Quick Fixes
| Problem | Solution | Doc |
|---------|----------|-----|
| Health check fails | Check DATABASE_URL exists | RAILWAY_DEPLOY.md |
| CORS errors | Fix GLOWUPAI_ALLOWED_ORIGINS | ENV_VARS_REFERENCE.md |
| Photos disappear | Mount volume at /data/photos | RAILWAY_DEPLOY.md §3.5 |
| 401 errors | Check GLOWUPAI_AUTH_REQUIRED | ENV_VARS_REFERENCE.md |

### Still Stuck?
1. Check `RAILWAY_DEPLOY.md` troubleshooting section
2. Review `ENV_VARS_REFERENCE.md` for variable issues
3. Railway Discord: https://discord.gg/railway

---

## 💰 Cost Estimate

**~$10-20/month** for MVP/light usage

Breakdown:
- Web Service: ~$5-10/month
- PostgreSQL: ~$5/month
- Volume: ~$0.25/month

See `RAILWAY_DEPLOY.md` for detailed cost info.

---

## 🎉 What's Next?

### After Successful Deployment
1. Update frontend API URL
2. Test end-to-end flow
3. Enable authentication (`GLOWUPAI_AUTH_REQUIRED=1`)
4. Add custom domain (optional)
5. Setup monitoring

### Ongoing
- Monitor Railway dashboard daily
- Review logs weekly
- Rotate secrets monthly

See `DEPLOYMENT_CHECKLIST.md` for maintenance schedule.

---

## 📊 Your Application

**Name:** GlowUp AI Backend (formerly GlowupAI)

**Tech Stack:**
- FastAPI (Python web framework)
- PostgreSQL (database)
- Google Gemini (AI features)
- Firebase (authentication)

**Key Features:**
- Health endpoint: `/api/health`
- API docs: `/docs` (auto-generated)
- Photo storage: Encrypted at rest
- Database: Auto-migrates on startup

---

## 🔐 Security Notes

**Required in Production:**
- `GLOWUPAI_ENV=production` (exactly)
- `GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1`
- Strong `GLOWUPAI_ADMIN_TOKEN` (32+ chars)
- Proper `GLOWUPAI_PHOTO_KEY` (base64 32 bytes)
- No secrets in Git

See `DEPLOYMENT_CHECKLIST.md` security section.

---

## 📁 Project Structure

```
/Users/21cabbage/GlowupAI/backend/
├── START_HERE.md                    ← You are here
├── RAILWAY_README.md                ← Package overview
├── RAILWAY_DEPLOY.md                ← Full deployment guide
├── RAILWAY_QUICKSTART.md            ← 5-minute quick start
├── DEPLOYMENT_CHECKLIST.md          ← Step-by-step checklist
├── ENV_VARS_REFERENCE.md            ← Variable documentation
├── .env.production.template         ← Variable template
├── railway-commands.sh              ← Helper commands
├── railway.json                     ← Railway config (existing)
├── Dockerfile                       ← Docker config (existing)
├── pyproject.toml                   ← Python deps (existing)
├── .env.example                     ← Dev reference (existing)
└── glowupai/                       ← App code (existing)
    ├── complete_api.py              ← Main API
    ├── config.py                    ← Settings
    └── migrations/                  ← Database migrations
```

---

## 🚦 Deployment Status

**Pre-Deployment:** Ready to deploy ✅
- All files created
- Configuration optimized
- Documentation complete
- Helper scripts ready

**What's Left:** You need to:
1. Get Gemini API key
2. Set up Railway project
3. Fill in environment variables
4. Deploy!

---

## 💡 Pro Tips

1. **Start Simple**
   - Deploy with minimal config first
   - Add features incrementally
   - Test each step

2. **Use Helper Script**
   - `source railway-commands.sh`
   - Saves typing, reduces errors
   - Has built-in validation

3. **Check Health Often**
   - After every config change
   - Use `check_health` command
   - Monitor Railway dashboard

4. **Keep Secrets Safe**
   - Never commit `.env.production` to Git
   - Use Railway variables only
   - Rotate regularly

5. **Read Error Messages**
   - Railway logs are detailed
   - Health endpoint shows DB status
   - Check both before asking for help

---

## 📞 Support Resources

**Railway:**
- Docs: https://docs.railway.app
- Discord: https://discord.gg/railway
- Status: https://status.railway.app

**Gemini API:**
- Get Key: https://ai.google.dev/
- Docs: https://ai.google.dev/docs

**This Project:**
- Full Docs: `RAILWAY_DEPLOY.md`
- Variables: `ENV_VARS_REFERENCE.md`
- Checklist: `DEPLOYMENT_CHECKLIST.md`

---

## ✨ Ready?

### Fastest Path to Production:
1. Open `RAILWAY_QUICKSTART.md`
2. Follow the 9 steps
3. Deploy!

### Most Thorough Path:
1. Open `DEPLOYMENT_CHECKLIST.md`
2. Check off each item
3. Verify success criteria

### Need Context First?
1. Read `RAILWAY_README.md`
2. Understand architecture
3. Choose your path

---

## 🎯 Your Mission

**Goal:** Deploy GlowUp AI backend to Railway

**Time Required:** 10-15 minutes

**Difficulty:** Easy (copy-paste ready)

**Outcome:** Production-ready API with health checks, database, and photo storage

---

**🚀 Let's Deploy! Open `RAILWAY_QUICKSTART.md` to begin.**

---

_Last Updated: 2026-08-30_
_Package Version: 1.0_
_Backend Version: 3.0.0_
