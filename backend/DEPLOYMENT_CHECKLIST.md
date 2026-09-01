# GlowUp AI Backend - Deployment Checklist

Use this as a step-by-step verification tool during deployment.

## Pre-Deployment Setup

### Railway Account & Project
- [ ] Railway account created at https://railway.app
- [ ] Railway CLI installed: `npm install -g @railway/cli`
- [ ] Logged into Railway CLI: `railway login`
- [ ] New Railway project created: `railway init`

### API Keys & Secrets
- [ ] Gemini API key obtained from https://ai.google.dev/
- [ ] Admin token generated (32+ characters random)
- [ ] Photo encryption key generated (base64-encoded 32 bytes)
- [ ] Firebase project ID confirmed: `glowup-ai-38ae7`

### Infrastructure Decisions
- [ ] Photo storage strategy chosen:
  - [ ] Railway Volume (recommended for MVP)
  - [ ] In-memory (dev only)
  - [ ] S3/GCS (future)
- [ ] Domain strategy decided:
  - [ ] Railway subdomain (free)
  - [ ] Custom domain (requires DNS setup)

---

## Railway Configuration

### Database
- [ ] PostgreSQL added to Railway project
- [ ] DATABASE_URL automatically created (verify in Variables tab)
- [ ] Database status shows "Healthy"

### Storage
If using Railway Volume:
- [ ] Volume created with name: `photos`
- [ ] Volume mount path set to: `/data/photos`
- [ ] Volume size configured (start with 1GB, can scale later)

### Environment Variables
Copy this into Railway Dashboard -> Variables:

#### Core Required (4 variables)
- [ ] `GLOWUPAI_FIREBASE_PROJECT_ID=glowup-ai-38ae7`
- [ ] `GLOWUPAI_ENV=production`
- [ ] `GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1`
- [ ] `GLOWUPAI_ALLOWED_ORIGINS=https://your-domain.com`

#### API Keys (2 variables)
- [ ] `GEMINI_API_KEY=<your-key>`
- [ ] `GLOWUPAI_ADMIN_TOKEN=<your-token>`

#### Authentication (1 variable)
- [ ] `GLOWUPAI_AUTH_REQUIRED=0` (set to 1 after frontend testing)

#### Photo Storage (3 variables, if using volume)
- [ ] `GLOWUPAI_PHOTO_DIR=/data/photos`
- [ ] `GLOWUPAI_PHOTO_KEY=<your-base64-key>`
- [ ] `GLOWUPAI_RAW_RETENTION_DAYS=730`

#### Optional Tuning (3 variables - has good defaults)
- [ ] `GLOWUPAI_GEMINI_MODEL=gemini-3.5-flash-lite`
- [ ] `GLOWUPAI_GEMINI_ENABLED=1`
- [ ] `GLOWUPAI_MODEL_VERSION=deterministic-3.0`

**Total Variables to Set: 13-14** (depending on photo storage choice)

---

## Deployment

### Code Deployment
Choose one method:

**Method A: CLI Deploy**
- [ ] `cd /Users/21cabbage/GlowupAI/backend`
- [ ] `railway up`
- [ ] Watch build logs for errors

**Method B: GitHub Auto-Deploy (Recommended)**
- [ ] Code pushed to GitHub repository
- [ ] Railway connected to GitHub repo (Settings -> Source)
- [ ] Auto-deploy enabled for main branch
- [ ] GitHub integration shows "Connected"

### Build Verification
- [ ] Build phase completed successfully
- [ ] Docker image built without errors
- [ ] Dependencies installed (check logs)
- [ ] Service started with uvicorn

---

## Post-Deployment Verification

### Health Check
- [ ] Service shows "Active" in Railway dashboard
- [ ] Public URL accessible (click service to see URL)
- [ ] Health endpoint responds: `curl https://your-project.up.railway.app/api/health`
- [ ] Response includes `"status": "ok"`
- [ ] Response includes `"database": "postgres"`
- [ ] Response includes `"database_ready": true`

### Database Migrations
Check deployment logs for:
- [ ] "Running PostgreSQL migrations..." message
- [ ] All 4 migration files applied:
  - [ ] 0001_initial.sql
  - [ ] 0002_growth_features.sql
  - [ ] 0003_checkins_measurement_feedback.sql
  - [ ] 0004_firebase_identity.sql
- [ ] No migration errors in logs

### API Functionality
Test basic endpoints:

**Create User:**
```bash
curl -X POST https://your-project.up.railway.app/api/users \
  -H "Content-Type: application/json" \
  -d '{"skin_type": "combination"}'
```
- [ ] Returns 200 OK
- [ ] Returns user object with `user_id`

**Get User:**
```bash
curl https://your-project.up.railway.app/api/users/{user_id}
```
- [ ] Returns 200 OK
- [ ] Returns user data

### CORS Testing
From your frontend domain:
```javascript
fetch('https://your-project.up.railway.app/api/health')
  .then(r => r.json())
  .then(console.log)
```
- [ ] No CORS errors in browser console
- [ ] Response received successfully

### Photo Storage (if using volume)
- [ ] Volume shows as mounted in Railway dashboard
- [ ] Upload test photo via API (POST /api/captures)
- [ ] Restart service (redeploy)
- [ ] Verify photo still accessible (GET /api/captures/{id}/thumbnail)

---

## Security Hardening

### Configuration Review
- [ ] `GLOWUPAI_ENV=production` is set (not development)
- [ ] `GLOWUPAI_DISABLE_LEGACY_KEY_FILE=1` is set
- [ ] `GLOWUPAI_ADMIN_TOKEN` is NOT a weak/test value
- [ ] `GLOWUPAI_PHOTO_KEY` is proper base64-encoded 32 bytes
- [ ] `GEMINI_API_KEY` is not visible in logs
- [ ] No secrets committed to Git (check .env is in .gitignore)

### CORS Configuration
- [ ] `GLOWUPAI_ALLOWED_ORIGINS` only includes production domains
- [ ] No wildcard (*) origins in production
- [ ] Both www and non-www versions added if needed

### Authentication
- [ ] Firebase project ID matches your project
- [ ] Frontend configured to send Authorization headers
- [ ] Test authentication flow end-to-end
- [ ] Once confirmed working, set `GLOWUPAI_AUTH_REQUIRED=1`

---

## Monitoring Setup

### Railway Monitoring
- [ ] Deployment notifications enabled
- [ ] Error alerts configured
- [ ] Resource usage baseline recorded (CPU, RAM)

### Database
- [ ] Backup schedule configured in Railway
- [ ] Backup retention policy set (at least 7 days)
- [ ] Test database restore procedure documented

### Logging
- [ ] Log retention configured in Railway
- [ ] Critical error patterns identified
- [ ] Log aggregation set up (optional: Datadog, LogDNA)

---

## Domain & SSL (Optional)

### Custom Domain Setup
- [ ] Domain purchased and DNS accessible
- [ ] CNAME record created pointing to Railway URL
- [ ] Custom domain added in Railway (Settings -> Domains)
- [ ] SSL certificate auto-provisioned (wait 5-10 minutes)
- [ ] HTTPS works at custom domain
- [ ] HTTP auto-redirects to HTTPS

### DNS Records
```
Type: CNAME
Name: api (or your subdomain)
Value: your-project.up.railway.app
TTL: 300
```
- [ ] DNS record propagated (check with `dig` or `nslookup`)
- [ ] SSL certificate shows as valid in browser

### Post-Domain Configuration
- [ ] Update `GLOWUPAI_ALLOWED_ORIGINS` with custom domain
- [ ] Update frontend API base URL
- [ ] Test end-to-end flow with custom domain

---

## Performance Baseline

### Initial Metrics
Record these for future comparison:
- [ ] Cold start time: _____ seconds
- [ ] Health check response time: _____ ms
- [ ] User creation response time: _____ ms
- [ ] Database query avg time: _____ ms
- [ ] Memory usage at idle: _____ MB
- [ ] CPU usage at idle: _____ %

### Load Testing (Optional)
- [ ] Run basic load test (e.g., Apache Bench, k6)
- [ ] Identify bottlenecks
- [ ] Adjust resource allocation if needed

---

## Documentation & Handoff

### Team Documentation
- [ ] Railway project URL shared with team
- [ ] Environment variables documented (without secrets)
- [ ] Deployment procedure documented
- [ ] Rollback procedure tested and documented
- [ ] On-call runbook created

### Access Management
- [ ] Team members invited to Railway project
- [ ] Access levels assigned appropriately
- [ ] Admin credentials stored in password manager
- [ ] Emergency access procedures documented

---

## Post-Launch Monitoring (First 24 Hours)

### Hour 1
- [ ] Health checks passing consistently
- [ ] No error spikes in logs
- [ ] Database connections stable
- [ ] Photo uploads working (if applicable)

### Hour 4
- [ ] Memory usage stable (not growing)
- [ ] No connection pool exhaustion
- [ ] Response times within acceptable range
- [ ] No unusual error patterns

### Hour 24
- [ ] All features functioning as expected
- [ ] No data loss or corruption
- [ ] Backups completing successfully
- [ ] Monitoring alerts working correctly

---

## Success Criteria

Your deployment is production-ready when ALL of these are true:

- [ ] Health endpoint returns 200 with `database: "postgres"`
- [ ] All 4 database migrations applied successfully
- [ ] Can create, read, update, delete users
- [ ] CORS allows requests from production frontend
- [ ] Photos persist after service restart (if using volume)
- [ ] Gemini API features work (shelf scan, Q&A)
- [ ] Firebase authentication validates tokens
- [ ] No critical errors in logs for 1 hour
- [ ] Response times acceptable (<500ms for most endpoints)
- [ ] Resource usage within Railway plan limits

---

## Rollback Plan

If issues arise, follow this procedure:

### Immediate Rollback
1. [ ] Identify last working deployment ID
2. [ ] Click "..." menu on that deployment
3. [ ] Select "Redeploy"
4. [ ] Verify health checks pass
5. [ ] Notify team

### Investigate Issues
1. [ ] Export logs for failed deployment
2. [ ] Review environment variable changes
3. [ ] Check for database migration issues
4. [ ] Review code changes between deployments
5. [ ] Document root cause

### Emergency Shutdown
If rollback fails:
1. [ ] Run `railway down` in CLI
2. [ ] Or disable service in Railway dashboard
3. [ ] Assess situation without production pressure
4. [ ] Plan recovery strategy

---

## Cost Tracking

### Initial Estimate
- Web service (512MB RAM): $____ /month
- PostgreSQL (1GB): $____ /month  
- Volume storage (1GB): $____ /month
- **Total estimate:** $____ /month

### Actual Usage (Update after 30 days)
- Web service: $____ /month
- PostgreSQL: $____ /month
- Volume storage: $____ /month
- **Total actual:** $____ /month

### Cost Alerts
- [ ] Railway budget alert configured
- [ ] Alert threshold set at: $____ /month
- [ ] Alert notification recipients configured

---

## Maintenance Schedule

### Daily
- [ ] Check Railway dashboard for alerts
- [ ] Review error logs for critical issues
- [ ] Verify health checks passing

### Weekly
- [ ] Review resource usage trends
- [ ] Check backup completion
- [ ] Review security alerts
- [ ] Update dependencies if needed

### Monthly
- [ ] Review and rotate secrets
- [ ] Audit environment variables
- [ ] Review and optimize database
- [ ] Update deployment documentation
- [ ] Review cost vs budget

---

## Emergency Contacts

**Railway Support:**
- Dashboard: https://railway.app/support
- Discord: https://discord.gg/railway
- Email: team@railway.app

**On-Call Engineer:**
- Name: _______________
- Phone: _______________
- Slack: @_______________

**Backup Contact:**
- Name: _______________
- Phone: _______________
- Slack: @_______________

---

## Notes & Issues

Use this section to track any deployment-specific notes:

**Deployment Date:** _______________

**Deployed By:** _______________

**Railway Project ID:** _______________

**Production URL:** _______________

**Custom Domain:** _______________

**Issues Encountered:**
- 
- 
- 

**Resolutions:**
- 
- 
- 

**Follow-up Items:**
- [ ] 
- [ ] 
- [ ] 

---

**Status:** [ ] Pre-Deployment [ ] In Progress [ ] Deployed [ ] Production-Ready

**Deployment Completed:** _______________

**Sign-off:** _______________
