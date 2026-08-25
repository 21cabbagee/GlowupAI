# syntax=docker/dockerfile:1
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

# --- Dependency layer -------------------------------------------------------
# Installed from a hardcoded list (not `pip install .`) so this layer only
# invalidates when pyproject.toml itself changes, not on every source edit.
# Keep this list in sync with the [project].dependencies array in
# pyproject.toml. Only runtime deps go here -- [project.optional-dependencies]
# "dev" (pytest, httpx) is never installed in the image.
COPY pyproject.toml README.md ./
RUN pip install --no-cache-dir \
        "fastapi>=0.110" \
        "uvicorn[standard]>=0.29" \
        "pillow>=10.0" \
        "cryptography>=42.0" \
        "google-genai>=1.0.0" \
        "psycopg[binary,pool]>=3.2" \
        "PyJWT[crypto]>=2.8"

# --- Application layer -------------------------------------------------------
# Only the app source changes on a normal deploy, so only this and the
# following --no-deps install (metadata only, no resolver work) re-run.
COPY skinproof ./skinproof
RUN pip install --no-cache-dir --no-deps .

# --- Runtime user -------------------------------------------------------
# Non-root: create an unprivileged user and hand it a writable data dir
# (used only for local SQLite fallback / local photo dir; Postgres +
# object storage in production do not need it, but the dir must exist and
# be writable in either case since Settings.prepare() creates it on boot).
RUN useradd --create-home --uid 10001 --shell /usr/sbin/nologin skinproof \
    && mkdir -p /app/.data \
    && chown -R skinproof:skinproof /app
USER skinproof

# Railway and Render both inject PORT at runtime and route traffic to it;
# 8000 is only the local-dev default when PORT is unset.
ENV PORT=8000
EXPOSE 8000

# Uses the stdlib (no extra HTTP client dependency) to hit the same
# /api/health route the platform's own healthcheck polls, so container
# orchestration and platform healthchecks agree on liveness.
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD python -c "import os,urllib.request,sys; \
port=os.environ.get('PORT','8000'); \
urllib.request.urlopen(f'http://127.0.0.1:{port}/api/health', timeout=3).read()" || exit 1

CMD ["sh", "-c", "uvicorn skinproof.api:app --host 0.0.0.0 --port ${PORT}"]
