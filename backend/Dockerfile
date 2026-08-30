# syntax=docker/dockerfile:1

# =============================================================================
# Stage 1: Builder - Install dependencies
# =============================================================================
FROM python:3.12-slim AS builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# Install build dependencies (needed for some Python packages)
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    python3-dev \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
# Hardcoded list ensures layer only invalidates when dependencies change
COPY pyproject.toml README.md ./
RUN pip install --user --no-cache-dir \
        "fastapi>=0.110" \
        "uvicorn[standard]>=0.29" \
        "pillow>=10.0" \
        "cryptography>=42.0" \
        "google-genai>=1.0.0" \
        "psycopg[binary,pool]>=3.2" \
        "PyJWT[crypto]>=2.8"

# =============================================================================
# Stage 2: Runtime - Minimal production image
# =============================================================================
FROM python:3.12-slim AS runtime

# Security: Install security updates
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PORT=8000 \
    # Production defaults
    SKINPROOF_ENV=production \
    SKINPROOF_LOG_LEVEL=INFO \
    SKINPROOF_JSON_LOGS=1 \
    SKINPROOF_RATE_LIMIT_ENABLED=1 \
    SKINPROOF_REQUEST_TIMEOUT=30

WORKDIR /app

# Copy Python dependencies from builder
COPY --from=builder /root/.local /root/.local
ENV PATH=/root/.local/bin:$PATH

# Copy application code
COPY skinproof ./skinproof
COPY pyproject.toml README.md ./

# Install application (metadata only, no dependencies)
RUN pip install --no-cache-dir --no-deps .

# Create non-root user and data directory
# User ID 10001 is a common convention for non-root container users
RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin skinproof \
    && mkdir -p /app/.data \
    && chown -R skinproof:skinproof /app

# Switch to non-root user
USER skinproof

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
CMD ["sh", "-c", "uvicorn skinproof.api:app --host 0.0.0.0 --port ${PORT} --workers ${UVICORN_WORKERS:-1} --no-access-log"]
