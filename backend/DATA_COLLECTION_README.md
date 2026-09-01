# Data Collection Pipeline - Implementation Guide

## Overview

This data collection pipeline enables continuous improvement of our ML model by collecting anonymized user data (with consent), feedback on model accuracy, and monitoring model health in production.

## 🏗️ Architecture

### 3 Core Systems

1. **Data Collection Pipeline** (`data_collection.py`)
   - Collects anonymized capture data
   - Enforces consent requirements
   - Exports training datasets

2. **Feedback Loop** (`feedback.py`)
   - Collects user feedback on accuracy
   - Identifies model weaknesses
   - Provides correction data for retraining

3. **Model Monitoring** (`ml_monitoring.py`)
   - Tracks prediction variance
   - Detects distribution drift
   - Sends alerts for model health issues

## 📦 Installation

### 1. Run Database Migration

```bash
cd /Users/21cabbage/GlowupAI/backend

# Apply migration to add new tables
sqlite3 glowupai.db < glowupai/migrations/003_data_collection_feedback.sql

# Verify tables created
sqlite3 glowupai.db "SELECT name FROM sqlite_master WHERE type='table';"
```

### 2. Configure Environment Variables

Add to `.env`:

```bash
# Data Collection
DATA_COLLECTION_SALT="your-secret-salt-here-change-in-production"

# Email Alerts (for model monitoring)
SMTP_HOST="smtp.gmail.com"
SMTP_PORT="587"
SMTP_USER="alerts@glowupai.com"
SMTP_PASSWORD="your-app-password"
ALERT_EMAIL="team@glowupai.com"

# Slack Alerts
SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
```

### 3. Verify Installation

```python
from glowupai.data_collection import DataCollector
from glowupai.feedback import FeedbackCollector
from glowupai.ml_monitoring import ModelMonitor
from glowupai.db import Database

db = Database()
collector = DataCollector(db)
feedback = FeedbackCollector(db)
monitor = ModelMonitor(db)

print("✅ All modules loaded successfully")
```

## 🚀 Usage

### Data Collection

#### Collect Data from a Capture

```python
from glowupai.data_collection import DataCollector

collector = DataCollector(db)

# Automatically collects if user has consented
success = collector.collect_capture(
    capture_id="capture_123",
    user_id="user_456",
    image_path="/path/to/image.jpg",
    metrics={
        "blemish_count": 12,
        "redness_score": 0.34,
        "texture_score": 15.2,
        "darkspot_area": 0.08,
        "confidence": 0.92,
        "model_version": "v2.1.0"
    },
    quality={
        "brightness": 0.55,
        "sharpness": 0.82,
        "face_present": True,
        "yaw_degrees": 2.3,
        "pitch_degrees": -1.5,
        "distance_cm": 45,
        "score": 0.89
    },
    device_meta={
        "os": "Android",
        "os_version": "14",
        "device_model": "Pixel 8",
        "camera_resolution": "4000x3000"
    }
)

if success:
    print("✅ Data collected successfully")
else:
    print("⏭️ Skipped (no consent or quality too low)")
```

#### Check User Consent

```python
has_consent = collector.check_consent("user_456")
print(f"User consent: {has_consent}")
```

#### Export Training Dataset

```python
stats = collector.export_training_dataset(
    output_dir="/path/to/training_data",
    min_quality=0.75,  # Only high-quality samples
    max_samples=10000   # Limit dataset size
)

print(f"Exported {stats['total_samples']} samples")
print(f"Train: {stats['train_samples']}, Val: {stats['val_samples']}")
```

#### Get Collection Statistics

```python
stats = collector.get_collection_stats()
print(f"Total samples: {stats['total_samples']}")
print(f"Unique faces: {stats['unique_faces']}")
print(f"Quality distribution: {stats['quality_distribution']}")
```

#### Cleanup Old Data (GDPR Compliance)

```python
# Delete data older than 365 days
deleted = collector.cleanup_old_data(retention_days=365)
print(f"Cleaned up {deleted} old files")
```

### Feedback Collection

#### Submit Feedback

```python
from glowupai.feedback import FeedbackCollector

feedback = FeedbackCollector(db)

# User says analysis was inaccurate
feedback_id = feedback.submit_feedback(
    capture_id="capture_123",
    user_id="user_456",
    feedback_type="inaccurate",
    issues=["blemishes_too_high", "redness_too_low"],
    corrections={
        "blemish_count": 8,  # User says actually 8, not 12
        "redness_score": 0.42  # User says higher redness
    },
    comment="The lighting was good but blemish count seems off"
)

print(f"Feedback recorded: {feedback_id}")
```

#### Get Feedback Statistics

```python
stats = feedback.get_feedback_stats()
print(f"Total feedback: {stats['total_feedback']}")
print(f"Accuracy rate (30d): {stats['accuracy_rate_30d']:.1%}")
print(f"Top issues: {stats['top_issues']}")
```

#### Analyze Metric Accuracy

```python
analysis = feedback.get_metric_accuracy_analysis()
for metric, data in analysis.items():
    print(f"{metric}: {data['total_issues']} issues")
    print(f"  Bias: {data['bias']}")
    print(f"  Too high: {data['too_high_pct']:.1%}")
    print(f"  Too low: {data['too_low_pct']:.1%}")
```

#### Check if Retraining Needed

```python
should_retrain, reason = feedback.should_trigger_retraining()
if should_retrain:
    print(f"🔄 Retraining recommended: {reason}")
else:
    print(f"✅ Model healthy: {reason}")
```

#### Export Corrections for Retraining

```python
corrections = feedback.get_pending_corrections(limit=1000)
print(f"Found {len(corrections)} user corrections")

# Export to JSON
feedback.export_feedback_for_retraining(
    output_path="/path/to/corrections.json"
)
```

### Model Monitoring

#### Track Predictions

```python
from glowupai.ml_monitoring import ModelMonitor
import time

monitor = ModelMonitor(db)

# Track every prediction
start_time = time.time()
predictions = {
    "blemish_count": 12,
    "redness_score": 0.34,
    "texture_score": 15.2,
    "darkspot_area": 0.08
}
processing_time = (time.time() - start_time) * 1000

monitor.track_prediction(
    capture_id="capture_123",
    predictions=predictions,
    processing_time_ms=processing_time,
    error=None  # or error message if failed
)
```

#### Check Model Health

```python
health = monitor.get_health_status()
print(f"Status: {health['status']}")
print(f"Issues: {health['issues']}")
print(f"Variance: {health['variance']['current']}")
print(f"Error rate: {health['error_rate']['current']:.2%}")
print(f"Drift: {health['drift']['current']}")
```

#### Run Automated Health Checks

```python
# Call this from a cron job or scheduled task
monitor.check_and_alert()
# Sends email/Slack alerts if issues detected
```

#### Generate Daily Report

```python
report = monitor.generate_daily_report()
print(f"Date: {report['date']}")
print(f"Total predictions: {report['predictions']['total']}")
print(f"Error rate: {report['predictions']['error_rate']:.2%}")
print(f"Feedback accuracy: {report['feedback']['accuracy_rate']:.1%}")
```

## 🤖 Android Integration

### User Consent

```kotlin
// Show consent screen on first launch
DataConsentRoute(
    onConsent = { granted ->
        // Call API to record consent
        api.submitDataCollectionConsent(userId, granted)
    },
    onBack = { /* navigate back */ }
)
```

### API Call to Record Consent

```kotlin
// POST /api/users/{user_id}/consent/data-collection
data class ConsentRequest(
    val granted: Boolean,
    val policyVersion: String = "1.0"
)

suspend fun submitDataCollectionConsent(userId: String, granted: Boolean) {
    apiService.submitConsent(
        userId = userId,
        body = ConsentRequest(granted = granted)
    )
}
```

### Feedback Dialog

```kotlin
// Show after capture result
var showFeedbackDialog by remember { mutableStateOf(true) }

if (showFeedbackDialog) {
    FeedbackDialog(
        captureId = captureId,
        onDismiss = { showFeedbackDialog = false },
        onSubmit = { feedbackData ->
            // Call API to submit feedback
            viewModel.submitFeedback(feedbackData)
            showFeedbackDialog = false
        }
    )
}
```

### API Call to Submit Feedback

```kotlin
// POST /api/captures/{capture_id}/feedback
data class FeedbackRequest(
    val feedbackType: String,  // "accurate" or "inaccurate"
    val issues: List<String>,
    val corrections: Map<String, Float>,
    val comment: String?
)

suspend fun submitCaptureFeedback(
    captureId: String,
    feedbackData: FeedbackData
) {
    apiService.submitFeedback(
        captureId = captureId,
        body = FeedbackRequest(
            feedbackType = feedbackData.feedbackType.name.lowercase(),
            issues = feedbackData.issues,
            corrections = emptyMap(),  // Optional: add slider for corrections
            comment = feedbackData.comment
        )
    )
}
```

## 📊 Admin Dashboard

### Feedback Dashboard

```bash
# GET /api/admin/feedback
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.glowupai.com/api/admin/feedback
```

Response:
```json
{
  "total_feedback": 1523,
  "by_type": {
    "accurate": 1234,
    "inaccurate": 289
  },
  "accuracy_rate_30d": 0.81,
  "top_issues": [
    {"issue": "blemishes_too_high", "count": 127},
    {"issue": "redness_too_low", "count": 89}
  ]
}
```

### Monitoring Dashboard

```bash
# GET /api/admin/monitoring
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.glowupai.com/api/admin/monitoring
```

Response:
```json
{
  "status": "healthy",
  "variance": {
    "current": {
      "blemish_count": 0.023,
      "redness_score": 0.018
    },
    "threshold": 0.05,
    "status": "ok"
  },
  "error_rate": {
    "current": 0.003,
    "threshold": 0.01,
    "status": "ok"
  }
}
```

### Data Collection Stats

```bash
# GET /api/admin/data-collection/stats
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  https://api.glowupai.com/api/admin/data-collection/stats
```

### Export Training Dataset

```bash
# POST /api/admin/data-collection/export
curl -X POST \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "output_dir": "/mnt/training/dataset_2026_09",
    "min_quality": 0.75,
    "max_samples": 10000
  }' \
  https://api.glowupai.com/api/admin/data-collection/export
```

## 🔄 Automated Workflows

### Cron Jobs

Add to crontab:

```cron
# Check model health every hour
0 * * * * python -c "from glowupai.ml_monitoring import ModelMonitor; from glowupai.db import Database; ModelMonitor(Database()).check_and_alert()"

# Cleanup old data weekly (every Sunday at 2 AM)
0 2 * * 0 python -c "from glowupai.data_collection import DataCollector; from glowupai.db import Database; DataCollector(Database()).cleanup_old_data(365)"

# Generate daily report (every day at 8 AM)
0 8 * * * python -c "from glowupai.ml_monitoring import ModelMonitor; from glowupai.db import Database; print(ModelMonitor(Database()).generate_daily_report())" | mail -s "Daily ML Report" team@glowupai.com
```

### Background Worker

```python
# worker.py
import time
from glowupai.db import Database
from glowupai.ml_monitoring import ModelMonitor

def monitor_loop():
    db = Database()
    monitor = ModelMonitor(db)
    
    while True:
        print("🔍 Checking model health...")
        monitor.check_and_alert()
        
        # Check every hour
        time.sleep(3600)

if __name__ == "__main__":
    monitor_loop()
```

Run as systemd service:

```ini
# /etc/systemd/system/glowup-monitor.service
[Unit]
Description=GlowupAI Model Monitor
After=network.target

[Service]
Type=simple
User=glowup
WorkingDirectory=/opt/glowupai
ExecStart=/usr/bin/python3 worker.py
Restart=always

[Install]
WantedBy=multi-user.target
```

## 🔒 Security & Privacy

### Access Controls

```python
# Only admins can access these endpoints
@app.get("/api/admin/feedback")
def admin_feedback(authorization: str | None = Header(default=None)):
    _require_admin(authorization)  # Validates admin token
    return run(active.get_feedback_stats)
```

### Data Anonymization

```python
# User ID is hashed one-way
def anonymize_user_id(user_id: str) -> str:
    salt = os.getenv("DATA_COLLECTION_SALT")
    hash_input = f"{user_id}:{salt}".encode("utf-8")
    return hashlib.sha256(hash_input).hexdigest()[:16]
```

### Consent Enforcement

```python
# Collection only happens if user has consented
def collect_capture(...):
    if not self.check_consent(user_id):
        return False  # Skip collection
    # ... collect data
```

## 📈 Monitoring Best Practices

1. **Set up alerts**: Configure email/Slack webhooks
2. **Review daily reports**: Check model health regularly
3. **Act on feedback**: Investigate high-issue metrics
4. **Export regularly**: Export training data monthly
5. **Clean up data**: Run cleanup script quarterly

## 🐛 Troubleshooting

### "No consent found"
- Check consent_events table for user
- Verify consent screen shown in app
- Check API call for consent recording

### "Collection not working"
- Check DATA_COLLECTION_SALT in .env
- Verify database migration ran
- Check file permissions on .data/ directory

### "Alerts not sending"
- Verify SMTP credentials in .env
- Check Slack webhook URL
- Test with: `monitor.send_slack_alert("Test")`

### "Low accuracy rate"
- Review feedback issues: `feedback.get_metric_accuracy_analysis()`
- Check if specific metrics have bias
- Consider retraining trigger

## 📞 Support

- **Documentation**: See DATA_COLLECTION_POLICY.md
- **Issues**: Open GitHub issue
- **Questions**: privacy@glowupai.com

---

**Built with privacy and ethics in mind. Questions? We're here to help.**
