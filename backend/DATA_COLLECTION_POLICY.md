# Data Collection Policy for Model Training

**Last Updated:** September 1, 2026  
**Version:** 1.0

## Overview

GlowupAI collects anonymized user data to improve our machine learning models. This document explains what data we collect, how it's used, and how we protect user privacy.

## Consent Required

- **Opt-in only**: Data collection requires explicit user consent
- **Clear explanation**: Users see a consent screen explaining what's collected
- **Easy opt-out**: Users can revoke consent anytime in Settings
- **No impact on service**: App works fully without consent

## Data Collected

### 1. Capture Images
- **What**: Facial photos taken during skin analysis
- **Why**: Train future ML models to recognize skin conditions
- **Anonymization**: Linked to random face_id hash, not user_id

### 2. Ground Truth Metrics
- **What**: Analysis results from current model
  - Blemish count
  - Redness score
  - Dark spot area
  - Texture score
  - Confidence levels
- **Why**: Provides labeled training data for supervised learning
- **Note**: Model version tracked for analysis

### 3. Capture Quality Metadata
- **What**: Technical quality indicators
  - Brightness level
  - Sharpness score
  - Face detection confidence
  - Pose angles (yaw/pitch)
  - Distance from camera
- **Why**: Filter low-quality samples and improve data quality

### 4. Device Metadata
- **What**: Technical device information
  - OS type and version
  - Device model
  - Camera resolution
- **Why**: Understand hardware diversity and improve compatibility
- **Not collected**: Device ID, IP address, location

### 5. User Feedback
- **What**: Accuracy ratings and corrections
  - "Was this accurate?" (thumbs up/down)
  - Specific issues identified
  - User-provided corrections
- **Why**: Identify model weaknesses and correction opportunities

## Privacy Protections

### Anonymization
```python
# User ID is hashed one-way with salt
face_id = SHA256(user_id + salt)[:16]
```
- **One-way hashing**: Cannot reverse face_id back to user_id
- **Consistent grouping**: Same user gets same face_id
- **No PII stored**: Name, email, location never collected

### Data Retention
- **Automatic deletion**: Data deleted after 365 days
- **Compliance**: Meets GDPR/CCPA requirements
- **Right to deletion**: Users can request immediate deletion

### Access Controls
- **Encrypted storage**: Data encrypted at rest
- **Limited access**: Only ML team can access training data
- **Audit logs**: All access logged for security review

## Data Usage

### Permitted Uses
1. **Model training**: Train improved ML models
2. **Quality analysis**: Understand model performance
3. **Research**: Academic research with anonymized data

### Prohibited Uses
- **Never sold**: Data never sold to third parties
- **No advertising**: Not used for targeted advertising
- **No identification**: Never used to identify individuals
- **No sharing**: Not shared with external companies

## User Rights

### Right to Know
- View this policy anytime
- Request data collection statistics
- See consent history

### Right to Opt-Out
- Revoke consent in Settings → Privacy
- Stop future collection immediately
- No penalty for opting out

### Right to Deletion
- Request data deletion via Settings
- Complete within 30 days
- Confirmation email sent

### Right to Export
- Request copy of anonymized data
- Provided in JSON format
- Includes all metadata

## Technical Implementation

### Data Collection Pipeline

```
User Capture
    ↓
Check Consent ✓
    ↓
Anonymize User ID → face_id hash
    ↓
Store Image + Metadata
    ↓
Log Collection Event
    ↓
Queue for Training Export
```

### Storage Structure

```
.data/training_collection/
├── images/
│   └── {face_id}_{timestamp}_{capture_id}.jpg
├── labels/
│   └── {face_id}_{timestamp}_{capture_id}.json
└── metadata/
    └── {face_id}_{timestamp}_{capture_id}.json
```

### Database Tables

**collection_log**: Tracks collected samples
```sql
CREATE TABLE collection_log (
    face_id TEXT NOT NULL,
    anonymous_capture_id TEXT NOT NULL,
    collected_at TEXT NOT NULL,
    quality_score REAL NOT NULL,
    model_version TEXT NOT NULL
);
```

**capture_feedback**: User feedback on accuracy
```sql
CREATE TABLE capture_feedback (
    capture_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    feedback_type TEXT NOT NULL,
    issues_json TEXT,
    corrections_json TEXT,
    created_at TEXT NOT NULL
);
```

## Quality Controls

### Automatic Filtering
- **Quality threshold**: Only accept captures with quality_score >= 0.75
- **Blur detection**: Reject blurry images
- **Face detection**: Must have face detected
- **Lighting**: Must meet brightness requirements

### Manual Review
- Random sample review by ML team
- Flag inappropriate content
- Quality spot-checks

## Compliance

### GDPR (European Union)
- ✅ Explicit consent required
- ✅ Right to access data
- ✅ Right to deletion
- ✅ Right to data portability
- ✅ Data minimization
- ✅ Purpose limitation

### CCPA (California)
- ✅ Right to know what's collected
- ✅ Right to deletion
- ✅ Right to opt-out
- ✅ No discrimination for opting out

### HIPAA Considerations
- ⚠️ Not HIPAA-covered entity
- ⚠️ Not medical diagnosis tool
- ✅ Medical disclaimer provided
- ✅ Recommend professional consultation

## Transparency

### Public Metrics
We publish quarterly reports:
- Number of consented users
- Total samples collected
- Data quality distribution
- Model improvement metrics

### Research Publications
- Anonymized datasets may be used in research
- Published papers cite data source
- Never include identifiable information

## Contact

### Questions or Concerns
- **Email**: privacy@glowupai.com
- **Response time**: Within 5 business days

### Data Subject Requests
- **Email**: privacy@glowupai.com
- **Subject line**: "Data Subject Request - [Your Request]"
- **Response time**: Within 30 days

### Security Issues
- **Email**: security@glowupai.com
- **Response time**: Within 24 hours

## Changes to Policy

- **Notification**: Email sent to consented users
- **Re-consent**: Required for material changes
- **Version history**: Available at /privacy/data-collection-history

## Audit Trail

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-09-01 | Initial policy |

---

**Your privacy matters to us. Questions? Contact privacy@glowupai.com**
