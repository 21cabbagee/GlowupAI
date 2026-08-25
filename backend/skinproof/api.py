"""Default complete application with legacy constructor compatibility."""

import atexit

from .complete_api import app, create_complete_app

_legacy_apps = []
ROADMAP = [
    {"phase": "Measure", "window": "Month 0-3", "status": "shipped", "features": ["guided capture", "quality gates", "baseline history", "routine logging", "deterministic metrics"]},
    {"phase": "Experiment", "window": "Month 3-6", "status": "shipped", "features": ["Premium", "one-variable experiments", "four-state verdicts", "grounded insight copy", "streaks and reminders"]},
    {"phase": "Personalize", "window": "Month 6-12", "status": "shipped", "features": ["ingredient intelligence", "grounded data Q&A", "labels", "model reprocessing", "analytics"]},
    {"phase": "Discover", "window": "Year 2", "status": "shipped", "features": ["minimum-sample cohorts", "affiliate-only commerce", "multi-appearance history"]},
]


def create_app(service=None):
    if service is not None:
        from .api_legacy import create_app as create_legacy_app
        legacy = create_legacy_app(service)
        _legacy_apps.append(legacy)
        return legacy
    return create_complete_app()


@app.get("/api/roadmap", tags=["product"])
def roadmap():
    return {"timeline": ROADMAP, "status": "complete_product_surface"}


@atexit.register
def _close_databases() -> None:
    apps = [app]
    try:
        from . import complete_api
        apps.append(complete_api.app)
    except Exception:
        pass
    try:
        from . import api_legacy
        apps.append(api_legacy.app)
    except Exception:
        pass
    apps.extend(_legacy_apps)
    seen = set()
    for current in apps:
        database = getattr(getattr(current, "state", None), "skinproof", None)
        database = getattr(database, "db", None)
        if database is not None and id(database) not in seen:
            seen.add(id(database))
            database.close()


__all__ = ["app", "create_app", "create_complete_app"]
