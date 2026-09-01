# Data Collection Pipeline - Quick Start

**🚀 Get started in 5 minutes**

## What This Does

Builds a complete system to improve your ML model by:
- ✅ Collecting anonymized user data (with consent)
- ✅ Gathering feedback on model accuracy
- ✅ Monitoring model health in production
- ✅ Automatically triggering retraining when needed

## Setup

### 1. Run Setup Script

```bash
cd /Users/21cabbage/GlowupAI/backend
python scripts/setup_data_collection.py
```

This will:
- Create database tables
- Verify installation
- Create test data (optional)

### 2. Configure Environment

Add to `.env`:

```bash
# Required
DATA_COLLECTION_SALT="your-secret-salt-change-in-production"

# Optional (for alerts)
SMTP_HOST="smtp.gmail.com"
SMTP_USER="alerts@glowupai.com"
SMTP_PASSWORD="your-password"
ALERT_EMAIL="team@glowupai.com"
SLACK_WEBHOOK_URL="https://hooks.slack.com/..."
```

### 3. Deploy Android Changes

1. Add DataConsentScreen to onboarding flow
2. Show FeedbackDialog after each capture
3. Call consent API: `POST /api/users/{id}/consent/data-collection`
4. Call feedback API: `POST /api/captures/{id}/feedback`

## Quick Test

```python
from skinproof.db import Database
from skinproof.data_collection import DataCollector
from skinproof.feedback import FeedbackCollector
from skinproof.ml_monitoring import ModelMonitor

# Initialize
db = Database()
collector = DataCollector(db)
feedback = FeedbackCollector(db)
monitor = ModelMonitor(db)

# Test data collection
stats = collector.get_collection_stats()
print(f"Collected samples: {stats['total_samples']}")

# Test feedback
feedback_stats = feedback.get_feedback_stats()
print(f"Total feedback: {feedback_stats['total_feedback']}")

# Test monitoring
health = monitor.get_health_status()
print(f"Model health: {health['status']}")
```

## API Endpoints

### User Endpoints

```bash
# Record data collection consent
POST /api/users/{user_id}/consent/data-collection
{
  "granted": true,
  "policy_version": "1.0"
}

# Submit feedback on a capture
POST /api/captures/{capture_id}/feedback
{
  "feedback_type": "inaccurate",
  "issues": ["blemishes_too_high"],
  "corrections": {"blemish_count": 8},
  "comment": "Seems a bit high"
}
```

### Admin Endpoints

```bash
# Get feedback statistics
GET /api/admin/feedback

# Get model health status
GET /api/admin/monitoring

# Get data collection stats
GET /api/admin/data-collection/stats

# Export training dataset
POST /api/admin/data-collection/export
{
  "output_dir": "/path/to/dataset",
  "min_quality": 0.75,
  "max_samples": 10000
}

# Cleanup old data
POST /api/admin/data-collection/cleanup
{
  "retention_days": 365
}
```

## Automated Tasks

### Cron Jobs

```bash
# Check model health every hour
0 * * * * cd /path/to/backend && python -c "from skinproof.ml_monitoring import ModelMonitor; from skinproof.db import Database; ModelMonitor(Database()).check_and_alert()"

# Cleanup old data weekly
0 2 * * 0 cd /path/to/backend && python -c "from skinproof.data_collection import DataCollector; from skinproof.db import Database; DataCollector(Database()).cleanup_old_data(365)"
```

## Files Created

### Backend
- ✅ `skinproof/data_collection.py` - Data collection module
- ✅ `skinproof/feedback.py` - Feedback collection module
- ✅ `skinproof/ml_monitoring.py` - Model monitoring module
- ✅ `skinproof/migrations/003_data_collection_feedback.sql` - Database migration
- ✅ Admin API endpoints in `complete_api.py`
- ✅ Service methods in `complete_service.py`

### Android
- ✅ `FeedbackDialog.kt` - Feedback UI component
- ✅ `DataConsentScreen.kt` - Consent screen

### Documentation
- ✅ `DATA_COLLECTION_POLICY.md` - Privacy policy
- ✅ `DATA_COLLECTION_README.md` - Full implementation guide
- ✅ `setup_data_collection.py` - Setup script

## Key Features

### 1. Privacy-First Design
- **Anonymization**: User IDs hashed one-way
- **Consent required**: Opt-in only
- **Easy opt-out**: Revoke anytime
- **Auto-deletion**: Data deleted after 1 year
- **GDPR/CCPA compliant**

### 2. Quality Controls
- **Automatic filtering**: Only high-quality captures
- **Blur detection**: Reject blurry images
- **Face detection**: Must have face present
- **Lighting checks**: Must meet brightness requirements

### 3. Model Health Monitoring
- **Variance tracking**: Detect prediction instability
- **Drift detection**: Spot distribution changes
- **Error rate monitoring**: Track failures
- **Automatic alerts**: Email/Slack notifications

### 4. Feedback Loop
- **User feedback**: Thumbs up/down after each capture
- **Issue identification**: Specific problems reported
- **User corrections**: Collect better labels
- **Retraining triggers**: Auto-suggest when to retrain

## Usage Examples

### Example 1: Export Training Data

```python
from skinproof.data_collection import DataCollector
from skinproof.db import Database

collector = DataCollector(Database())

# Export high-quality samples for training
stats = collector.export_training_dataset(
    output_dir="/mnt/training/dataset_2026_09",
    min_quality=0.75,
    max_samples=10000
)

print(f"✅ Exported {stats['total_samples']} samples")
print(f"   Train: {stats['train_samples']}")
print(f"   Val: {stats['val_samples']}")
```

### Example 2: Check Model Health

```python
from skinproof.ml_monitoring import ModelMonitor
from skinproof.db import Database

monitor = ModelMonitor(Database())

health = monitor.get_health_status()

if health['status'] != 'healthy':
    print(f"⚠️  Model health issue: {health['status']}")
    print(f"Issues: {health['issues']}")
    
    # Send alert
    monitor.check_and_alert()
```

### Example 3: Analyze Feedback

```python
from skinproof.feedback import FeedbackCollector
from skinproof.db import Database

feedback = FeedbackCollector(Database())

# Get accuracy analysis
analysis = feedback.get_metric_accuracy_analysis()

for metric, data in analysis.items():
    if data['total_issues'] > 50:
        print(f"⚠️  {metric} has {data['total_issues']} issues")
        print(f"   Bias: {data['bias']}")
```

### Example 4: Check Retraining Trigger

```python
from skinproof.feedback import FeedbackCollector
from skinproof.db import Database

feedback = FeedbackCollector(Database())

should_retrain, reason = feedback.should_trigger_retraining()

if should_retrain:
    print(f"🔄 Time to retrain! {reason}")
    
    # Export corrections for training
    corrections = feedback.get_pending_corrections(limit=1000)
    print(f"   Using {len(corrections)} user corrections")
```

## Troubleshooting

### Migration failed
```bash
# Check database file exists
ls -la /Users/21cabbage/GlowupAI/backend/.data/

# Manually run migration
sqlite3 .data/skinproof.sqlite3 < skinproof/migrations/003_data_collection_feedback.sql
```

### Module import errors
```bash
# Verify Python path
python -c "import skinproof; print(skinproof.__file__)"

# Install dependencies
pip install -r requirements.txt
```

### No data being collected
```python
# Check user consent
from skinproof.data_collection import DataCollector
from skinproof.db import Database

collector = DataCollector(Database())
has_consent = collector.check_consent("user_id_here")
print(f"Has consent: {has_consent}")

# Check consent events table
from skinproof.db import Database
db = Database()
consents = db.fetchall("SELECT * FROM consent_events WHERE consent_type='data_collection'")
print(f"Found {len(consents)} consent events")
```

## Next Steps

1. **Review Documentation**
   - Read DATA_COLLECTION_POLICY.md
   - Read DATA_COLLECTION_README.md

2. **Test in Development**
   - Create test user with consent
   - Submit test feedback
   - Check monitoring dashboard

3. **Deploy to Production**
   - Add environment variables
   - Set up cron jobs
   - Deploy Android changes

4. **Monitor Results**
   - Check admin dashboard daily
   - Review feedback weekly
   - Export training data monthly

## Support

- **Full Guide**: See `DATA_COLLECTION_README.md`
- **Privacy Policy**: See `DATA_COLLECTION_POLICY.md`
- **Questions**: privacy@glowupai.com

---

**Ready to improve your model with real user data! 🚀**
