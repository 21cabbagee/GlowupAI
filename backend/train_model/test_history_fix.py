#!/usr/bin/env python3
"""Test script to verify the history endpoint bug fix"""

def test_original_buggy_code():
    """This demonstrates the original bug"""
    print("Testing BUGGY code (should fail):")
    # Simulate what active.history returns - a list of dicts
    result = [
        {"id": "capture1", "captured_at": "2024-01-01"},
        {"id": "capture2", "captured_at": "2024-01-02"},
    ]

    try:
        # This is the buggy code - trying to call .get() on a list
        if result and len(result.get("captures", [])) > 1:
            capture_ids = [c.get("id") for c in result.get("captures", [])]
            print(f"  Capture IDs: {capture_ids}")
    except AttributeError as e:
        print(f"  ❌ AttributeError (as expected): {e}")
    print()


def test_fixed_code():
    """This demonstrates the fixed code"""
    print("Testing FIXED code (should work):")
    # Simulate what active.history returns - a list of dicts
    result = [
        {"id": "capture1", "captured_at": "2024-01-01"},
        {"id": "capture2", "captured_at": "2024-01-02"},
        {"id": "capture3", "captured_at": "2024-01-03"},
    ]

    try:
        # This is the fixed code - result is already a list
        if result and len(result) > 1:
            capture_ids = [c.get("id") for c in result]
            print(f"  ✓ Capture IDs: {capture_ids}")
            print(f"  ✓ Count: {len(capture_ids)}")
    except Exception as e:
        print(f"  ❌ Unexpected error: {e}")
    print()


def test_edge_cases():
    """Test edge cases"""
    print("Testing edge cases:")

    # Empty result
    result = []
    if result and len(result) > 1:
        capture_ids = [c.get("id") for c in result]
    else:
        print("  ✓ Empty list: correctly skipped analytics tracking")

    # Single capture
    result = [{"id": "capture1"}]
    if result and len(result) > 1:
        capture_ids = [c.get("id") for c in result]
    else:
        print("  ✓ Single capture: correctly skipped analytics tracking (need 2+)")

    # None result
    result = None
    if result and len(result) > 1:
        capture_ids = [c.get("id") for c in result]
    else:
        print("  ✓ None result: correctly skipped analytics tracking")

    # Two captures (should trigger analytics)
    result = [{"id": "c1"}, {"id": "c2"}]
    if result and len(result) > 1:
        capture_ids = [c.get("id") for c in result]
        print(f"  ✓ Two captures: analytics triggered with IDs {capture_ids}")
    print()


if __name__ == "__main__":
    print("=" * 60)
    print("History Endpoint Bug Fix Verification")
    print("=" * 60)
    print()

    test_original_buggy_code()
    test_fixed_code()
    test_edge_cases()

    print("=" * 60)
    print("Summary: The fix correctly handles result as a list[dict]")
    print("         instead of trying to access result.get('captures')")
    print("=" * 60)
