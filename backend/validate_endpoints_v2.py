"""
Endpoint Validation Script - Router Refactoring V2
Validates all 69 endpoints across 5 routers are registered correctly.
Creates a complete app and inspects all registered routes.
"""

import sys
from collections import defaultdict

from glowupai.complete_api import create_complete_app


def extract_all_routes(app):
    """Extract all routes from the FastAPI app."""
    routes_by_router = defaultdict(list)

    for route in app.routes:
        if hasattr(route, 'methods') and hasattr(route, 'path'):
            for method in route.methods:
                if method != 'HEAD':  # Skip HEAD methods
                    path = route.path

                    # Categorize by path prefix
                    if '/api/users/' in path or path == '/api/users':
                        category = 'Users'
                    elif '/api/captures' in path or '/api/users/' in path and ('capture' in path or 'check-in' in path or 'measurement' in path or 'labels' in path or 'reprocess' in path or 'shelf-scan' in path):
                        category = 'Captures'
                    elif '/api/users/' in path and ('analytics' in path or 'engagement' in path or 'context-events' in path or 'root-cause' in path or 'budget-optimizer' in path or 'derm-export' in path):
                        category = 'Analytics'
                    elif '/api/products' in path or '/api/routine-events' in path or '/api/experiments' in path or ('/api/users/' in path and ('subscription' in path or 'purchase-guidance' in path or 'confound-check' in path or 'qna' in path or 'discover' in path or 'commerce' in path)):
                        category = 'Subscriptions'
                    elif '/api/admin' in path or path == '/api/metrics' or path == '/api/triage' or path == '/api/auth/session':
                        if path == '/api/auth/session':
                            category = 'Users'
                        else:
                            category = 'Admin'
                    elif path == '/api/health':
                        category = 'Core'
                    else:
                        category = 'Other'

                    routes_by_router[category].append({
                        'method': method,
                        'path': path,
                        'name': route.name if hasattr(route, 'name') else 'unknown'
                    })

    return routes_by_router


def validate_routes():
    """Validate all routes in the complete app."""

    print("=" * 80)
    print("ROUTER REFACTORING VALIDATION REPORT")
    print("=" * 80)
    print()

    print("Creating complete FastAPI application...")
    app = create_complete_app()
    print()

    routes_by_router = extract_all_routes(app)

    expected_counts = {
        'Users': 8,
        'Captures': 16,
        'Analytics': 8,
        'Subscriptions': 21,
        'Admin': 16,
    }

    total_routes = 0
    total_expected = sum(expected_counts.values())
    all_passed = True

    for router_name in ['Users', 'Captures', 'Analytics', 'Subscriptions', 'Admin']:
        routes = routes_by_router.get(router_name, [])
        expected = expected_counts[router_name]

        total_routes += len(routes)

        status = "✓ PASS" if len(routes) == expected else "✗ FAIL"
        if len(routes) != expected:
            all_passed = False

        print(f"{router_name} Router: {status}")
        print(f"  Expected: {expected} endpoints")
        print(f"  Found:    {len(routes)} endpoints")
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

    # Show other routes if any
    other_routes = routes_by_router.get('Other', []) + routes_by_router.get('Core', [])
    if other_routes:
        print("\nOther Routes:")
        for route in other_routes:
            print(f"  {route['method']:7s} {route['path']}")
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

    print("\n" + "=" * 80)
    print("COMPLETE ENDPOINT LIST")
    print("=" * 80)
    print()

    app = create_complete_app()
    routes_by_router = extract_all_routes(app)

    endpoint_number = 1
    for router_name in ['Users', 'Captures', 'Analytics', 'Subscriptions', 'Admin']:
        routes = routes_by_router.get(router_name, [])

        print(f"\n{router_name} Router ({len(routes)} endpoints):")
        print("-" * 80)

        for route in sorted(routes, key=lambda x: (x['path'], x['method'])):
            print(f"{endpoint_number:3d}. {route['method']:7s} {route['path']}")
            endpoint_number += 1

    print("\n" + "=" * 80)
    print(f"Total: {endpoint_number - 1} endpoints validated")
    print("=" * 80)


if __name__ == "__main__":
    # Run validation
    exit_code = validate_routes()

    # List all endpoints
    list_all_endpoints()

    sys.exit(exit_code)
