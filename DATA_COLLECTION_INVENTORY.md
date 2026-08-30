# Data Collection Inventory for GlowUp AI

**Document Version:** 1.0  
**Last Updated:** August 30, 2026  
**Purpose:** Comprehensive inventory of all data collected, processed, and stored by GlowUp AI

---

## Table of Contents

1. [Overview](#overview)
2. [User Account Data](#user-account-data)
3. [Facial Image Data](#facial-image-data)
4. [Appearance Metrics (Derived Data)](#appearance-metrics-derived-data)
5. [Skincare Routine Data](#skincare-routine-data)
6. [Usage & Analytics Data](#usage--analytics-data)
7. [Subscription & Payment Data](#subscription--payment-data)
8. [Technical & Device Data](#technical--device-data)
9. [Third-Party API Data](#third-party-api-data)
10. [Data Flow Diagram](#data-flow-diagram)
11. [Data Storage Locations](#data-storage-locations)
12. [Data Retention Summary](#data-retention-summary)

---

## Overview

GlowUp AI collects data across several categories to provide cosmetic skincare tracking services. This document provides a complete inventory of:
- **What data** is collected
- **Why** it is collected (purpose)
- **Where** it is stored
- **Who** has access to it
- **How long** it is retained
- **How** it can be deleted

**Key Principle:** All data collection is for service provision only. We do not sell data or use it for advertising.

---

## 1. User Account Data

### 1.1 User Profile Core Fields

| Field Name | Type | Source | Required? | Purpose | Storage Location | Editable? | Deletable? |
|------------|------|--------|-----------|---------|------------------|-----------|------------|
| `user.id` | String (UUID) | Backend auto-generated | YES | Primary user identifier | PostgreSQL backend | NO | Account deletion only |
| `user.firebase_uid` | String | Firebase Auth | YES | Links Firebase account to backend user | PostgreSQL backend | NO | Account deletion only |
| `user.skin_type` | String | User input | NO | Profile information | PostgreSQL backend | YES | YES |
| `user.consent_state` | Enum | User action | YES | Tracks consent to facial data processing | PostgreSQL backend | YES (can decline) | Account deletion only |
| `user.created_at` | Timestamp | Backend auto-generated | YES | Account creation time | PostgreSQL backend | NO | Account deletion only |

**Consent States:**
- `pending` — User has not yet granted consent
- `active` — User has granted consent to facial data processing
- `declined` — User has declined consent (photo features unavailable)

### 1.2 Experience Profile (Optional)

| Field Name | Type | Source | Required? | Purpose | Storage Location | Editable? | Deletable? |
|------------|------|--------|-----------|---------|------------------|-----------|------------|
| `experience_profile.display_name` | String | User input | NO | Display in UI | PostgreSQL backend | YES | YES |
| `experience_profile.focus_vertical` | String | User input | NO | Always "skin" for now | PostgreSQL backend | YES | YES |
| `experience_profile.goals` | Array[String] | User input | NO | User's skincare goals (e.g., "reduce redness") | PostgreSQL backend | YES | YES |
| `experience_profile.experience_level` | String | User input | NO | Skincare knowledge level (e.g., "beginner") | PostgreSQL backend | YES | YES |
| `experience_profile.onboarding_complete` | Boolean | User action | NO | Tracks onboarding progress | PostgreSQL backend | YES | YES |

### 1.3 Appearance Profile

| Field Name | Type | Source | Required? | Purpose | Storage Location | Editable? | Deletable? |
|------------|------|--------|-----------|---------|------------------|-----------|------------|
| `appearance_profile.id` | String (UUID) | Backend auto-generated | YES | Appearance profile identifier | PostgreSQL backend | NO | Account deletion only |
| `appearance_profile.vertical` | String | System-set | YES | Tracking vertical (always "skin") | PostgreSQL backend | NO | Account deletion only |
| `appearance_profile.baseline_capture_id` | String (UUID) | System-set on first photo | NO | Reference to the user's first photo | PostgreSQL backend | NO | Cleared if baseline photo deleted |

### 1.4 Firebase Authentication Data

| Data Element | Source | Purpose | Storage Location | Access |
|--------------|--------|---------|------------------|--------|
| Email address | User input | Account authentication, communication | Firebase Authentication | User, Firebase, GlowUp backend |
| Password hash | Firebase Auth | Account security | Firebase Authentication | Firebase only (hashed) |
| Firebase UID | Firebase Auth | Unique user identifier in Firebase | Firebase Authentication | User, Firebase, GlowUp backend |
| Authentication tokens | Firebase Auth | Session management, API authorization | User's device (local storage) | User device, Firebase |
| Sign-in method | User choice | Tracks if user used Google Sign-In or email/password | Firebase Authentication | Firebase, GlowUp backend |
| Last sign-in timestamp | Firebase Auth | Security auditing | Firebase Authentication | Firebase |

---

## 2. Facial Image Data

### 2.1 Photo Captures

| Field Name | Type | Source | Required? | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `capture.id` | String (UUID) | Backend auto-generated | YES | Unique capture identifier | PostgreSQL backend (metadata only) | YES |
| `capture.user_id` | String (UUID) | User association | YES | Links photo to user account | PostgreSQL backend | Account deletion only |
| `capture.image_base64` | Base64 String | User uploads via camera | YES | The actual facial photo | Firebase Storage (encrypted) | YES |
| `capture.captured_at` | Timestamp | Device or backend | YES | When photo was taken | PostgreSQL backend | YES |
| `capture.is_baseline` | Boolean | System-set | YES | First photo flag | PostgreSQL backend | YES (but affects metrics) |
| `capture.vertical` | String | System-set | YES | Always "skin" | PostgreSQL backend | YES |
| `capture.status` | Enum | System-set | YES | Processing status (e.g., "completed", "pending") | PostgreSQL backend | YES |
| `capture.experiment_id` | String (UUID) | User-initiated experiment | NO | Links photo to an experiment | PostgreSQL backend | YES |
| `capture.analysis_job_id` | String | Async processing | NO | Tracks async analysis job | PostgreSQL backend | YES |

**Storage Details:**
- **Image bytes:** Stored in Firebase Storage bucket `glowup-ai-38ae7.firebasestorage.app` with encryption at rest
- **Metadata:** Stored in PostgreSQL backend database
- **File naming:** Images are stored with the `capture.id` as the filename

### 2.2 Capture Pose & Quality Data

| Field Name | Type | Source | Purpose | Storage Location | Deletable? |
|------------|------|--------|---------|------------------|------------|
| `pose.face_present` | Boolean | ML Kit Face Detection (client) | Validates photo quality | PostgreSQL backend | YES (with photo) |
| `pose.yaw_degrees` | Float | ML Kit Face Detection (client) | Ensures face is front-facing | PostgreSQL backend | YES (with photo) |
| `pose.pitch_degrees` | Float | ML Kit Face Detection (client) | Ensures face is front-facing | PostgreSQL backend | YES (with photo) |
| `pose.distance_cm` | Float | ML Kit Face Detection (client) | Ensures consistent distance | PostgreSQL backend | YES (with photo) |
| `pose.expression_neutral` | Boolean | ML Kit Face Detection (client) | Ensures neutral expression | PostgreSQL backend | YES (with photo) |
| `quality.brightness` | Float | Backend analysis | Photo quality check | PostgreSQL backend | YES (with photo) |
| `quality.sharpness` | Float | Backend analysis | Photo quality check | PostgreSQL backend | YES (with photo) |
| `quality.score` | Float | Backend calculation | Overall quality score | PostgreSQL backend | YES (with photo) |
| `quality.accepted` | Boolean | Backend decision | Whether photo met quality threshold | PostgreSQL backend | YES (with photo) |
| `quality.failed_checks` | Array[String] | Backend validation | Which quality checks failed | PostgreSQL backend | YES (with photo) |
| `quality.coaching` | Array[Object] | Backend guidance | Tips to improve next photo | PostgreSQL backend | YES (with photo) |

---

## 3. Appearance Metrics (Derived Data)

### 3.1 Appearance Metrics (Per Capture)

These metrics are **automatically derived from uploaded photos** using computer vision algorithms.

| Field Name | Type | Source | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `metric.redness_score` | Float (0-100) | Backend CV model | Cosmetic redness assessment | PostgreSQL backend | YES (with photo) |
| `metric.blemish_count` | Integer | Backend CV model | Cosmetic blemish count | PostgreSQL backend | YES (with photo) |
| `metric.darkspot_area` | Float | Backend CV model | Cosmetic dark spot measurement | PostgreSQL backend | YES (with photo) |
| `metric.texture_score` | Float | Backend CV model | Cosmetic texture assessment | PostgreSQL backend | YES (with photo) |
| `metric.confidence` | Float (0-1) | Backend CV model | Algorithmic confidence in metric | PostgreSQL backend | YES (with photo) |
| `metric.confidence_label` | String | Backend | Human-readable confidence (e.g., "high") | PostgreSQL backend | YES (with photo) |
| `metric.model_version` | String | Backend | Version of CV model used | PostgreSQL backend | YES (with photo) |

**Important Notes:**
- These metrics are cosmetic assessments only, NOT medical diagnoses
- Metrics can vary due to lighting, camera quality, and algorithmic limitations
- Users can provide feedback if they disagree with a metric (see Measurement Feedback)

### 3.2 Derived Trends & Deltas

| Field Name | Type | Source | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `delta.redness_delta` | Float | Backend calculation | Change in redness vs. baseline | PostgreSQL backend (calculated on-demand) | YES (with photos) |
| `delta.blemish_delta` | Integer | Backend calculation | Change in blemish count | PostgreSQL backend (calculated on-demand) | YES (with photos) |
| `noise_floor` | Map[String, Float] | Backend statistical analysis | Metric variability threshold | PostgreSQL backend | YES (with photos) |

**Note:** Deltas and trends are calculated dynamically from stored metrics, not stored as separate persistent records.

### 3.3 Measurement Feedback (User-Provided)

| Field Name | Type | Source | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `feedback.id` | String (UUID) | Backend auto-generated | Feedback record identifier | PostgreSQL backend | YES |
| `feedback.capture_id` | String (UUID) | Links to photo | Associates feedback with a capture | PostgreSQL backend | YES |
| `feedback.agreement` | Enum | User input | Whether user agrees with metrics (e.g., "too_high", "accurate", "too_low") | PostgreSQL backend | YES |
| `feedback.note` | String | User input | Optional user comment | PostgreSQL backend | YES |
| `feedback.created_at` | Timestamp | Backend | When feedback was provided | PostgreSQL backend | YES |

---

## 4. Skincare Routine Data

### 4.1 Products

| Field Name | Type | Source | Required? | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `product.id` | String (UUID) | Backend auto-generated | YES | Product identifier | PostgreSQL backend | YES (if no events reference it) |
| `product.name` | String | User input or OCR | YES | Product name | PostgreSQL backend | YES |
| `product.category` | String | User input or OCR | NO | Product type (e.g., "serum", "moisturizer") | PostgreSQL backend | YES |
| `product.barcode` | String | Camera scan or user input | NO | Product barcode for identification | PostgreSQL backend | YES |
| `product.ingredients` | String | User input or OCR | NO | Product ingredients list | PostgreSQL backend | YES |
| `product.stabilization_days` | Integer | User input or default | NO | Days needed for product to show effects | PostgreSQL backend | YES |
| `product.created_at` | Timestamp | Backend | YES | When product was added | PostgreSQL backend | YES |

**Note:** Products are global records (not user-scoped in the database schema), but in practice, users only see their own products via the API.

### 4.2 Routine Events

| Field Name | Type | Source | Required? | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `routine_event.id` | String (UUID) | Backend auto-generated | YES | Event identifier | PostgreSQL backend | YES |
| `routine_event.user_id` | String (UUID) | User association | YES | Links event to user | PostgreSQL backend | Account deletion only |
| `routine_event.product_id` | String (UUID) | Links to product | YES | Which product was used | PostgreSQL backend | YES |
| `routine_event.applied_at` | Timestamp | User input or system | YES | When product was applied | PostgreSQL backend | YES |
| `routine_event.time_of_day` | String | User input | NO | "morning", "evening", etc. | PostgreSQL backend | YES |
| `routine_event.notes` | String | User input | NO | User's notes about application | PostgreSQL backend | YES |
| `routine_event.missed` | Boolean | User input | NO | Whether user missed this application | PostgreSQL backend | YES |

### 4.3 Experiments

| Field Name | Type | Source | Required? | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `experiment.id` | String (UUID) | Backend auto-generated | YES | Experiment identifier | PostgreSQL backend | YES |
| `experiment.user_id` | String (UUID) | User association | YES | Links experiment to user | PostgreSQL backend | Account deletion only |
| `experiment.name` | String | User input | YES | Experiment name (e.g., "Testing Vitamin C serum") | PostgreSQL backend | YES |
| `experiment.product_id` | String (UUID) | Links to product | YES | Which product is being tested | PostgreSQL backend | YES |
| `experiment.started_at` | Timestamp | User input or system | YES | Experiment start date | PostgreSQL backend | YES |
| `experiment.ended_at` | Timestamp | User input | NO | Experiment end date (if concluded) | PostgreSQL backend | YES |
| `experiment.status` | Enum | System or user | YES | "active", "paused", "completed" | PostgreSQL backend | YES |
| `experiment.baseline_capture_id` | String (UUID) | System-set | NO | Photo at experiment start | PostgreSQL backend | YES |
| `experiment.notes` | String | User input | NO | User's experiment notes | PostgreSQL backend | YES |

### 4.4 Context Events (Lifestyle Logs)

| Field Name | Type | Source | Required? | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `context_event.id` | String (UUID) | Backend auto-generated | YES | Event identifier | PostgreSQL backend | YES |
| `context_event.user_id` | String (UUID) | User association | YES | Links event to user | PostgreSQL backend | Account deletion only |
| `context_event.logged_at` | Timestamp | User input or system | YES | When event occurred | PostgreSQL backend | YES |
| `context_event.category` | String | User input | YES | Event type (e.g., "sleep", "stress", "diet") | PostgreSQL backend | YES |
| `context_event.value` | String | User input | YES | Event value (e.g., "poor", "good", "excellent") | PostgreSQL backend | YES |
| `context_event.notes` | String | User input | NO | Additional notes | PostgreSQL backend | YES |

---

## 5. Usage & Analytics Data

### 5.1 Firebase Analytics Events

These are automatically collected by Firebase Analytics SDK.

| Event Type | Data Collected | Purpose | Storage Location | Retention |
|------------|----------------|---------|------------------|-----------|
| `app_open` | Timestamp, device ID, app version | Track app usage | Firebase Analytics | 14 months (Firebase default) |
| `screen_view` | Screen name, timestamp, session ID | Track navigation | Firebase Analytics | 14 months |
| `user_engagement` | Session duration, screen count | Engagement metrics | Firebase Analytics | 14 months |
| Custom events (e.g., `capture_started`, `product_added`) | Event parameters, timestamp | Feature usage tracking | Firebase Analytics | 14 months |

**User Control:** Firebase Analytics is automatically collected and cannot be disabled by the user.

### 5.2 Firebase Crashlytics Reports

| Data Element | Source | Purpose | Storage Location | Retention |
|--------------|--------|---------|------------------|-----------|
| Crash reports | App crashes | Bug identification & fixing | Firebase Crashlytics | 90 days (Firebase default) |
| Stack traces | App execution | Root cause analysis | Firebase Crashlytics | 90 days |
| Device info | Device metadata | Crash context | Firebase Crashlytics | 90 days |
| User ID (if logged in) | App state | Associate crash with user session | Firebase Crashlytics | 90 days |

**Note:** Crash reports are anonymized after 90 days (user IDs removed).

### 5.3 Engagement & Achievements

| Field Name | Type | Source | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `engagement.user_id` | String (UUID) | User association | Links to user | PostgreSQL backend | Account deletion only |
| `engagement.last_activity` | Timestamp | System tracking | Track user activity | PostgreSQL backend | Account deletion only |
| `engagement.current_streak_days` | Integer | System calculation | Track consecutive days | PostgreSQL backend | Account deletion only |
| `engagement.longest_streak_days` | Integer | System calculation | Historical best streak | PostgreSQL backend | Account deletion only |
| `engagement.total_captures` | Integer | System count | Total photos uploaded | PostgreSQL backend | Account deletion only |
| `engagement.achievements` | Array[String] | System awards | Gamification badges | PostgreSQL backend | Account deletion only |

---

## 6. Subscription & Payment Data

### 6.1 Entitlement (Subscription Status)

| Field Name | Type | Source | Purpose | Storage Location | Deletable? |
|------------|------|--------|-----------|---------|------------------|------------|
| `entitlement.user_id` | String (UUID) | User association | Links to user | PostgreSQL backend | Account deletion only |
| `entitlement.plan` | Enum | Subscription choice | "free" or "premium" | PostgreSQL backend | Account deletion only |
| `entitlement.status` | Enum | Subscription state | "active", "expired", "canceled" | PostgreSQL backend | Account deletion only |
| `entitlement.started_at` | Timestamp | Subscription creation | When user subscribed | PostgreSQL backend | Account deletion only |
| `entitlement.renews_at` | Timestamp | Billing system | Next renewal date | PostgreSQL backend | Account deletion only |
| `entitlement.source` | String | Billing platform | "google_play" or "apple_store" | PostgreSQL backend | Account deletion only |

**Payment Credentials:**
- **NOT COLLECTED** — All payment processing (credit cards, billing info) is handled by Google Play Store or Apple App Store
- GlowUp AI never receives or stores credit card numbers or payment credentials

---

## 7. Technical & Device Data

### 7.1 Device Metadata

| Data Element | Type | Source | Purpose | Storage Location | Retention |
|--------------|------|--------|---------|------------------|-----------|
| Device ID | String | Firebase | Analytics, fraud prevention | Firebase Analytics | 14 months |
| Firebase Instance ID | String | Firebase SDK | Push notifications (if implemented) | Firebase | Until app uninstall |
| OS Version | String | Device | Bug context, compatibility | Firebase Analytics, Crashlytics | 14 months / 90 days |
| App Version | String | App | Bug tracking, feature gating | Firebase Analytics, Crashlytics | 14 months / 90 days |
| Device Model | String | Device | Compatibility testing | Firebase Crashlytics | 90 days |
| Screen Resolution | String | Device | UI optimization | Firebase Analytics | 14 months |
| Network Type | String | Device | Performance analysis | Firebase Analytics | 14 months |
| Locale/Language | String | Device | Localization (future) | Firebase Analytics | 14 months |

### 7.2 Local Device Storage (Not Sent to Server)

| Data Element | Purpose | Storage Location | Deletable? |
|--------------|---------|------------------|------------|
| Authentication tokens | Session persistence | Android Encrypted Shared Preferences | App uninstall or logout |
| Cached photos (pending upload) | Offline support | Android Room database (outbox) | Deleted after upload or app uninstall |
| User preferences | UI settings | Android DataStore | App uninstall |
| Cached API responses | Offline browsing | Android Room database | App uninstall or cache clear |

---

## 8. Third-Party API Data

### 8.1 Google Gemini API (AI Q&A)

| Data Sent | Purpose | Data Handling | Storage | User Control |
|-----------|---------|---------------|---------|--------------|
| User questions | Generate AI responses for skincare Q&A | Processed by Gemini API, not used for model training (per enterprise API terms) | Not stored by Google (ephemeral processing) | User can delete Q&A threads in app |
| Product barcode images | OCR for product scanning | Processed by Gemini API for text extraction | Not stored by Google (ephemeral processing) | User uploads voluntarily |

**Privacy Policy:** https://policies.google.com/privacy

### 8.2 Firebase Services (Google)

| Service | Data Shared | Purpose |
|---------|-------------|---------|
| Firebase Authentication | Email, Firebase UID, authentication tokens | User identity management |
| Firebase Storage | Facial photos (encrypted) | Photo storage |
| Firebase Analytics | Device IDs, app interactions, session data | Usage analytics |
| Firebase Crashlytics | Crash logs, stack traces, device info | Bug tracking |

**Privacy Policy:** https://firebase.google.com/support/privacy

---

## 9. Data Flow Diagram

```
USER DEVICE (Android App)
   |
   |--[1] Sign In/Create Account
   |      ↓
   |   Firebase Authentication (Google)
   |      ↓
   |   GlowUp Backend (API) → PostgreSQL Database
   |      ↓
   |   User profile created
   |
   |--[2] Grant Facial Data Consent
   |      ↓
   |   Consent state stored in PostgreSQL
   |
   |--[3] Capture Facial Photo
   |      ↓
   |   ML Kit Face Detection (on-device)
   |      ↓
   |   Photo + Pose Data → GlowUp Backend API
   |      ↓
   |   Photo stored in Firebase Storage (encrypted)
   |   Metadata stored in PostgreSQL
   |      ↓
   |   Backend CV Model processes photo
   |      ↓
   |   Appearance metrics stored in PostgreSQL
   |
   |--[4] Log Products & Routine
   |      ↓
   |   Product data → GlowUp Backend API
   |      ↓
   |   Stored in PostgreSQL
   |
   |--[5] Ask AI Q&A Question
   |      ↓
   |   Question → GlowUp Backend API
   |      ↓
   |   API → Google Gemini API (ephemeral processing)
   |      ↓
   |   Response → User Device
   |   Q&A thread stored in PostgreSQL
   |
   |--[6] App Usage Analytics
   |      ↓
   |   Firebase Analytics SDK (automatic)
   |      ↓
   |   Events stored in Firebase Analytics
   |
   |--[7] Crash Occurs
   |      ↓
   |   Firebase Crashlytics SDK (automatic)
   |      ↓
   |   Crash report stored in Firebase Crashlytics
   |
   |--[8] Export Data
   |      ↓
   |   Request → GlowUp Backend API
   |      ↓
   |   All user data retrieved from PostgreSQL & Firebase Storage
   |      ↓
   |   JSON file → User Device
   |
   |--[9] Delete Account
   |      ↓
   |   Request → GlowUp Backend API
   |      ↓
   |   All user data deleted from PostgreSQL
   |   All photos deleted from Firebase Storage
   |   Firebase Auth account deleted (optional)
   |      ↓
   |   User session terminated
```

---

## 10. Data Storage Locations

| Data Type | Primary Storage | Backup Storage | Geographic Location | Encryption |
|-----------|----------------|----------------|---------------------|------------|
| User profiles, metrics | PostgreSQL (Railway or cloud host) | Cloud provider backups | United States (likely) | Encrypted at rest |
| Facial photos | Firebase Storage | Firebase automatic backups | United States (Firebase default) | Encrypted at rest |
| Authentication tokens | Firebase Authentication | Firebase backups | United States | Encrypted at rest |
| Analytics events | Firebase Analytics | Firebase backups | United States | Encrypted at rest |
| Crash logs | Firebase Crashlytics | Firebase backups | United States | Encrypted at rest |
| Local caches (device) | Android local storage (Room, DataStore) | N/A (device only) | User's device | Android encryption |

**Notes:**
- Firebase services are hosted by Google Cloud Platform, typically in US data centers
- PostgreSQL backend may be hosted on Railway.app or similar cloud providers (US-based)
- All data in transit uses HTTPS/TLS encryption

---

## 11. Data Retention Summary

| Data Category | Retention Policy | Deletion Trigger |
|---------------|------------------|------------------|
| User account data | Until account deletion | User deletes account |
| Facial photos | Until photo/account deletion | User deletes photo or account |
| Appearance metrics | Until account deletion | User deletes account (metrics tied to photos) |
| Routine & product data | Until entry/account deletion | User deletes entry or account |
| Q&A threads | Until thread/account deletion | User deletes thread or account |
| Subscription history | Until account deletion | User deletes account |
| Firebase Analytics | 14 months (automatic) | Firebase automatic expiration |
| Firebase Crashlytics | 90 days (automatic) | Firebase automatic expiration |
| Backup systems | Up to 90 days after deletion | Automatic backup rotation |
| Anonymized aggregated data | Indefinite | Cannot be linked back to user |

**Post-Deletion:**
- Deleted data is removed from production systems immediately
- Backup systems retain data for up to 90 days before permanent deletion
- Anonymized, aggregated data (cannot be linked to individuals) may be retained indefinitely for research and service improvement

---

## 12. User Data Rights & Controls

| Right | How to Exercise | Implementation |
|-------|----------------|----------------|
| **Access** | View data in app; use "Export Data" feature | Export generates complete JSON file with all user data |
| **Correction** | Edit profile, products, routine entries in app | Direct editing via Account settings and product/routine screens |
| **Deletion** | Delete individual entries or entire account via "Delete Account" button | Immediate deletion from production, full removal within 90 days |
| **Portability** | Use "Export Data" feature | JSON file downloadable to device |
| **Consent Withdrawal** | Decline facial data consent or delete account | Consent can be declined (blocks photo features); account deletion removes all data |
| **Object to Processing** | Email privacy@glowupai.com | Manual review by privacy team |
| **Restrict Processing** | Email privacy@glowupai.com | Manual review by privacy team |

---

## 13. Data Sharing Summary

| Recipient | Data Shared | Purpose | Legal Basis |
|-----------|-------------|---------|-------------|
| Google Firebase | Email, photos, device IDs, analytics, crashes | Service provision (auth, storage, analytics) | Service provider agreement |
| Google Gemini API | Q&A questions, barcode images | AI-powered features | Service provider agreement |
| Cloud hosting provider (Railway, etc.) | All backend data | Infrastructure hosting | Service provider agreement |
| Law enforcement (if required) | Any data, as legally required | Legal compliance | Legal obligation |
| No other third parties | N/A | N/A | N/A |

**We do NOT share data for:**
- Advertising or marketing
- Sale to data brokers
- Third-party analytics (beyond Firebase)
- Social media platforms
- Any other commercial purposes

---

## 14. Compliance Notes

### GDPR (EU/EEA)
- **Legal Basis:** Consent (facial data), Contract Performance (service provision), Legitimate Interest (fraud prevention, analytics)
- **Data Controller:** GlowUp AI
- **User Rights:** Access, correction, deletion, portability, consent withdrawal, objection, restriction
- **Data Transfers:** US-based storage; standard contractual clauses or other safeguards apply

### CCPA (California)
- **Categories Collected:** Identifiers, biometric info, commercial info, internet activity, sensory data, inferences
- **Business Purpose:** Service provision, analytics, fraud prevention
- **Sale of Data:** NO — we do not sell personal information
- **User Rights:** Right to know, delete, opt-out (not applicable as we don't sell data), non-discrimination

### COPPA (Children's Privacy)
- **Age Requirement:** 18+
- **Children Under 13:** App is not directed at children; we do not knowingly collect data from children under 13

---

## 15. Incident Response

In the event of a data breach:
1. **Detection:** Identify the breach through monitoring or user reports
2. **Containment:** Immediately secure affected systems
3. **Assessment:** Determine what data was compromised and how many users affected
4. **Notification:** Notify affected users within 72 hours (GDPR) and relevant authorities
5. **Remediation:** Fix vulnerabilities and prevent recurrence
6. **Documentation:** Maintain records of the breach and response for compliance

**Contact for Security Issues:** security@glowupai.com

---

## 16. Document Maintenance

This Data Collection Inventory should be reviewed and updated:
- **Quarterly:** Routine review to ensure accuracy
- **Before major releases:** When new features are added that collect additional data
- **After privacy law changes:** When GDPR, CCPA, or other laws are updated
- **After incidents:** Following any data breach or security incident

**Document Owner:** Privacy Officer / Legal Team  
**Last Reviewed:** August 30, 2026  
**Next Review Due:** November 30, 2026

---

## 17. Contact & Questions

For questions about this inventory or data practices:

**Email:** privacy@glowupai.com  
**Subject Line:** "Data Inventory Inquiry"

**For Data Requests (GDPR/CCPA):**
- Access requests: Use "Export Data" feature in app or email privacy@glowupai.com
- Deletion requests: Use "Delete Account" feature or email privacy@glowupai.com
- Other requests: Email privacy@glowupai.com with specific request type

---

**END OF DOCUMENT**
