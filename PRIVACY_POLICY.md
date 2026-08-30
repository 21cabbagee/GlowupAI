# Privacy Policy for GlowUp AI

**Effective Date:** August 30, 2026  
**Last Updated:** August 30, 2026

## 1. Introduction

GlowUp AI ("we," "us," or "our") respects your privacy and is committed to protecting your personal data. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use the GlowUp AI mobile application and related services (collectively, the "Service").

**IMPORTANT: GlowUp AI is a cosmetic tracking tool, not a medical device or diagnostic tool.** We do not provide medical advice, diagnoses, or treatment recommendations. All metrics and insights are for cosmetic tracking purposes only.

By using GlowUp AI, you agree to the collection and use of information in accordance with this Privacy Policy. If you do not agree with our policies and practices, please do not use the Service.

## 2. Information We Collect

### 2.1 Personal Information You Provide

- **Account Information:** When you create an account via Firebase Authentication, we collect:
  - Email address
  - Display name (optional)
  - Firebase authentication credentials
  
- **Profile Information:** You may provide:
  - Skin type (e.g., dry, oily, combination)
  - Skincare goals
  - Experience level with skincare products
  - Focus areas for tracking

### 2.2 Facial Image Data

- **Photographs:** You voluntarily capture and upload facial photographs through the app for cosmetic tracking purposes. These photos are:
  - Stored securely in Firebase Storage (glowup-ai-38ae7.firebasestorage.app)
  - Processed to extract cosmetic appearance metrics
  - Never shared with third parties except as described in this policy
  - Retained for the duration specified in Section 6

- **Pose & Quality Data:** When capturing photos, we collect:
  - Face detection data (position, angle, distance from camera)
  - Image quality metrics (brightness, sharpness, expression analysis)
  - Capture timestamp and device metadata
  - These data points ensure consistent photo quality for meaningful comparisons

### 2.3 Analyzed Appearance Metrics

From your uploaded photos, we automatically derive and store:
- Redness scores
- Blemish counts
- Dark spot measurements
- Texture scores
- Confidence ratings for each metric
- Historical trends and deltas over time

**These metrics are cosmetic assessments only and do not constitute medical diagnoses.**

### 2.4 Skincare Routine Data

- **Products:** Names, categories, ingredients, barcodes of skincare products you track
- **Routine Events:** Application times, frequencies, and usage patterns
- **Experiments:** Product changes you're testing and their tracked outcomes
- **Context Logs:** Optional lifestyle factors (sleep quality, stress levels, diet) that you choose to log

### 2.5 Usage & Interaction Data

- **In-App Activity:**
  - Features accessed
  - Q&A conversations with our AI assistant
  - Streaks and achievements earned
  - Feedback you provide on measurements
  - Commerce interactions (offers viewed, products discovered)

- **Analytics Data:** Via Firebase Analytics:
  - App opens, session duration
  - Feature usage patterns
  - Navigation flows
  - Performance metrics

- **Technical Data:**
  - Device type and operating system version
  - App version
  - Crash reports and diagnostic data (via Firebase Crashlytics)
  - Network information (for API connectivity)

### 2.6 Subscription & Payment Information

- Subscription plan (Free or Premium)
- Subscription status, start date, and renewal date
- Payment source (e.g., "google_play")
- **Note:** Payment details (credit card numbers) are handled entirely by Google Play Store; we never receive or store your payment credentials

## 3. How We Use Your Information

We use the collected information for the following purposes:

### 3.1 Service Provision
- Create and maintain your account
- Process and analyze facial photos for cosmetic metrics
- Generate appearance trends and insights
- Power product verdict features
- Enable AI-powered Q&A assistance
- Deliver personalized skincare tracking experiences

### 3.2 Service Improvement
- Analyze usage patterns to improve features
- Develop and refine our appearance analysis algorithms
- Identify and fix bugs through crash reports
- Optimize app performance

### 3.3 Communication
- Send important service updates
- Respond to your support requests
- Provide guidance on feature usage
- Notify you about subscription status changes

### 3.4 Legal & Safety
- Enforce our Terms of Service
- Comply with legal obligations
- Protect against fraud or abuse
- Defend our legal rights

## 4. How We Share Your Information

We do not sell your personal information. We share information only in the following limited circumstances:

### 4.1 Service Providers

We share data with trusted third-party service providers who assist in operating our Service:

- **Google Firebase Services:**
  - Firebase Authentication (user identity management)
  - Firebase Storage (secure photo storage)
  - Firebase Analytics (usage analytics)
  - Firebase Crashlytics (crash reporting)
  - **Privacy Policy:** https://firebase.google.com/support/privacy

- **Google Gemini API:**
  - Powers product barcode scanning (OCR)
  - Provides AI-driven Q&A responses
  - Processes product ingredient analysis
  - **Privacy Policy:** https://policies.google.com/privacy

- **Backend Infrastructure:**
  - Railway.app or similar cloud hosting providers (for our Python/FastAPI backend)
  - PostgreSQL database hosting
  - These providers have access only to data necessary for hosting operations

### 4.2 Legal Requirements

We may disclose your information if required by law, subpoena, court order, or other legal process, or if we believe in good faith that disclosure is necessary to:
- Comply with legal obligations
- Protect the rights, property, or safety of GlowUp AI, our users, or the public
- Investigate potential violations of our Terms of Service
- Prevent fraud or security threats

### 4.3 Business Transfers

In the event of a merger, acquisition, reorganization, bankruptcy, or sale of assets, your information may be transferred to the successor entity. You will be notified of any such change via email and/or prominent notice in the app.

### 4.4 With Your Consent

We may share information for purposes not described in this policy with your explicit consent.

## 5. International Data Transfers

GlowUp AI operates globally, and your information may be transferred to and processed in countries other than your country of residence, including the United States. These countries may have data protection laws that differ from your jurisdiction.

When we transfer your personal data internationally, we implement appropriate safeguards, including:
- Standard contractual clauses approved by relevant authorities
- Ensuring service providers are certified under recognized privacy frameworks
- Other legally accepted mechanisms for international transfers

## 6. Data Retention

We retain your information for as long as necessary to provide the Service and fulfill the purposes described in this policy:

- **Account & Profile Data:** Retained until you delete your account
- **Facial Photographs:** Retained until you delete them individually or delete your account
- **Appearance Metrics:** Retained until you delete your account (metrics are derived from your photos and track your progress over time)
- **Routine & Product Data:** Retained until you delete individual entries or delete your account
- **Analytics Data:** Aggregated analytics may be retained indefinitely in anonymized form
- **Backup Systems:** Deleted data may remain in backup systems for up to 90 days before permanent deletion

After account deletion, we will permanently delete or anonymize your personal information within 90 days, except where we are required to retain data for legal, tax, or audit purposes.

## 7. Your Privacy Rights

Depending on your jurisdiction, you may have the following rights regarding your personal data:

### 7.1 Access & Portability
- **Export Your Data:** Use the "Export Data" feature in the app's Account settings to download a complete JSON file containing all your data, including profile, consent history, photos, metrics, routine events, experiments, Q&A threads, and engagement data.
- **Request a Copy:** Contact us at privacy@glowupai.com to request a human-readable summary of your data.

### 7.2 Correction & Update
- Update your profile information, skin type, goals, and display name directly in the app's Account settings.
- Contact us to correct inaccurate data you cannot modify yourself.

### 7.3 Deletion & Right to be Forgotten
- **Delete Individual Photos:** Remove specific captures from your history at any time.
- **Delete Products/Routine Events:** Remove individual products or routine entries as needed.
- **Delete Your Account:** Use the "Delete Account" option in Account settings. This action:
  - Permanently deletes your user profile, all photos, metrics, and routine data
  - Cannot be undone
  - Takes effect immediately, with complete removal from backups within 90 days

### 7.4 Withdraw Consent
- You initially grant consent to facial data processing when setting up your account.
- You can withdraw consent at any time by deleting your account.
- Note: Withdrawing consent means you can no longer use photo-based tracking features.

### 7.5 Object to Processing
- You may object to certain processing activities by contacting privacy@glowupai.com.

### 7.6 Restrict Processing
- Request restriction of processing in certain circumstances (e.g., while we verify data accuracy).

### 7.7 Lodge a Complaint
- You have the right to lodge a complaint with a data protection authority in your jurisdiction if you believe we have violated your privacy rights.

**For EU residents:** Contact your national data protection authority.  
**For California residents:** See Section 9 for CCPA-specific rights.

## 8. GDPR Compliance (For EU/EEA Users)

If you are located in the European Union or European Economic Area, the General Data Protection Regulation (GDPR) provides additional rights and protections.

### 8.1 Legal Basis for Processing

We process your personal data under the following legal bases:

- **Consent:** You explicitly consent to facial data processing when you accept our terms during onboarding. You can withdraw consent by deleting your account.
- **Contract Performance:** Processing is necessary to provide the Service you've requested (account management, photo analysis, routine tracking).
- **Legitimate Interests:** We have legitimate interests in improving our Service, preventing fraud, and ensuring security, provided these interests do not override your fundamental rights.

### 8.2 Data Controller

GlowUp AI is the data controller for your personal information. Contact us at:
- Email: privacy@glowupai.com

### 8.3 Your GDPR Rights

In addition to the rights listed in Section 7, you have the right to:
- Receive transparent information about how we use your data
- Be informed of any data breaches that may affect you
- Restrict automated decision-making (note: our appearance metrics are automated but used for cosmetic tracking only, not for legal/significant decisions)

## 9. CCPA Compliance (For California Residents)

If you are a California resident, the California Consumer Privacy Act (CCPA) provides additional rights.

### 9.1 Categories of Personal Information We Collect

In the past 12 months, we have collected the following categories of personal information:

| Category | Examples | Collected |
|----------|----------|-----------|
| Identifiers | Email, user ID, device ID | Yes |
| Biometric Information | Facial photographs, facial geometry (pose data) | Yes |
| Commercial Information | Subscription status, product purchases tracked | Yes |
| Internet/Network Activity | App usage, features accessed, clicks | Yes |
| Geolocation Data | (Not collected) | No |
| Sensory Data | Photographs | Yes |
| Professional/Employment Info | (Not collected) | No |
| Inferences | Skincare insights, appearance trends | Yes |

### 9.2 Sources of Personal Information

We collect personal information from:
- Directly from you (account creation, photo uploads, profile inputs)
- Automatically from your device (usage analytics, device metadata)
- From third parties (Firebase authentication tokens)

### 9.3 Business/Commercial Purpose for Collection

We use personal information for the purposes described in Section 3 (service provision, improvement, communication, legal compliance).

### 9.4 Third Parties We Share Information With

We share personal information with the service providers listed in Section 4.1. We do not sell personal information.

### 9.5 Your CCPA Rights

California residents have the right to:

- **Right to Know:** Request disclosure of what personal information we collect, use, disclose, and sell (up to twice per year).
- **Right to Delete:** Request deletion of your personal information, subject to certain exceptions.
- **Right to Opt-Out of Sale:** We do not sell personal information, so there is nothing to opt out of.
- **Right to Non-Discrimination:** We will not discriminate against you for exercising your CCPA rights (no denial of service, different pricing, or lower service quality).

To exercise these rights, email privacy@glowupai.com with "CCPA Request" in the subject line or use the in-app "Export Data" and "Delete Account" features.

### 9.6 Verification Process

To protect your privacy, we will verify your identity before fulfilling requests. We may ask you to:
- Confirm your email address
- Provide your user ID
- Answer security questions about your account

### 9.7 Authorized Agents

You may designate an authorized agent to make CCPA requests on your behalf by providing written authorization.

## 10. Security Measures

We implement industry-standard security measures to protect your information:

### 10.1 Technical Safeguards
- **Encryption in Transit:** All data transmitted between the app and our servers uses HTTPS/TLS encryption.
- **Encryption at Rest:** Facial photos are stored in Firebase Storage with encryption at rest.
- **Authentication:** Firebase Authentication with secure token management.
- **Access Controls:** Backend API enforces user-scoped access controls; you can only access your own data.

### 10.2 Organizational Safeguards
- Limited employee access to personal data (need-to-know basis only)
- Regular security audits and penetration testing
- Incident response procedures for data breaches

### 10.3 Your Responsibility
- Keep your account credentials secure
- Use a strong, unique password
- Do not share your account with others
- Log out on shared devices

**However, no method of transmission over the internet or electronic storage is 100% secure.** While we strive to protect your personal information, we cannot guarantee absolute security.

## 11. Children's Privacy

GlowUp AI is not intended for use by anyone under the age of 18. We do not knowingly collect personal information from children under 18.

If you are a parent or guardian and believe your child has provided us with personal information, please contact us at privacy@glowupai.com. We will promptly delete such information from our systems.

## 12. Cookies and Tracking Technologies

### 12.1 Mobile App

The GlowUp AI mobile app does not use traditional browser cookies. However, we use similar tracking technologies:

- **Firebase Analytics SDK:** Collects usage analytics via mobile identifiers
- **Local Storage:** DataStore and Room database for offline functionality and caching
- **Session Tokens:** Firebase authentication tokens stored locally for secure API access

### 12.2 Web Interface (If Applicable)

If you access GlowUp AI via a web interface, we may use:
- **Strictly Necessary Cookies:** Session management, authentication
- **Analytics Cookies:** Google Analytics or Firebase Analytics (anonymized)
- **Performance Cookies:** To monitor and improve site performance

You can control cookies through your browser settings, but disabling certain cookies may limit functionality.

## 13. Third-Party Links

The Service may contain links to third-party websites, services, or content (e.g., product recommendations, educational resources). We are not responsible for the privacy practices of these third parties. We encourage you to review their privacy policies before providing any personal information.

## 14. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect changes in our practices, technology, legal requirements, or other factors. When we make material changes, we will:

- Update the "Last Updated" date at the top of this policy
- Notify you via email (if you've provided an email address)
- Display a prominent notice in the app upon your next login
- Request renewed consent if required by law

Your continued use of the Service after changes become effective constitutes your acceptance of the updated policy.

## 15. Data Breach Notification

In the unlikely event of a data breach that affects your personal information, we will:
- Notify affected users within 72 hours of becoming aware (as required by GDPR)
- Provide details about what information was compromised
- Explain the steps we're taking to address the breach
- Offer guidance on how you can protect yourself
- Notify relevant data protection authorities as required by law

## 16. Contact Us

If you have questions, concerns, or requests regarding this Privacy Policy or our data practices, please contact us:

**Email:** privacy@glowupai.com  
**Subject Line:** Include "Privacy Inquiry" for general questions or "Data Request" for access/deletion requests

**Mailing Address:**  
GlowUp AI  
[Insert Physical Address]  
[City, State, ZIP Code]

**Response Time:** We aim to respond to all inquiries within 30 days (or as required by applicable law, whichever is sooner).

---

## 17. Summary of Key Points

- **Cosmetic Use Only:** GlowUp AI is not a medical device; all metrics are for cosmetic tracking only.
- **Facial Data:** You voluntarily upload facial photos, which are securely stored and analyzed for appearance metrics.
- **No Sale of Data:** We do not sell your personal information to third parties.
- **Third-Party Services:** We use Firebase (Google) and Gemini API for service delivery.
- **Your Rights:** Access, export, correct, and delete your data at any time.
- **Security:** We use encryption and access controls to protect your information.
- **Retention:** Data is retained until you delete your account, then removed within 90 days.
- **GDPR & CCPA Compliant:** We honor EU and California privacy rights.
- **Children:** Not for use by anyone under 18.

---

**By using GlowUp AI, you acknowledge that you have read, understood, and agree to this Privacy Policy.**
