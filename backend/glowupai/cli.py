from __future__ import annotations

import argparse
import json
import os
import time

import uvicorn


def main() -> None:
    parser = argparse.ArgumentParser(prog="glowupai")
    parser.add_argument(
        "command",
        choices=["serve", "worker", "backup", "verify-backup"],
        nargs="?",
        default="serve",
    )
    parser.add_argument(
        "--once", action="store_true", help="Process one batch of pending jobs"
    )
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8000)
    parser.add_argument("--path", help="Backup artifact path for verify-backup")
    parser.add_argument("--label", default="scheduled", help="Backup artifact label")
    args = parser.parse_args()
    if args.command == "backup":
        from .config import Settings
        from .production_ops import BackupManager

        settings = Settings.from_env()
        settings.validate_for_production()
        print(json.dumps(BackupManager.from_env(settings).create_backup(args.label)))
    elif args.command == "verify-backup":
        if not args.path:
            parser.error("verify-backup requires --path")
        from .config import Settings
        from .production_ops import BackupManager

        settings = Settings.from_env()
        settings.validate_for_production()
        print(
            json.dumps(
                BackupManager.from_env(settings).verify_backup(args.path),
            )
        )
    elif args.command == "serve":
        uvicorn.run("glowupai.api:app", host=args.host, port=args.port, reload=False)
    elif args.command == "worker":
        from .complete_db import build_full_database
        from .complete_service import CompleteGlowupAIService
        from .config import Settings
        from .photos import build_photo_store

        os.environ["GLOWUPAI_JOB_INLINE"] = "0"
        settings = Settings.from_env()
        settings.validate_for_production()
        settings.prepare()
        service = CompleteGlowupAIService(
            build_full_database(settings),
            settings,
            build_photo_store(settings.photo_dir),
        )
        try:
            next_reconciliation = 0.0
            while True:
                service.jobs.run_pending()
                service.deletions.run_pending()
                if time.monotonic() >= next_reconciliation:
                    service.subscription_svc.reconcile_play_purchases()
                    next_reconciliation = time.monotonic() + 300
                if args.once:
                    break
                time.sleep(2)
        except KeyboardInterrupt:
            pass
        finally:
            service.jobs.shutdown()
            service.db.close()


if __name__ == "__main__":
    main()
