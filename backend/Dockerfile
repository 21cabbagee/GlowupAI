# syntax=docker/dockerfile:1

# =============================================================================
# Stage 1: Builder - Install dependencies
# =============================================================================
FROM python:3.12.14-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# Install build dependencies and security updates
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    gcc \
    python3-dev \
    libgl1 \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies from pyproject.toml
# This ensures versions stay in sync with project requirements
COPY pyproject.toml ./
RUN pip install --user --no-cache-dir \
        "fastapi>=0.115.6" \
        "uvicorn[standard]>=0.34.0" \
        "pillow>=11.0.0" \
        "opencv-python>=4.10.0.84" \
        "numpy>=2.0.0" \
        "cryptography>=44.0.0" \
        "google-genai>=1.0.0" \
        "psycopg[binary,pool]>=3.2.6" \
        "PyJWT[crypto]>=2.10.0" \
        "redis>=5.2.1" \
        "sentry-sdk[fastapi]>=2.20.0"

# =============================================================================
# Stage 2: Runtime - Minimal production image
# =============================================================================
FROM python:3.12.14-slim AS runtime

# Security metadata
LABEL maintainer="GlowupAI Team" \
      security.scan="trivy" \
      security.non-root="true" \
      version="3.0.0"

# Security: Install security updates and runtime dependencies
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    && apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
    # Remove unnecessary setuid/setgid binaries to reduce attack surface
    find / -xdev -perm /6000 -type f -exec chmod a-s {} \; 2>/dev/null || true

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PORT=8000 \
    # Production defaults
    GLOWUPAI_ENV=production \
    GLOWUPAI_LOG_LEVEL=INFO \
    GLOWUPAI_JSON_LOGS=1 \
    GLOWUPAI_RATE_LIMIT_ENABLED=1 \
    GLOWUPAI_REQUEST_TIMEOUT=30

WORKDIR /app

# Copy Python dependencies from builder to shared location
COPY --from=builder /root/.local /usr/local

# Copy application code
COPY glowupai ./glowupai
COPY pyproject.toml ./

# Install application (metadata only, no dependencies)
RUN pip install --no-cache-dir --no-deps .

# Create non-root user and data directory
# User ID 10001 is a common convention for non-root container users
RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin glowupai \
    && mkdir -p /app/.data \
    && chown -R glowupai:glowupai /app

# Switch to non-root user
USER glowupai

# Set PATH for non-root user
ENV PATH=/usr/local/bin:$PATH

# Expose port
EXPOSE 8000

# Health check: polls /api/health endpoint
# - interval: How often to check (30s)
# - timeout: Max time for health check to respond (5s)
# - start-period: Grace period on container start (15s)
# - retries: Consecutive failures needed to mark unhealthy (3)
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD python -c "import os,urllib.request,sys; \
port=os.environ.get('PORT','8000'); \
response = urllib.request.urlopen(f'http://127.0.0.1:{port}/api/health', timeout=3); \
sys.exit(0 if response.status == 200 else 1)"

# Start uvicorn with production settings
# - host 0.0.0.0: Listen on all interfaces
# - port: From PORT env variable
# - workers: Set via UVICORN_WORKERS env var (default: 1)
# - log-config: Disabled, we use our own structured logging
CMD ["sh", "-c", "uvicorn glowupai.complete_api:app --host 0.0.0.0 --port ${PORT} --workers ${UVICORN_WORKERS:-1} --no-access-log"]
