# Railway Deployment Package for GlowUp AI Backend

Everything you need to deploy the GlowUp AI backend to Railway in one package.

## Package Contents

### 📋 Documentation Files

1. **RAILWAY_DEPLOY.md** (Comprehensive Guide)
   - Complete step-by-step deployment guide
   - Environment variable descriptions
   - Troubleshooting section
   - Security checklist
   - Monitoring and maintenance
   - Cost estimation

2. **RAILWAY_QUICKSTART.md** (5-Minute Setup)
   - TL;DR version for quick deployment
   - Copy-paste ready commands
   - Minimal viable configuration

3. **DEPLOYMENT_CHECKLIST.md** (Step-by-Step Verification)
   - Pre-deployment setup checklist
   - Railway configuration checklist
   - Post-deployment verification
   - Security hardening checks
   - Success criteria

4. **ENV_VARS_REFERENCE.md** (Complete Variable Documentation)
   - All environment variables explained
   - Format specifications
   - Common mistakes and fixes
   - Troubleshooting by variable
   - Quick copy-paste templates

### 🛠️ Configuration Files

5. **railway.json** (Railway Build Config)
   - Docker build configuration
   - Health check settings
   - Restart policy
   - Already exists and optimized

6. **.env.production.template** (Environment Variables Template)
   - Template for all required variables
   - Generation instructions for secrets
   - In-line documentation

7. **railway-commands.sh** (Helper Script)
   - Pre-built commands for common operations
   - Secret generation functions
   - Deployment helpers
   - Monitoring commands

### 📦 Existing Project Files

8. **Dockerfile** (Already optimized for Railway)
9. **pyproject.toml** (Python dependencies)
10. **.env.example** (Development reference)

---

## Quick Start (5 Minutes)

### Prerequisites
- Railway account: https://railway.app
- Gemini API key: https://ai.google.dev/

### Steps

1. **Install Railway CLI**
   ```bash
   npm install -g @railway/cli
   railway login
   ```

2. **Initialize Project**
   ```bash
   cd /Users/21cabbage/GlowupAI/backend
   railway init
   ```

3. **Add PostgreSQL**
   - Railway Dashboard -> New -> Database -> PostgreSQL

4. **Generate Secrets**
   ```bash
   source railway-commands.sh
   generate_all_secrets
   ```

5. **Set Environment Variables**
   - Copy template from `.env.production.template`
   - Paste into Railway Dashboard -> Variables
   - Replace placeholders with real values

6. **Create Volume** (for photo storage)
   - Settings -> Volumes -> New Volume
   - Name: `photos`, Mount: `/data/photos`

7. **Deploy**
   ```bash
   railway up
   ```

8. **Verify**
   ```bash
   source railway-commands.sh
   check_health
   ```

---

## File Usage Guide

### For First-Time Deployment
Read in this order:
1. `RAILWAY_QUICKSTART.md` - Get oriented
2. `DEPLOYMENT_CHECKLIST.md` - Follow step-by-step
3. `.env.production.template` - Fill in your values
4. `railway-commands.sh` - Use helper commands
5. `RAILWAY_DEPLOY.md` - Reference when needed

### For Troubleshooting
1. `ENV_VARS_REFERENCE.md` - Variable-specific issues
2. `RAILWAY_DEPLOY.md` - Troubleshooting section
3. `railway-commands.sh` - Debugging commands

### For Team Onboarding
Give new team members:
1. `RAILWAY_QUICKSTART.md` - Quick overview
2. `ENV_VARS_REFERENCE.md` - Variable reference
3. Access to Railway project

---

## Common Tasks

### Deploy for the First Time
```bash
# See DEPLOYMENT_CHECKLIST.md for complete process
```

### Update Environment Variables
```bash
source railway-commands.sh
set_var GLOWUPAI_ALLOWED_ORIGINS "https://new-domain.com"
```

### Check Deployment Status
```bash
source railway-commands.sh
railway_status
check_health
```

### View Logs
```bash
source railway-commands.sh
railway_logs
```

### Rollback Deployment
```bash
source railway-commands.sh
railway_rollback
```

### Generate New Secrets
```bash
source railway-commands.sh
generate_all_secrets
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         Railway                              │
│                                                              │
│  ┌────────────────┐      ┌──────────────┐                  │
│  │   Web Service  │─────▶│  PostgreSQL  │                  │
│  │  (FastAPI App) │      │   Database   │                  │
│  │                │      └──────────────┘                  │
│  │  Port: Auto    │                                         │
│  │  Replicas: 1   │      ┌──────────────┐                  │
│  │                │─────▶│    Volume    │                  │
│  └────────────────┘      │ /data/photos │                  │
│         │                └──────────────┘                  │
│         │                                                   │
│         ▼                                                   │
│  ┌────────────────┐                                        │
│  │ Health Check   │                                        │
│  │ /api/health    │                                        │
│  │ Every 30s      │                                        │
│  └────────────────┘                                        │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                    │
                    ▼
            ┌──────────────┐
            │   Internet   │
            └──────────────┘
                    │
                    ▼
            ┌──────────────┐
            │   Frontend   │
            │ (Your Domain)│
            └──────────────┘
```

---

## Key Features

### ✅ Production-Ready
- Docker-based deployment
- Health checks configured
- Auto-restart on failure
- PostgreSQL with connection pooling
- Encrypted photo storage

### 🔒 Security
- Environment-based secrets
- Firebase authentication
- CORS protection
- Encrypted photo storage
- Admin token protection

### 📊 Monitoring
- Health check endpoint
- Structured logging
- Railway metrics dashboard
- Database connection monitoring

### 🔄 DevOps
- Auto-deploy from GitHub
- One-click rollback
- Environment variables management
- Database migrations on startup

---

## Environment Variables Summary

**Required (7 variables):**
```bash
GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7
GLOWUPAI_ENV=production
GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1
GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com
GEMINI_API_KEY=your_key
GLOWUPAI_ADMIN_TOKEN=your_token
GLOWUPAI_AUTH_REQUIRED=0
```

**Photo Storage (3 variables):**
```bash
GLOWUPAI_PHOTO_DIR=/data/photos
GLOWUPAI_PHOTO_KEY=your_base64_key
GLOWUPAI_RAW_RETENTION_DAYS=730
```

**Auto-Injected by Railway:**
- `DATABASE_URL` (from PostgreSQL service)
- `PORT` (by Railway platform)

See `ENV_VARS_REFERENCE.md` for complete documentation.

---

## Deployment Verification

After deploying, verify these endpoints:

### Health Check
```bash
curl https://your-project.up.railway.app/api/health
```
Expected: `{"status": "ok", "database": "postgres"}`

### Create User
```bash
curl -X POST https://your-project.up.railway.app/api/users \
  -H "Content-Type: application/json" \
  -d '{"skin_type": "combination"}'
```
Expected: User object with `user_id`

### API Documentation
Visit: `https://your-project.up.railway.app/docs`

---

## Cost Estimation

**Railway Hobby Plan:** $5/month + usage

**Estimated Monthly Costs:**
- Web Service (512MB): ~$5-10/month
- PostgreSQL (1GB): ~$5/month
- Volume (1GB): ~$0.25/month

**Total: ~$10-20/month** for MVP/light usage

See `RAILWAY_DEPLOY.md` for detailed cost breakdown.

---

## Troubleshooting Quick Reference

| Symptom | Solution | Doc Reference |
|---------|----------|---------------|
| Health check fails | Check DATABASE_URL | RAILWAY_DEPLOY.md §6.1 |
| CORS errors | Fix GLOWUPAI_ALLOWED_ORIGINS | ENV_VARS_REFERENCE.md |
| 401 errors | Check Firebase config | ENV_VARS_REFERENCE.md |
| Photos disappear | Mount volume at /data/photos | RAILWAY_DEPLOY.md §3.5 |
| Build fails | Check Dockerfile, pyproject.toml | RAILWAY_DEPLOY.md §4 |
| Slow responses | Increase DB pool size | ENV_VARS_REFERENCE.md |

---

## Support & Resources

### Documentation
- **Full Guide:** `RAILWAY_DEPLOY.md`
- **Quick Start:** `RAILWAY_QUICKSTART.md`
- **Variables:** `ENV_VARS_REFERENCE.md`
- **Checklist:** `DEPLOYMENT_CHECKLIST.md`

### Helper Tools
- **Commands:** `source railway-commands.sh && show_help`
- **Template:** `.env.production.template`

### External Resources
- **Railway Docs:** https://docs.railway.app
- **Railway Discord:** https://discord.gg/railway
- **Railway Status:** https://status.railway.app
- **Gemini API:** https://ai.google.dev/

### GlowUp AI Backend Docs
- **Architecture:** `docs/architecture.md`
- **API Map:** `docs/frontend-api-map.md`
- **Operations:** `docs/operations.md`

---

## Next Steps After Deployment

1. **Verify Health**
   - Run health checks
   - Check database migrations
   - Test basic endpoints

2. **Configure Frontend**
   - Update API base URL
   - Test CORS
   - Integrate Firebase auth

3. **Enable Authentication**
   - Verify auth flow works
   - Set `GLOWUPAI_AUTH_REQUIRED=1`

4. **Setup Monitoring**
   - Configure Railway alerts
   - Setup uptime monitoring
   - Review logs regularly

5. **Custom Domain** (Optional)
   - Configure DNS
   - Add domain in Railway
   - Update CORS origins

6. **Performance Tuning**
   - Monitor resource usage
   - Adjust DB pool if needed
   - Consider horizontal scaling

---

## Success Criteria Checklist

Your deployment is successful when:

- [x] Health endpoint returns 200 OK
- [x] Database shows as "postgres" not "sqlite"
- [x] Can create and retrieve users
- [x] CORS works from frontend
- [x] Photos persist after restart (if using volume)
- [x] Gemini features work (shelf scan, Q&A)
- [x] No critical errors in logs
- [x] Service auto-restarts on failure
- [x] Response times < 500ms
- [x] All migrations applied

---

## Getting Help

### Quick Checks
1. Read `RAILWAY_DEPLOY.md` troubleshooting section
2. Check `ENV_VARS_REFERENCE.md` for variable issues
3. Review Railway logs: `railway logs`
4. Test health endpoint: `curl .../api/health`

### If Still Stuck
1. Railway Discord: https://discord.gg/railway
2. Railway Support: https://railway.app/support
3. Check Railway status: https://status.railway.app

### Before Asking for Help
Have ready:
- Railway logs (last 100 lines)
- Environment variables (without secrets)
- Exact error messages
- What you were trying to do
- What you expected vs what happened

---

## Maintenance Schedule

### Daily
- Check Railway dashboard for alerts
- Review error logs

### Weekly
- Review resource usage
- Check backup status
- Update dependencies if needed

### Monthly
- Rotate secrets
- Review and optimize database
- Update documentation
- Review costs vs budget

See `DEPLOYMENT_CHECKLIST.md` for complete maintenance checklist.

---

## Version History

- **v1.0** (2026-08-30) - Initial Railway deployment package
  - Complete documentation suite
  - Helper scripts
  - Environment templates
  - Deployment checklists

---

## License & Usage

This deployment package is part of the GlowUp AI (formerly GlowupAI) backend project.

All documentation and scripts in this package are provided as-is for deploying the GlowUp AI backend to Railway.

---

## Feedback

Found an issue or have a suggestion for improving this deployment package?

- File an issue in the project repository
- Update the documentation directly
- Share your improvements with the team

---

**Ready to deploy? Start with `RAILWAY_QUICKSTART.md`**

**Have questions? Check `RAILWAY_DEPLOY.md`**

**Need variable help? See `ENV_VARS_REFERENCE.md`**

**Want to verify? Use `DEPLOYMENT_CHECKLIST.md`**

**Need commands? Source `railway-commands.sh`**
