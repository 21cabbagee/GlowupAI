# Render Deployment - Quick Checklist

**Print this or keep it on a second screen while deploying**

---

## Pre-Deployment

- [ ] GitHub repo is up to date: https://github.com/piyushxpc7/GlowupAI
- [ ] Have your Gemini API key ready: https://aistudio.google.com/apikey
- [ ] Render account exists or ready to sign up with GitHub

---

## Deployment Steps (10 min)

### 1. Sign Up (2 min)
- [ ] Go to https://render.com
- [ ] Click "Sign up with GitHub"
- [ ] Authorize Render

### 2. Create Blueprint (5 min)
- [ ] Click "New +" > "Blueprint"
- [ ] Select "piyushxpc7/GlowupAI" repo
- [ ] Click "Connect"
- [ ] Review: Should show web service + database
- [ ] Add **GEMINI_API_KEY** in environment variables
- [ ] Click "Apply"

### 3. Wait for Build (3-5 min)
- [ ] Watch build logs
- [ ] Wait for "Live" status

### 4. Test Deployment (2 min)
- [ ] Copy your URL: `https://glowupai-backend-XXXX.onrender.com`
- [ ] Test health: Add `/api/health` to URL
- [ ] Should see: `{"status": "ok", ...}`
- [ ] Test docs: Add `/docs` to URL

---

## Required Environment Variables

**Must Set:**
```
GEMINI_API_KEY=your_actual_key_here
```

**Recommended:**
```
SKINPROOF_ALLOWED_ORIGINS=https://your-frontend-domain.com
SKINPROOF_AUTH_REQUIRED=0
SKINPROOF_DISABLE_LEGACY_KEY_FILE=1
```

---

## Your URLs After Deployment

```
Base URL:    https://glowupai-backend-XXXX.onrender.com
Health:      https://glowupai-backend-XXXX.onrender.com/api/health
API Docs:    https://glowupai-backend-XXXX.onrender.com/docs
```

---

## Common Issues

| Problem | Quick Fix |
|---------|-----------|
| Build fails | Check logs, verify Dockerfile exists |
| Service crashes | Add GEMINI_API_KEY |
| 502 Error | Wait 2 min, service still starting |
| CORS errors | Add frontend to ALLOWED_ORIGINS |
| Slow first request | Normal - free tier spins down after 15 min |

---

## Success Indicators

- [ ] Dashboard shows green "Live" indicator
- [ ] `/api/health` returns 200 OK
- [ ] `/docs` shows Swagger UI
- [ ] No errors in logs tab
- [ ] Can make API requests from frontend

---

## Post-Deployment

- [ ] Save your deployment URL
- [ ] Update frontend to use new backend URL
- [ ] Test one full user flow
- [ ] Set up email notifications for deploy status
- [ ] Monitor logs for first 24 hours

---

**Need help?** See full guide: `RENDER_DEPLOYMENT_STEPS.md`

**Render Dashboard**: https://dashboard.render.com
