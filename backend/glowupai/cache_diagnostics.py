"""Cache diagnostic utilities."""

from __future__ import annotations

import logging
import os
from typing import Any

logger = logging.getLogger(__name__)


def check_cache_health(cache) -> dict[str, Any]:
    """Check cache health and configuration.

    Args:
        cache: RedisCache instance

    Returns:
        Dictionary with cache status information
    """
    status = {
        "enabled": True,
        "backend": "redis" if cache.redis_client else "memory",
        "default_ttl": cache.default_ttl,
        "redis_url": cache.redis_url if cache.redis_url else None,
        "redis_connected": False,
        "memory_cache_size": len(cache.memory_cache),
        "env_vars": {
            "GLOWUPAI_CACHE_ENABLED": os.getenv("GLOWUPAI_CACHE_ENABLED", "1"),
            "REDIS_URL": "set" if os.getenv("REDIS_URL") else "not set",
            "REDIS_PRIVATE_URL": "set" if os.getenv("REDIS_PRIVATE_URL") else "not set",
        },
    }

    # Test Redis connection
    if cache.redis_client:
        try:
            cache.redis_client.ping()
            status["redis_connected"] = True
            logger.info("Cache using Redis backend (connected)")
        except (ConnectionError, TimeoutError, OSError) as exc:
            logger.warning(f"Redis configured but not connected: {exc}")
            status["redis_error"] = str(exc)
    else:
        logger.info("Cache using in-memory backend")

    return status


def test_cache_operations(cache) -> dict[str, Any]:
    """Test basic cache operations.

    Args:
        cache: RedisCache instance

    Returns:
        Dictionary with test results
    """
    test_key = "cache:test:diagnostic"
    test_value = {"test": "data", "timestamp": "diagnostic"}

    results: dict[str, Any] = {
        "set": False,
        "get": False,
        "delete": False,
        "errors": [],
    }

    try:
        # Test set
        results["set"] = cache.set(test_key, test_value, ttl=10)

        # Test get
        retrieved = cache.get(test_key)
        results["get"] = retrieved == test_value

        # Test delete
        results["delete"] = cache.delete(test_key)

        # Verify delete
        should_be_none = cache.get(test_key)
        if should_be_none is not None:
            results["errors"].append("Delete did not remove value")

    except Exception as exc:
        results["errors"].append(str(exc))
        logger.exception("Cache operation test failed")

    results["success"] = results["set"] and results["get"] and results["delete"]

    return results


def get_cache_stats(cache) -> dict[str, Any]:
    """Get cache statistics.

    Args:
        cache: RedisCache instance

    Returns:
        Dictionary with cache statistics
    """
    stats: dict[str, Any] = {
        "backend": "redis" if cache.redis_client else "memory",
        "memory_entries": len(cache.memory_cache),
    }

    if cache.redis_client:
        try:
            info = cache.redis_client.info("stats")
            stats["redis_stats"] = {
                "total_commands_processed": info.get("total_commands_processed"),
                "keyspace_hits": info.get("keyspace_hits"),
                "keyspace_misses": info.get("keyspace_misses"),
            }

            # Calculate hit rate
            hits = info.get("keyspace_hits", 0)
            misses = info.get("keyspace_misses", 0)
            total = hits + misses
            if total > 0:
                stats["hit_rate"] = f"{(hits / total * 100):.1f}%"
        except (ConnectionError, TimeoutError, OSError, AttributeError) as exc:
            logger.warning(f"Could not retrieve Redis stats: {exc}")
            stats["redis_stats"] = "unavailable"

    return stats


def print_cache_diagnostics(cache) -> None:
    """Print cache diagnostics to console.

    Args:
        cache: RedisCache instance
    """
    print("\n" + "=" * 60)
    print("CACHE DIAGNOSTICS")
    print("=" * 60)

    # Check health
    health = check_cache_health(cache)
    print("\nCache Health:")
    print(f"  Backend: {health['backend']}")
    print(f"  Enabled: {health['enabled']}")
    print(f"  Default TTL: {health['default_ttl']}s")

    if health["backend"] == "redis":
        print(f"  Redis Connected: {health['redis_connected']}")
        if not health["redis_connected"]:
            print(f"  Redis Error: {health.get('redis_error', 'Unknown')}")
    else:
        print(f"  Memory Cache Size: {health['memory_cache_size']}")

    print("\nEnvironment Variables:")
    for key, value in health["env_vars"].items():
        print(f"  {key}: {value}")

    # Test operations
    print("\nTesting Cache Operations:")
    tests = test_cache_operations(cache)
    print(f"  Set: {'✓' if tests['set'] else '✗'}")
    print(f"  Get: {'✓' if tests['get'] else '✗'}")
    print(f"  Delete: {'✓' if tests['delete'] else '✗'}")

    if tests["errors"]:
        print("  Errors:")
        for error in tests["errors"]:
            print(f"    - {error}")

    print(f"\n  Overall: {'✓ PASS' if tests['success'] else '✗ FAIL'}")

    # Get stats
    stats = get_cache_stats(cache)
    print("\nCache Statistics:")
    print(f"  Backend: {stats['backend']}")

    if stats["backend"] == "redis" and stats.get("redis_stats") != "unavailable":
        redis_stats = stats.get("redis_stats", {})
        print(f"  Commands: {redis_stats.get('total_commands_processed', 'N/A')}")
        print(f"  Hits: {redis_stats.get('keyspace_hits', 'N/A')}")
        print(f"  Misses: {redis_stats.get('keyspace_misses', 'N/A')}")
        print(f"  Hit Rate: {stats.get('hit_rate', 'N/A')}")
    else:
        print(f"  Memory Entries: {stats['memory_entries']}")

    print("\n" + "=" * 60)
