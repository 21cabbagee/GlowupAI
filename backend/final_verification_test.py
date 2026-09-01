#!/usr/bin/env python3
"""Quick verification test of critical endpoints"""
import requests
import json

BASE_URL = "http://localhost:8000"

tests = []

def test(name, method, path, data=None, expected_status=200):
    try:
        if method == "GET":
            r = requests.get(f"{BASE_URL}{path}", timeout=5)
        else:
            r = requests.post(f"{BASE_URL}{path}", json=data or {}, timeout=5)
        
        success = r.status_code == expected_status
        tests.append({
            "name": name,
            "status": r.status_code,
            "expected": expected_status,
            "success": success
        })
        return success
    except Exception as e:
        tests.append({
            "name": name,
            "status": "ERROR",
            "expected": expected_status,
            "success": False,
            "error": str(e)
        })
        return False

# Run tests
print("Testing critical endpoints...\n")

test("Health Check", "GET", "/api/health")
test("Create User", "POST", "/api/users", {})
test("Create User with Name", "POST", "/api/users", {"name": "Test", "focus": "acne"})
test("Search Products", "GET", "/api/products/search?q=moisturizer")

# Print results
print("\nRESULTS:")
print("=" * 60)
passed = sum(1 for t in tests if t["success"])
total = len(tests)

for t in tests:
    status = "✅" if t["success"] else "❌"
    print(f"{status} {t['name']}: {t['status']} (expected {t['expected']})")

print("=" * 60)
print(f"\n{passed}/{total} tests passed ({passed/total*100:.0f}%)")

if passed == total:
    print("\n🎉 ALL TESTS PASSED - READY TO PROCEED!")
    exit(0)
else:
    print("\n⚠️  SOME TESTS FAILED - NEEDS MORE FIXES")
    exit(1)
