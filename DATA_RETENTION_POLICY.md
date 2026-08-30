# Data Retention Policy for GlowUp AI

**Document Version:** 1.0  
**Effective Date:** August 30, 2026  
**Last Updated:** August 30, 2026  
**Document Owner:** Privacy Officer / Legal Team

---

## Table of Contents

1. [Purpose](#purpose)
2. [Scope](#scope)
3. [Retention Principles](#retention-principles)
4. [Retention Schedules by Data Category](#retention-schedules-by-data-category)
5. [Account Deletion Process](#account-deletion-process)
6. [Backup and Recovery](#backup-and-recovery)
7. [Legal and Regulatory Compliance](#legal-and-regulatory-compliance)
8. [Data Anonymization](#data-anonymization)
9. [Exceptions to Retention Limits](#exceptions-to-retention-limits)
10. [Roles and Responsibilities](#roles-and-responsibilities)
11. [Policy Review and Updates](#policy-review-and-updates)
12. [Contact Information](#contact-information)

---

## 1. Purpose

This Data Retention Policy establishes the guidelines for how long GlowUp AI retains user data and the procedures for secure deletion. The policy aims to:

- **Balance user needs** with privacy and security best practices
- **Comply with legal obligations** under GDPR, CCPA, and other privacy laws
- **Minimize data storage** to only what is necessary for service provision
- **Ensure secure deletion** when data is no longer needed
- **Provide transparency** to users about how long their data is retained

---

## 2. Scope

This policy applies to all personal data collected, processed, and stored by GlowUp AI, including:

- User account and profile information
- Facial photographs and appearance metrics
- Skincare routine and product data
- Usage analytics and technical data
- Subscription and entitlement records
- Data stored in third-party services (Firebase, backend databases)

This policy applies to:
- All GlowUp AI employees, contractors, and service providers
- All systems and databases (production, staging, backups)
- All geographic locations where data is stored

---

## 3. Retention Principles

GlowUp AI adheres to the following principles for data retention:

### 3.1 Data Minimization
**Retain only what is necessary** to provide the Service and fulfill legal obligations. Do not retain data "just in case" it might be useful someday.

### 3.2 Purpose Limitation
**Retain data only for the purpose it was collected.** Once the purpose is fulfilled, data should be deleted or anonymized.

### 3.3 User Control
**Users control their data.** They can delete individual entries (photos, products, routine events) or their entire account at any time.

### 3.4 Transparency
**Users are informed** about retention periods via the Privacy Policy, Terms of Service, and this document.

### 3.5 Secure Deletion
**Deleted data is permanently removed** from production systems immediately and from all backups within a defined timeframe (90 days).

### 3.6 Legal Compliance
**Retain data as required by law** for tax, audit, legal disputes, or regulatory requirements, even if it exceeds standard retention periods.

---

## 4. Retention Schedules by Data Category

### 4.1 User Account Data

| Data Type | Retention Period | Deletion Trigger | Notes |
|-----------|------------------|------------------|-------|
| User ID, Firebase UID | Account lifetime | Account deletion | Primary identifiers |
| Email address | Account lifetime | Account deletion | Used for auth & communication |
| Display name | Account lifetime | User edit or account deletion | Optional field |
| Skin type, goals, experience level | Account lifetime | User edit or account deletion | Profile information |
| Consent state & version | Account lifetime + 7 years | Account deletion + statute of limitations | Legal record of consent |
| Account creation timestamp | Account lifetime + 7 years | Account deletion + legal retention | Audit trail |

**Account Lifetime:** Until the user deletes their account via the "Delete Account" feature or requests deletion via email.

**Post-Deletion:** User account data is immediately removed from production systems. Data in backup systems is permanently deleted within **90 days**.

**Exception:** Consent records may be retained for **7 years** after account deletion as legal proof of user consent, stored separately from other personal data in anonymized form (user ID only, no email or photos).

---

### 4.2 Facial Image Data

| Data Type | Retention Period | Deletion Trigger | Storage Location | Notes |
|-----------|------------------|------------------|------------------|-------|
| Photo image files (Base64/binary) | Account lifetime or until user deletes photo | User deletes photo or account | Firebase Storage | Encrypted at rest |
| Capture metadata (timestamps, pose, quality) | Account lifetime or until user deletes photo | User deletes photo or account | PostgreSQL backend | Tied to photo |
| Baseline capture reference | Account lifetime | User deletes baseline photo or account | PostgreSQL backend | Cleared if baseline deleted |

**User Control:**
- Users can **delete individual photos** at any time from their capture history
- Deleting a photo also deletes its metadata and appearance metrics
- Deleting the account deletes **all photos**

**Post-Deletion:**
- Deleted photos are immediately removed from Firebase Storage (production)
- Firebase Storage backups may retain photos for up to **30 days** (Firebase automatic backup retention)
- Backend metadata is immediately removed from PostgreSQL production database
- Backend database backups retain metadata for up to **90 days** before permanent deletion

**Anonymization:** No anonymization option — photos are fully deleted because they cannot be anonymized.

---

### 4.3 Appearance Metrics (Derived Data)

| Data Type | Retention Period | Deletion Trigger | Notes |
|-----------|------------------|------------------|-------|
| Redness, blemishes, texture, dark spots scores | Account lifetime or until photo deleted | Photo deletion or account deletion | Derived from photos |
| Confidence scores, model versions | Account lifetime or until photo deleted | Photo deletion or account deletion | Analysis metadata |
| Deltas & trends (calculated on-demand) | N/A (not stored persistently) | N/A | Calculated dynamically from stored metrics |

**Retention Rationale:**
- Appearance metrics are meaningless without their associated photos
- Metrics are retained for as long as the photo exists to enable historical comparisons
- Deleting a photo deletes its metrics

**Post-Deletion:**
- Metrics are immediately removed from production database
- Backup retention: **90 days**

**Anonymization:** Not applicable — metrics are deleted, not anonymized.

---

### 4.4 Skincare Routine Data

| Data Type | Retention Period | Deletion Trigger | User Control | Notes |
|-----------|------------------|------------------|--------------|-------|
| Products (name, barcode, ingredients) | Account lifetime or until user deletes | User deletes product or account | Individual deletion or bulk via account deletion | Global records (in practice user-scoped) |
| Routine events (applications, timestamps) | Account lifetime or until user deletes | User deletes event or account | Individual deletion or bulk | Logs of product usage |
| Experiments (product tests, notes) | Account lifetime or until user deletes | User deletes experiment or account | Individual deletion or bulk | Tracking product changes |
| Context events (sleep, stress, diet) | Account lifetime or until user deletes | User deletes event or account | Individual deletion or bulk | Lifestyle factors |

**User Control:**
- Users can delete **individual products, routine events, experiments, or context logs** at any time
- Deleting the account deletes **all routine data**

**Post-Deletion:**
- Immediate removal from production database
- Backup retention: **90 days**

**Anonymization:** Not applicable — routine data is user-specific and cannot be meaningfully anonymized.

---

### 4.5 Q&A Threads (AI Conversations)

| Data Type | Retention Period | Deletion Trigger | User Control | Notes |
|-----------|------------------|------------------|--------------|-------|
| Q&A questions and AI responses | Account lifetime or until thread deleted | User deletes thread or account | Individual thread deletion or bulk | Stored in backend |
| Questions sent to Gemini API | Ephemeral (not stored by Google) | Processed and discarded immediately | N/A | Per Google enterprise API terms |

**User Control:**
- Users can delete **individual Q&A threads** at any time
- Deleting the account deletes **all Q&A history**

**Post-Deletion:**
- Immediate removal from production database
- Backup retention: **90 days**

**Third-Party Processing:** Google Gemini API processes questions ephemerally (not stored by Google after processing is complete).

---

### 4.6 Usage Analytics & Technical Data

#### Firebase Analytics

| Data Type | Retention Period | Deletion Trigger | User Control | Notes |
|-----------|------------------|------------------|--------------|-------|
| App interaction events (screen views, clicks) | 14 months (automatic) | Firebase automatic expiration | Cannot be disabled | Firebase default retention |
| Device IDs, session data | 14 months (automatic) | Firebase automatic expiration | Cannot be disabled | Firebase default |
| Custom events (e.g., capture_started) | 14 months (automatic) | Firebase automatic expiration | Cannot be disabled | Firebase default |

**Retention Rationale:** Firebase Analytics automatically expires data after **14 months**. This is a Firebase platform constraint, not configurable by GlowUp AI.

**Account Deletion Impact:**
- Deleting a GlowUp account does **not** delete historical Firebase Analytics events (Firebase limitation)
- Future events will not be generated after account deletion
- Events auto-expire after 14 months

**Anonymization:** After 14 months, Firebase auto-expires events. GlowUp AI may retain **aggregated, anonymized analytics** (e.g., "50% of users access the Capture feature daily") indefinitely for service improvement.

#### Firebase Crashlytics

| Data Type | Retention Period | Deletion Trigger | User Control | Notes |
|-----------|------------------|------------------|--------------|-------|
| Crash reports, stack traces | 90 days (automatic) | Firebase automatic expiration | Cannot be disabled | Firebase default retention |
| User IDs in crash reports | 90 days, then anonymized | Firebase automatic anonymization | N/A | User IDs removed after 90 days |

**Retention Rationale:** Crash reports are essential for debugging and security. Firebase Crashlytics retains crash data for **90 days**, then automatically anonymizes it by removing user IDs.

**Account Deletion Impact:**
- Deleting an account does **not** delete historical crash reports (Firebase limitation)
- User IDs in crash reports are automatically anonymized after 90 days

---

### 4.7 Subscription & Payment Data

| Data Type | Retention Period | Deletion Trigger | Notes |
|-----------|------------------|------------------|-------|
| Subscription plan (free/premium) | Account lifetime | Account deletion | Entitlement record |
| Subscription status, renewal dates | Account lifetime | Account deletion | Billing metadata |
| Subscription source (e.g., "google_play") | Account lifetime | Account deletion | Platform identifier |
| Payment credentials (credit cards) | NEVER COLLECTED | N/A | Handled by Google Play Store |

**Retention Rationale:**
- Subscription data is necessary to provide Premium features
- We do **not** store payment credentials (handled by app stores)

**Post-Deletion:**
- Immediate removal from production database
- Backup retention: **90 days**

**Legal Retention:** If there is a billing dispute or chargeback, subscription records may be retained for up to **7 years** after account deletion for legal/tax purposes (anonymized to the extent possible).

---

### 4.8 Engagement & Achievements

| Data Type | Retention Period | Deletion Trigger | Notes |
|-----------|------------------|------------------|-------|
| Streaks, achievements, engagement stats | Account lifetime | Account deletion | Gamification data |

**Retention Rationale:** Engagement data is part of the user experience (gamification) and is retained to motivate continued app usage.

**Post-Deletion:**
- Immediate removal from production database
- Backup retention: **90 days**

---

### 4.9 Audit Logs & Admin Data

| Data Type | Retention Period | Deletion Trigger | Notes |
|-----------|------------------|------------------|-------|
| Admin audit logs (admin actions) | 7 years | Automatic expiration | Legal/compliance requirement |
| User activity logs (for fraud detection) | 1 year | Automatic expiration | Security monitoring |
| Data export requests (logs) | 3 years | Automatic expiration | Compliance documentation |
| Data deletion requests (logs) | 7 years | Automatic expiration | Legal proof of deletion |

**Retention Rationale:**
- Audit logs are retained for **7 years** to comply with legal, tax, and regulatory requirements
- Logs are stored separately from user data and contain minimal personal information (user ID, action type, timestamp)

**Account Deletion Impact:**
- Deleting an account does **not** delete audit logs of admin actions related to that account
- Audit logs may reference a deleted user's ID for compliance purposes but do not contain photos, metrics, or other sensitive personal data

---

## 5. Account Deletion Process

### 5.1 User-Initiated Deletion

**Method 1: In-App**
1. User navigates to **Account Settings**
2. User taps **"Delete Account"**
3. App displays a warning: "This action is permanent and cannot be undone. All your photos, metrics, and routine data will be deleted."
4. User confirms deletion
5. App sends deletion request to backend API
6. Backend immediately deletes user data (see below)
7. App logs user out and clears local cache

**Method 2: Email Request**
1. User emails **privacy@glowupai.com** with subject "Delete My Account"
2. Privacy team verifies user identity (email match or security questions)
3. Privacy team manually triggers account deletion via admin API
4. Privacy team confirms deletion to user within **5 business days**

### 5.2 Backend Deletion Procedure

When an account deletion request is received:

1. **Immediate Production Deletion:**
   - User profile, appearance profiles, entitlement, experience profile deleted from PostgreSQL
   - All facial photos deleted from Firebase Storage
   - All capture metadata, appearance metrics deleted from PostgreSQL
   - All products, routine events, experiments, context events deleted from PostgreSQL
   - All Q&A threads deleted from PostgreSQL
   - All engagement records deleted from PostgreSQL
   - Firebase Authentication account deleted (optional — user can choose to keep Firebase account for other services)

2. **Session Termination:**
   - All active authentication tokens invalidated
   - User is logged out of all devices

3. **Backup Deletion:**
   - Deletion flag is set in backup metadata
   - Backup systems will permanently delete data within **90 days**
   - Automated backup rotation ensures data does not persist beyond 90 days

4. **Third-Party Services:**
   - Firebase Analytics: Historical events cannot be deleted (Firebase limitation) but auto-expire after 14 months
   - Firebase Crashlytics: Historical crash reports cannot be deleted (Firebase limitation) but auto-anonymize after 90 days

5. **Audit Log Entry:**
   - Deletion event logged in admin audit log (retained for 7 years for compliance)
   - Log entry contains: user ID (anonymized hash), deletion timestamp, deletion method (user-initiated or admin)

### 5.3 Deletion Confirmation

After deletion:
- User receives confirmation email: "Your GlowUp AI account has been deleted. All your data will be permanently removed from our backups within 90 days."
- User can no longer log in to the app
- Any future login attempts result in "Account not found" error

### 5.4 Exceptions to Deletion

The following data **may** be retained after account deletion:
- **Audit logs:** User ID (anonymized) in audit logs retained for 7 years
- **Consent records:** Proof of consent (user ID only, no photos or personal data) retained for 7 years
- **Legal disputes:** If user's account is subject to an ongoing legal dispute, data may be retained until dispute resolution
- **Anonymized aggregated data:** Statistical data that cannot be linked back to the user (e.g., "average redness score across all users") may be retained indefinitely

---

## 6. Backup and Recovery

### 6.1 Backup Strategy

GlowUp AI maintains backups for disaster recovery and business continuity:

- **PostgreSQL Database Backups:**
  - **Frequency:** Daily incremental backups, weekly full backups
  - **Retention:** 90 days (rolling)
  - **Storage:** Cloud provider backup system (encrypted)

- **Firebase Storage Backups:**
  - **Frequency:** Automatic Firebase backups
  - **Retention:** 30 days (Firebase default)
  - **Storage:** Google Cloud backup infrastructure

### 6.2 Backup Deletion Policy

When user data is deleted:

1. **Immediate production deletion:** Data is removed from active production systems immediately
2. **Backup metadata flagging:** Deletion event is flagged in backup metadata
3. **Backup expiration:** Deleted data remains in backups for up to **90 days** (PostgreSQL) or **30 days** (Firebase Storage), then is permanently removed as backups rotate
4. **No restoration:** Deleted user data is never restored from backups unless required by law

### 6.3 Disaster Recovery Exclusions

In the event of a disaster recovery (e.g., database corruption, ransomware attack):
- If backups are restored, **deletion flags are preserved**
- Any data that was marked for deletion before the disaster will be deleted again immediately upon restoration
- Users who deleted their accounts before the disaster will remain deleted

---

## 7. Legal and Regulatory Compliance

### 7.1 GDPR Compliance (EU/EEA)

Under the General Data Protection Regulation (GDPR):

- **Right to Erasure (Right to be Forgotten):** Users can request deletion at any time. GlowUp AI complies within **30 days** (actual deletion is immediate; 90-day backup retention is disclosed).
- **Data Retention Limitation:** Data is retained only as long as necessary for the purpose it was collected.
- **Legal Basis for Extended Retention:** Consent records are retained for 7 years under "legal obligation" (proof of compliance).

### 7.2 CCPA Compliance (California)

Under the California Consumer Privacy Act (CCPA):

- **Right to Deletion:** California residents can request deletion of their personal information. GlowUp AI complies within **45 days** (actual deletion is immediate).
- **Exceptions to Deletion:** Data may be retained if necessary to:
  - Complete a transaction
  - Detect security incidents or fraud
  - Comply with legal obligations
  - Internal lawful uses reasonably aligned with user expectations

### 7.3 Tax and Financial Record Retention

- **Subscription records** for tax purposes: Retained for **7 years** after the last transaction (anonymized to the extent possible)
- **Invoices and payment logs:** Handled by Google Play Store / Apple App Store, not retained by GlowUp AI

### 7.4 Litigation Hold

If a user's account is subject to a legal dispute, lawsuit, or regulatory investigation:
- **Litigation hold** is placed on the account
- Data is **not deleted** until the legal matter is resolved, even if the user requests deletion
- User is notified of the litigation hold and the reason for retention (if legally permissible)

---

## 8. Data Anonymization

### 8.1 Anonymization vs. Deletion

**Anonymization:** Removing or hashing personal identifiers so data can no longer be linked to an individual.
**Deletion:** Permanently removing data entirely.

**GlowUp AI's Approach:**
- Most user data is **deleted, not anonymized**, because photos and biometric data cannot be meaningfully anonymized
- **Aggregated analytics** (e.g., average metrics across all users) are anonymized and may be retained indefinitely for service improvement

### 8.2 Anonymized Data Retention

The following data may be retained **indefinitely** after anonymization:

- **Aggregated usage statistics:** "50% of users capture photos at night" — no individual identifiers
- **Product effectiveness trends:** "Vitamin C serums show a 20% average improvement in dark spots" — no user IDs
- **App performance metrics:** "Average app load time is 2 seconds" — no user-specific data

**Anonymization Process:**
1. Data is aggregated across all users
2. All personal identifiers (user IDs, emails, photos) are removed
3. Data is verified to be non-identifiable (cannot be reverse-engineered to link to individuals)
4. Anonymized data is stored separately from personal data

---

## 9. Exceptions to Retention Limits

Data may be retained beyond standard retention periods in the following circumstances:

### 9.1 Legal Obligations
- **Tax records:** 7 years (IRS requirement in the US)
- **Consent records:** 7 years (proof of GDPR compliance)
- **Audit logs:** 7 years (industry standard for compliance)

### 9.2 Legal Disputes
- Data may be retained until a lawsuit, regulatory investigation, or legal dispute is resolved
- Litigation hold takes precedence over user deletion requests

### 9.3 Fraud or Security Investigations
- If a user's account is flagged for fraud, abuse, or security violations, data may be retained for investigation purposes (up to 3 years)

### 9.4 User Requests Extension
- If a user explicitly requests to retain data beyond standard periods (e.g., for personal records), GlowUp AI may accommodate on a case-by-case basis

---

## 10. Roles and Responsibilities

### 10.1 Privacy Officer
- **Oversees data retention policy** compliance
- **Approves exceptions** to retention limits
- **Responds to user data requests** (access, deletion, correction)
- **Conducts annual policy reviews**

### 10.2 Engineering Team
- **Implements automated deletion** procedures
- **Maintains backup systems** with proper retention limits
- **Ensures secure deletion** (data is unrecoverable after deletion)
- **Monitors compliance** with retention schedules

### 10.3 Legal Team
- **Reviews retention policy** for legal compliance
- **Advises on litigation holds** and legal exceptions
- **Updates policy** when laws change

### 10.4 Customer Support Team
- **Assists users** with data export and deletion requests
- **Verifies user identity** for email-based deletion requests
- **Escalates complex cases** to Privacy Officer

---

## 11. Policy Review and Updates

### 11.1 Review Schedule
- **Annual review:** Every 12 months (next review: August 30, 2027)
- **Regulatory changes:** Immediate review when GDPR, CCPA, or other laws are updated
- **Major product changes:** Review before launching features that collect new data types
- **Post-incident:** Review after any data breach or compliance incident

### 11.2 Update Process
1. Privacy Officer initiates review
2. Legal and Engineering teams provide input
3. Proposed changes are documented
4. Changes are approved by executive leadership
5. Updated policy is published to company wiki and public-facing documents
6. Users are notified of material changes via email

### 11.3 Version Control
- All versions of this policy are archived with timestamps
- Changes are tracked in a revision log

---

## 12. Contact Information

### For Users
**Data Deletion Requests:** Use "Delete Account" feature in app or email privacy@glowupai.com  
**Data Access Requests:** Use "Export Data" feature in app or email privacy@glowupai.com  
**Retention Questions:** Email privacy@glowupai.com with subject "Retention Policy Inquiry"

### Internal Contacts
**Privacy Officer:** privacy-officer@glowupai.com  
**Legal Team:** legal@glowupai.com  
**Engineering Team:** eng-privacy@glowupai.com

---

## 13. Summary Table: Quick Reference

| Data Type | Retention Period | Deletion Trigger | Backup Retention | Exceptions |
|-----------|------------------|------------------|------------------|------------|
| User account | Account lifetime | User deletes account | 90 days | Consent records: 7 years |
| Facial photos | Account lifetime or photo deletion | User deletes photo/account | 30 days (Firebase) | None |
| Appearance metrics | Tied to photos | Photo/account deletion | 90 days | None |
| Routine data | Account lifetime or entry deletion | User deletes entry/account | 90 days | None |
| Q&A threads | Account lifetime or thread deletion | User deletes thread/account | 90 days | None |
| Firebase Analytics | 14 months (automatic) | Firebase auto-expiration | N/A | Aggregated data: indefinite |
| Firebase Crashlytics | 90 days (automatic) | Firebase auto-expiration | N/A | None |
| Subscription records | Account lifetime | Account deletion | 90 days | Tax records: 7 years (anonymized) |
| Audit logs | 7 years | Automatic expiration | 7 years | Legal disputes: until resolved |

---

## 14. Acknowledgment

This Data Retention Policy is consistent with GlowUp AI's Privacy Policy and Terms of Service. By using GlowUp AI, users acknowledge and agree to the retention periods described in this document.

**Last Updated:** August 30, 2026  
**Version:** 1.0  
**Next Review:** August 30, 2027

---

**END OF DOCUMENT**
