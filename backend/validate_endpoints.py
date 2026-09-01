"""
Endpoint Validation Script - Router Refactoring
Validates all 69 endpoints across 5 routers are registered correctly.
"""

import sys
from collections import defaultdict

from glowupai.routers import (
    users_router,
    captures_router,
    analytics_router,
    subscriptions_router,
    admin_router
)


def extract_routes_from_router(router):
    """Extract all routes from a router."""
    routes = []
    for route in router.routes:
        if hasattr(route, 'methods') and hasattr(route, 'path'):
            for method in route.methods:
                if method != 'HEAD':  # Skip HEAD methods
                    routes.append({
                        'method': method,
                        'path': route.path,
                        'name': route.name if hasattr(route, 'name') else 'unknown'
                    })
    return routes


def validate_routers():
    """Validate all routers and their endpoints."""

    routers = {
        'Users': {'router': users_router, 'expected': 8},
        'Captures': {'router': captures_router, 'expected': 16},
        'Analytics': {'router': analytics_router, 'expected': 8},
        'Subscriptions': {'router': subscriptions_router, 'expected': 21},
        'Admin': {'router': admin_router, 'expected': 16},
    }

    print("=" * 80)
    print("ROUTER REFACTORING VALIDATION REPORT")
    print("=" * 80)
    print()

    total_routes = 0
    total_expected = 0
    all_passed = True

    for router_name, config in routers.items():
        router = config['router']
        expected = config['expected']
        routes = extract_routes_from_router(router)

        total_routes += len(routes)
        total_expected += expected

        status = "✓ PASS" if len(routes) == expected else "✗ FAIL"
        if len(routes) != expected:
            all_passed = False

        print(f"{router_name} Router: {status}")
        print(f"  Expected: {expected} endpoints")
        print(f"  Found:    {len(routes)} endpoints")
        print(f"  Prefix:   {router.prefix}")
        print()

        # Group by method
        by_method = defaultdict(list)
        for route in routes:
            by_method[route['method']].append(route['path'])

        for method in sorted(by_method.keys()):
            paths = by_method[method]
            print(f"  {method} ({len(paths)} endpoints):")
            for path in sorted(paths):
                print(f"    {path}")
        print()

    print("=" * 80)
    print("SUMMARY")
    print("=" * 80)
    print(f"Total Endpoints Expected: {total_expected}")
    print(f"Total Endpoints Found:    {total_routes}")
    print(f"Status: {'✓ ALL ROUTERS VALID' if all_passed else '✗ VALIDATION FAILED'}")
    print("=" * 80)

    return 0 if all_passed else 1


def list_all_endpoints():
    """List all endpoints in detail."""

    routers = {
        'Users': users_router,
        'Captures': captures_router,
        'Analytics': analytics_router,
        'Subscriptions': subscriptions_router,
        'Admin': admin_router,
    }

    print("\n" + "=" * 80)
    print("COMPLETE ENDPOINT LIST (69 endpoints)")
    print("=" * 80)
    print()

    endpoint_number = 1
    for router_name, router in routers.items():
        routes = extract_routes_from_router(router)

        print(f"\n{router_name} Router ({len(routes)} endpoints):")
        print("-" * 80)

        for route in sorted(routes, key=lambda x: (x['path'], x['method'])):
            print(f"{endpoint_number:3d}. {route['method']:7s} {route['path']}")
            endpoint_number += 1

    print("\n" + "=" * 80)


if __name__ == "__main__":
    # Run validation
    exit_code = validate_routers()

    # List all endpoints
    list_all_endpoints()

    sys.exit(exit_code)
