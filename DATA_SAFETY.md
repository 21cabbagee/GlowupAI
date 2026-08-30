# Data Safety Section for Google Play Store

**App Name:** GlowUp AI  
**Package Name:** com.glowup.ai  
**Version:** 1.0  
**Last Updated:** August 30, 2026

---

This document provides the information required for the **Data Safety** section of the Google Play Store listing. Copy and adapt this information when filling out the Data Safety form in the Google Play Console.

---

## Overview Statement

**GlowUp AI is a cosmetic skincare tracking app that collects and processes facial photographs to generate appearance metrics for cosmetic tracking purposes only. We use Firebase services for authentication, storage, and analytics, and Google Gemini API for AI-powered features. Your data is encrypted in transit and at rest. You can export or delete your data at any time.**

---

## 1. Data Collection Summary

### Does your app collect or share user data?
**YES** — GlowUp AI collects personal data, facial images, and usage information to provide skincare tracking services.

---

## 2. Data Types Collected

### 2.1 Personal Information

#### **Name**
- **Collected:** YES (optional)
- **Purpose:** App functionality (display name in profile)
- **Shared:** NO
- **Optional:** YES
- **User Control:** Can be edited or deleted by the user

#### **Email Address**
- **Collected:** YES (required)
- **Purpose:** Account management, authentication
- **Shared:** YES — with Firebase Authentication (Google)
- **Optional:** NO (required for account creation)
- **User Control:** Cannot be changed once set (tied to Firebase account)

#### **User IDs**
- **Collected:** YES (automatically generated)
- **Purpose:** Account management, data association
- **Shared:** NO
- **Optional:** NO (automatically generated)
- **User Control:** Cannot be changed (internal identifier)

---

### 2.2 Photos and Videos

#### **Photos**
- **Collected:** YES (required for core functionality)
- **Purpose:** App functionality — cosmetic appearance tracking, metric generation
- **Shared:** YES — stored in Firebase Storage (Google); processed by backend API
- **Optional:** NO (required for photo tracking features, but you can decline consent and use the app without photos)
- **User Control:** Can be deleted individually or all at once by deleting the account
- **Encryption:** Encrypted in transit (HTTPS/TLS) and at rest (Firebase Storage encryption)

---

### 2.3 Health & Fitness

#### **Health Info** (Appearance Metrics)
- **Collected:** YES (derived from photos)
- **Purpose:** App functionality — cosmetic appearance tracking (redness, blemishes, texture, dark spots)
- **Shared:** NO (stored in backend database only, not shared with third parties)
- **Optional:** NO (automatically generated from uploaded photos)
- **User Control:** Can be deleted by deleting photos or account
- **Note:** Metrics are cosmetic assessments only, not medical diagnoses

#### **Fitness Info** (NOT APPLICABLE)
- **Collected:** NO

---

### 2.4 App Activity

#### **App Interactions**
- **Collected:** YES
- **Purpose:** Analytics, app functionality
- **Shared:** YES — with Firebase Analytics (Google)
- **Optional:** NO (automatically collected)
- **User Control:** Cannot be disabled individually (part of core analytics)
- **Examples:** Features accessed, buttons clicked, screens viewed

#### **In-App Search History**
- **Collected:** YES (product searches, Q&A questions)
- **Purpose:** App functionality — to provide search results, AI responses
- **Shared:** YES — Q&A questions are sent to Google Gemini API for processing
- **Optional:** YES (only if you use search or Q&A features)
- **User Control:** Can be deleted by deleting account; Q&A threads can be deleted individually

#### **Other User-Generated Content**
- **Collected:** YES (product logs, routine events, experiment notes, context logs)
- **Purpose:** App functionality — skincare routine tracking
- **Shared:** NO (stored in backend database only)
- **Optional:** YES (you choose what to log)
- **User Control:** Can be edited or deleted by the user

---

### 2.5 App Info and Performance

#### **Crash Logs**
- **Collected:** YES
- **Purpose:** Analytics, bug fixing
- **Shared:** YES — with Firebase Crashlytics (Google)
- **Optional:** NO (automatically collected when app crashes)
- **User Control:** Cannot be disabled

#### **Diagnostics**
- **Collected:** YES (app version, device type, OS version, network info)
- **Purpose:** Analytics, bug fixing, app functionality
- **Shared:** YES — with Firebase Analytics and Crashlytics (Google)
- **Optional:** NO (automatically collected)
- **User Control:** Cannot be disabled

#### **Other App Performance Data**
- **Collected:** YES (session duration, feature usage)
- **Purpose:** Analytics, performance monitoring
- **Shared:** YES — with Firebase Analytics (Google)
- **Optional:** NO (automatically collected)
- **User Control:** Cannot be disabled

---

### 2.6 Device or Other IDs

#### **Device or Other IDs**
- **Collected:** YES (Firebase instance ID, device identifiers)
- **Purpose:** App functionality, analytics, fraud prevention
- **Shared:** YES — with Firebase services (Google)
- **Optional:** NO (automatically collected)
- **User Control:** Cannot be disabled

---

### 2.7 Financial Info

#### **Purchase History**
- **Collected:** YES (subscription plan, status, renewal date)
- **Purpose:** App functionality — subscription management
- **Shared:** NO (subscription data is stored in backend; payment processing is handled entirely by Google Play Store)
- **Optional:** NO (if you subscribe to Premium)
- **User Control:** Can view and cancel subscriptions via Google Play Store settings
- **Note:** We do NOT collect or store credit card numbers or payment credentials

#### **Other Financial Info**
- **Collected:** NO

---

### 2.8 Location

#### **Approximate Location**
- **Collected:** NO

#### **Precise Location**
- **Collected:** NO

**Note:** GlowUp AI does NOT collect any location data.

---

### 2.9 Other Data Types

#### **Files and Docs**
- **Collected:** NO (except photos uploaded by the user, already covered in "Photos")

#### **Calendar**
- **Collected:** NO

#### **Contacts**
- **Collected:** NO

#### **Audio**
- **Collected:** NO

#### **Music and Other Audio Files**
- **Collected:** NO

#### **Web Browsing History**
- **Collected:** NO

#### **Messages**
- **Collected:** NO (Q&A threads are covered under "In-App Search History")

---

## 3. Data Usage Purposes

### App Functionality
- Account management (email, user ID)
- Photo capture and storage (photos)
- Appearance metric generation (photos → metrics)
- Routine and product tracking (user-generated content)
- AI-powered Q&A (search history sent to Gemini API)
- Subscription management (purchase history)

### Analytics
- Firebase Analytics (app interactions, device IDs, diagnostics)
- Usage pattern analysis to improve the app

### Developer Communications
- Email for account-related notifications, support responses, policy updates

### Fraud Prevention, Security, and Compliance
- Device IDs for fraud detection
- Crash logs and diagnostics for security monitoring

### Advertising or Marketing
- **NOT APPLICABLE** — GlowUp AI does NOT use data for advertising or marketing purposes

### Personalization
- Display name, profile settings, routine data for personalized experience

---

## 4. Data Sharing

### Third-Party Service Providers We Share Data With:

#### **Google Firebase** (Service Provider)
- **Data Shared:** Email, device IDs, app interactions, crash logs, diagnostics, photos (via Firebase Storage)
- **Purpose:** Authentication, analytics, crash reporting, photo storage
- **Data Handling:** Firebase complies with Google's privacy policies and industry-standard security practices
- **Privacy Policy:** https://firebase.google.com/support/privacy

#### **Google Gemini API** (Service Provider)
- **Data Shared:** User questions in Q&A feature, product barcode images for OCR
- **Purpose:** AI-powered skincare Q&A, product scanning
- **Data Handling:** Gemini API processes requests and returns responses; data is not used to train models (per Google's enterprise API terms)
- **Privacy Policy:** https://policies.google.com/privacy

#### **Railway / Cloud Hosting Provider** (Service Provider)
- **Data Shared:** All backend data (user profiles, photos, metrics, routine data)
- **Purpose:** Backend API hosting and database storage
- **Data Handling:** Infrastructure provider with encryption and standard security practices

### We Do NOT Share Data For:
- Advertising or marketing
- Sale to data brokers
- Analytics by third parties (beyond Firebase Analytics)
- Social media platforms
- Other apps or services not listed above

---

## 5. Security Practices

### Data Encryption
- **In Transit:** YES — All data transmitted between the app and backend uses HTTPS/TLS encryption
- **At Rest:** YES — Photos stored in Firebase Storage are encrypted at rest; backend database uses encryption

### User Data Controls
- **Export Data:** YES — Users can export all their data (photos, metrics, routine history) as a JSON file via the "Export Data" feature in Account settings
- **Delete Data:** YES — Users can delete individual photos, routine entries, or their entire account (full deletion within 90 days)
- **Request Data Deletion:** YES — Users can email privacy@glowupai.com to request data deletion

### Account Deletion
- **Available:** YES — "Delete Account" button in Account settings
- **Effect:** Permanently deletes user profile, all photos, metrics, and routine data. Deletion is immediate from the app; complete removal from backups occurs within 90 days.

---

## 6. Data Retention and Deletion

### Retention Policy
- **Account Data:** Retained until the user deletes their account
- **Photos & Metrics:** Retained until the user deletes them or deletes their account
- **Routine & Product Data:** Retained until the user deletes entries or deletes their account
- **Analytics Data:** Anonymized aggregated data may be retained indefinitely
- **Backup Systems:** Deleted data may persist in backups for up to 90 days before permanent deletion

### User-Initiated Deletion
- Users can delete their account at any time via the app's Account settings
- Users can delete individual photos and routine entries
- Users can email privacy@glowupai.com to request data deletion

---

## 7. Data Collection Disclosure

### Is data collected optional?
- **Some data is required:** Email, user ID, device IDs (for account functionality and analytics)
- **Some data is optional:** Display name, photos (you can decline consent to facial data processing and use the app without photo tracking), routine logs, Q&A usage

### Can users request data deletion?
- **YES** — via in-app "Delete Account" feature or by emailing privacy@glowupai.com

---

## 8. Compliance Statements

### Independent Security Review
- **Completed:** In progress (mention if you've completed any security audits)
- **Reviewer:** [Insert third-party security firm name if applicable, or mark as "Planned"]

### COPPA Compliance (Children's Privacy)
- **Target Audience:** 18 years and older
- **Children Under 13:** App is NOT directed at children under 13; we do not knowingly collect data from children under 13
- **Age Verification:** Users must confirm they are 18+ during account creation

### Family Policy Compliance
- **Target Age Group:** Adults 18+
- **Not a Family App:** GlowUp AI is not designed for or marketed to families or children

---

## 9. Play Store Data Safety Questionnaire Answers

Use these answers when filling out the Google Play Console Data Safety form:

### Question: Does your app collect or share user data?
**Answer:** YES

### Question: Is all of the user data collected by your app encrypted in transit?
**Answer:** YES

### Question: Do you provide a way for users to request that their data is deleted?
**Answer:** YES

### Question: Will the app's services and/or features work as expected if users decline permission for all optional data types?
**Answer:** PARTIALLY — Users can use the app for product tracking without uploading photos, but photo-based appearance tracking requires photo upload consent.

### Question: Are children under 13 part of your target audience?
**Answer:** NO

### Question: Is your app subject to a family policy?
**Answer:** NO

---

## 10. Additional Context for Reviewers

### Why We Collect Facial Photos
GlowUp AI's core feature is cosmetic appearance tracking. Users voluntarily upload facial photos to track changes in their skin over time. Photos are:
- Processed to generate cosmetic appearance metrics (redness, blemishes, texture)
- Stored securely in Firebase Storage with encryption
- Never shared with third parties for advertising or marketing
- Fully under user control (users can delete photos or their account at any time)

### Cosmetic Tracking, Not Medical Use
GlowUp AI is a cosmetic tracking tool, NOT a medical device. We do not diagnose, treat, or provide medical advice. All metrics are for personal cosmetic tracking only. This is clearly disclosed in our Terms of Service and Medical Disclaimer.

### Firebase and Gemini API Usage
We use Google Firebase for authentication, storage, and analytics because it provides industry-standard security and compliance. Gemini API powers optional AI features (Q&A, product scanning) and is used per Google's enterprise API terms.

---

## 11. Summary Table for Quick Reference

| Data Type | Collected? | Purpose | Shared? | User Control |
|-----------|------------|---------|---------|--------------|
| Email | YES | Account management | Firebase Auth | Cannot change (tied to Firebase) |
| Display Name | YES (optional) | Profile display | NO | Editable/deletable |
| Photos | YES | Appearance tracking | Firebase Storage | Deletable |
| Appearance Metrics | YES (derived) | Tracking trends | NO | Deletable (with photos) |
| Product Logs | YES (optional) | Routine tracking | NO | Editable/deletable |
| Q&A Queries | YES (optional) | AI responses | Gemini API | Deletable |
| App Interactions | YES | Analytics | Firebase Analytics | Cannot disable |
| Crash Logs | YES | Bug fixing | Firebase Crashlytics | Cannot disable |
| Device IDs | YES | Fraud prevention | Firebase | Cannot disable |
| Subscription Status | YES (if Premium) | Subscription mgmt | NO | View/cancel via Play Store |
| Location | NO | N/A | N/A | N/A |
| Contacts | NO | N/A | N/A | N/A |

---

## 12. Privacy Policy and Contact

**Privacy Policy:** [Insert Play Store Link or Website URL when published]

**Contact Email:** privacy@glowupai.com

**Developer Name:** GlowUp AI

**Developer Address:** [Insert Physical Address]

---

## 13. Checklist Before Submission

Before submitting to the Google Play Store, ensure:

- [ ] Privacy Policy is published and accessible (link in Play Store listing)
- [ ] Terms of Service and Medical Disclaimer are published and accessible
- [ ] Data Safety section is fully and accurately filled out
- [ ] In-app consent flow explicitly asks for facial data processing consent
- [ ] "Export Data" and "Delete Account" features are fully functional
- [ ] All third-party SDKs (Firebase, Gemini API) are up to date
- [ ] App does not collect data from users under 18
- [ ] Age verification is implemented (user confirms they are 18+ during sign-up)
- [ ] Medical disclaimer is displayed prominently during onboarding
- [ ] Metrics and verdicts carry the cosmetic-only disclaimer in the UI
- [ ] All data transmissions use HTTPS/TLS

---

## 14. Notes for Play Store Console

When filling out the Data Safety form in the Play Store Console:

1. **Be thorough and accurate** — Inaccurate disclosures can result in app rejection or removal
2. **Select all applicable data types** — Better to over-disclose than under-disclose
3. **Explain "ephemeral" data** — If any data is not stored (e.g., photo bytes are processed but not stored), mark it as ephemeral and explain in the notes
4. **Update after changes** — If you add new data collection in future versions, update the Data Safety section before releasing the update
5. **Link to privacy policy** — The Play Store requires a publicly accessible Privacy Policy link

---

**This document should be reviewed by legal counsel before submission to ensure compliance with Google Play policies and applicable privacy laws (GDPR, CCPA, etc.).**

---

**Last Updated:** August 30, 2026  
**Version:** 1.0  
**Prepared for:** Google Play Store Data Safety Section
