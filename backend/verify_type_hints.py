#!/usr/bin/env python3
"""Verify type hints have been added to all public functions."""

import ast
from pathlib import Path

# Priority files to check
PRIORITY_FILES = [
    "glowupai/user_service.py",
    "glowupai/capture_service.py",
    "glowupai/guidance_service.py",
    "glowupai/commerce_service.py",
    "glowupai/subscription_service.py",
    "glowupai/analytics_service.py",
    "glowupai/service.py",
    "glowupai/ml_monitoring.py",
    "glowupai/analytics.py",
    "glowupai/complete_service.py",
    "glowupai/routers/admin.py",
    "glowupai/routers/analytics.py",
    "glowupai/routers/captures.py",
    "glowupai/routers/subscriptions.py",
    "glowupai/routers/users.py",
]


def check_file(filepath: Path):
    """Check a single file for type hints."""
    try:
        with open(filepath) as f:
            content = f.read()
            tree = ast.parse(content)
    except Exception as e:
        return {"error": str(e)}

    total_functions = 0
    typed_functions = 0
    missing_functions = []

    for node in ast.walk(tree):
        if isinstance(node, ast.FunctionDef):
            # Skip private methods except __init__
            if node.name.startswith("_") and node.name != "__init__":
                continue

            total_functions += 1
            if node.returns is not None:
                typed_functions += 1
            else:
                missing_functions.append(node.name)

    return {
        "total": total_functions,
        "typed": typed_functions,
        "missing": missing_functions,
        "percentage": (typed_functions / total_functions * 100) if total_functions > 0 else 0,
    }


def main():
    """Main verification function."""
    backend_dir = Path(__file__).parent
    results = {}
    total_all = 0
    typed_all = 0

    print("=" * 80)
    print("TYPE HINTS VERIFICATION REPORT")
    print("=" * 80)
    print()

    for target_file in PRIORITY_FILES:
        filepath = backend_dir / target_file
        if not filepath.exists():
            print(f"✗ {target_file}: FILE NOT FOUND")
            continue

        result = check_file(filepath)
        if "error" in result:
            print(f"✗ {target_file}: ERROR - {result['error']}")
            continue

        total_all += result["total"]
        typed_all += result["typed"]
        results[target_file] = result

        status = "✓" if result["typed"] == result["total"] else "⚠"
        print(f"{status} {target_file}:")
        print(f"  {result['typed']}/{result['total']} functions ({result['percentage']:.1f}%)")

        if result["missing"]:
            print(f"  Missing type hints: {', '.join(result['missing'][:5])}")
            if len(result["missing"]) > 5:
                print(f"    ... and {len(result['missing']) - 5} more")
        print()

    print("=" * 80)
    print(f"TOTAL: {typed_all}/{total_all} functions with type hints ({typed_all/total_all*100:.1f}%)")
    print(f"Missing: {total_all - typed_all} functions")
    print("=" * 80)

    # Exit with error code if not all functions are typed
    if typed_all < total_all:
        print("\n⚠ Some functions are still missing type hints!")
        return 1
    else:
        print("\n✓ All public functions have type hints!")
        return 0


if __name__ == "__main__":
    exit(main())
