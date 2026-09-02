# Security Policy

## Supported Versions

We release patches for security vulnerabilities for the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues.**

If you discover a security vulnerability in GlowupAI, please send an email to the maintainers with:

1. **Description** of the vulnerability
2. **Steps to reproduce** the issue
3. **Potential impact** of the vulnerability
4. **Suggested fix** (if you have one)

### What to expect:

- **Acknowledgment**: We will acknowledge receipt of your vulnerability report within 48 hours
- **Updates**: We will send you regular updates about our progress
- **Timeline**: We aim to address critical vulnerabilities within 7 days
- **Credit**: If you wish, we will credit you in the security advisory

### Security Best Practices for Contributors

When contributing to GlowupAI:

- **Never commit** secrets, API keys, or credentials
- **Use environment variables** for configuration
- **Validate all user inputs** server-side
- **Follow OWASP** security guidelines
- **Keep dependencies** up to date
- **Review security scan results** in CI/CD

## Security Features

GlowupAI implements several security measures:

- ✅ CORS protection configured
- ✅ Rate limiting on API endpoints
- ✅ Input validation and sanitization
- ✅ Secure credential storage
- ✅ HTTPS enforcement in production
- ✅ Security scanning in CI/CD (Trivy, Bandit, Safety)
- ✅ Firebase Authentication
- ✅ ProGuard/R8 code shrinking for Android

## Known Security Considerations

### Development vs Production

- **Development**: Uses localhost origins, debug signing
- **Production**: Requires proper CORS configuration, release signing, environment variables

### Environment Variables

Required production secrets:
- `GLOWUPAI_ADMIN_TOKEN`: Admin API authentication
- `GLOWUPAI_GEMINI_API_KEY`: AI service integration
- `DATABASE_URL`: PostgreSQL connection string
- `REDIS_URL`: Cache connection string (optional)
- `SENTRY_DSN`: Error monitoring (optional)

Never commit these to version control.

## Security Updates

We use:
- **Dependabot**: Automated dependency updates (enabled)
- **GitHub CodeQL**: Static analysis security scanning (enabled)
- **Trivy**: Docker image vulnerability scanning
- **Bandit**: Python security linting
- **Safety**: Python dependency vulnerability scanning

Security updates are released as needed.
