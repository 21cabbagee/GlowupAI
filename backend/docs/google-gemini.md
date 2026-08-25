# Google Gemini language provider

SkinProof uses Gemini only for concise, evidence-grounded wording. Local
capture metrics, attribution labels, and medical-scope triage remain
authoritative and continue to work without Google.

## Configure locally

Install the declared dependency and provide the key through the process
environment:

```powershell
python -m pip install -e .
$env:GEMINI_API_KEY = "<your-key>"
$env:SKINPROOF_GEMINI_MODEL = "gemini-3.5-flash-lite"
$env:SKINPROOF_GEMINI_ENABLED = "1"
python -m skinproof.cli serve
```

`SKINPROOF_GEMINI_API_KEY` takes precedence over `GEMINI_API_KEY`. The current
workspace also has a development-only migration bridge that reads the quoted
value in `first.py` without executing the file. It is disabled when
`SKINPROOF_ENV=production` or `SKINPROOF_DISABLE_LEGACY_KEY_FILE=1`.

## Production requirements

Use a secret manager or deployment secret for `GEMINI_API_KEY`; do not put the
key in source control, browser code, logs, database rows, or `.env` files that
are committed. Set `SKINPROOF_ENV=production` so the `first.py` bridge cannot
be used accidentally. Rotate the key if it has ever been shared outside the
trusted deployment boundary.

The provider sends bounded JSON evidence only. It removes keys associated with
raw/image/byte data, and it falls back to the deterministic local language
layer on missing SDKs, quota errors, invalid credentials, or network failures.

The model is configurable with `SKINPROOF_GEMINI_MODEL`; the default is the
current free-tier-compatible `gemini-3.5-flash-lite` configuration used by this
app. Free-tier quotas and eligible models are controlled by Google and can
change, so production should monitor usage and retain the local fallback.

See Google's [API key guide](https://ai.google.dev/gemini-api/docs/generate-content/api-key),
[Generate Content API](https://ai.google.dev/api/generate-content), and
[pricing page](https://ai.google.dev/gemini-api/docs/pricing) when changing the
deployment model or quota plan.
