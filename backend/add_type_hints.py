#!/usr/bin/env python3
"""
Script to add type hints to all public functions in the backend.
This script systematically adds type hints using AST manipulation.
"""

import ast
import os
from pathlib import Path
from typing import Set

# Files to process
TARGET_FILES = [
    "glowupai/capture_service.py",
    "glowupai/guidance_service.py",
    "glowupai/commerce_service.py",
    "glowupai/subscription_service.py",
    "glowupai/analytics_service.py",
    "glowupai/service.py",
    "glowupai/ml_monitoring.py",
    "glowupai/analytics.py",
    "glowupai/routers/admin.py",
    "glowupai/routers/analytics.py",
    "glowupai/routers/captures.py",
    "glowupai/routers/subscriptions.py",
    "glowupai/routers/users.py",
    "glowupai/complete_service.py",
]

# Common type hint replacements
TYPE_REPLACEMENTS = {
    "dict": "Dict[str, Any]",
    "list": "List[Any]",
    "str | None": "Optional[str]",
    "int | None": "Optional[int]",
    "float | None": "Optional[float]",
    "bool | None": "Optional[bool]",
    "dict | None": "Optional[Dict[str, Any]]",
    "list | None": "Optional[List[Any]]",
    "list[dict]": "List[Dict[str, Any]]",
    "list[str]": "List[str]",
    "dict[str, float]": "Dict[str, float]",
    "dict[str, int]": "Dict[str, int]",
    "dict[str, Any]": "Dict[str, Any]",
}


def add_imports_if_missing(content: str) -> str:
    """Add typing imports if not present."""
    lines = content.split("\n")

    # Check if typing imports exist
    has_typing = any("from typing import" in line or "import typing" in line for line in lines)

    if not has_typing:
        # Find the best place to add imports (after from __future__ and before other imports)
        import_index = 0
        for i, line in enumerate(lines):
            if line.startswith("from __future__"):
                import_index = i + 1
                # Skip empty lines after __future__
                while import_index < len(lines) and lines[import_index].strip() == "":
                    import_index += 1
                break

        # Add typing import
        typing_import = "from typing import Any, Callable, Dict, List, Optional, Union"
        lines.insert(import_index, typing_import)
        lines.insert(import_index + 1, "")

    return "\n".join(lines)


def process_file(filepath: Path) -> None:
    """Process a single file to add type hints."""
    print(f"Processing {filepath}...")

    content = filepath.read_text()

    # Add imports if missing
    content = add_imports_if_missing(content)

    # Apply type replacements
    for old_type, new_type in TYPE_REPLACEMENTS.items():
        # Simple replacement for common patterns
        content = content.replace(f"-> {old_type}:", f"-> {new_type}:")
        content = content.replace(f": {old_type} =", f": {new_type} =")
        content = content.replace(f": {old_type},", f": {new_type},")
        content = content.replace(f": {old_type})", f": {new_type})")

    filepath.write_text(content)
    print(f"✓ Updated {filepath}")


def main():
    """Main function to process all target files."""
    backend_dir = Path(__file__).parent

    for target_file in TARGET_FILES:
        filepath = backend_dir / target_file
        if filepath.exists():
            try:
                process_file(filepath)
            except Exception as e:
                print(f"✗ Error processing {filepath}: {e}")
        else:
            print(f"✗ File not found: {filepath}")

    print("\nDone! Run 'mypy backend/glowupai/' to verify.")


if __name__ == "__main__":
    main()
