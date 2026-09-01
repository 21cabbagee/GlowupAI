# Cache Quick Reference

## Quick Test

```bash
# Run verification script
cd /Users/21cabbage/GlowupAI/backend
python3 verify_cache.py

# With admin token for full diagnostics
python3 verify_cache.py --admin-token YOUR_ADMIN_TOKEN
```

## Check Cache Status in Logs

```bash
# Look for these log messages:
grep "Cache HIT\|Cache MISS" your_log_file.log
```

Example logs:
```
INFO Cache MISS: /api/users/user123/dashboard (stored with key: cache:user:user...)
INFO Cache HIT: /api/users/user123/dashboard (key: cache:user:user...)
```

## Check Cache Headers

```bash
# First request (will be MISS)
curl -I http://localhost:8000/api/users/user123/dashboard

# Second request (should be HIT)
curl -I http://localhost:8000/api/users/user123/dashboard
```

Look for:
```
X-Cache-Status: HIT
Cache-Control: public, max-age=300
X-Response-Time: 1.23ms
```

## Admin Diagnostics API

```bash
curl -H "Authorization: Bearer YOUR_ADMIN_TOKEN" \
     http://localhost:8000/api/admin/cache/diagnostics | jq
```

Returns:
```json
{
  "health": {
    "enabled": true,
    "backend": "redis",
    "redis_connected": true,
    "default_ttl": 300
  },
  "operations_test": {
    "set": true,
    "get": true,
    "delete": true,
    "success": true
  },
  "stats": {
    "backend": "redis",
    "hit_rate": "95.2%"
  }
}
```

## Invalidate Cache (Code Example)

```python
from glowupai.performance import CacheMiddleware

# Get cache from app state
cache = app.state.cache

# Invalidate all cache for a user
CacheMiddleware.invalidate_user_cache(cache, user_id)

# Or specifically dashboard cache
CacheMiddleware.invalidate_dashboard_cache(cache, user_id)
```

## Environment Variables

```bash
# Enable/disable cache (default: enabled)
GLOWUPAI_CACHE_ENABLED=1

# Redis URL (optional - falls back to in-memory)
REDIS_URL=redis://localhost:6379/0
```

## Cacheable Endpoints

By default, these paths are cached:
- `/api/dashboard`
- `/api/users/*` (including `/api/users/{user_id}/dashboard`)

Only GET requests are cached. POST/PUT/DELETE are never cached.

## Performance Expectations

- **Without cache**: ~100-150ms per dashboard request
- **With cache (HIT)**: ~1-5ms per dashboard request
- **Expected speedup**: 20-100x for cached requests

## Troubleshooting

### Cache not working?

1. **Check if enabled**:
   ```bash
   echo $GLOWUPAI_CACHE_ENABLED  # should be "1"
   ```

2. **Check logs for cache events**:
   ```bash
   grep -i cache your_log_file.log
   ```

3. **Check response headers**:
   ```bash
   curl -I http://localhost:8000/api/users/test/dashboard | grep X-Cache
   ```

4. **Run diagnostics**:
   ```bash
   python3 verify_cache.py
   ```

### No speedup detected?

- Check `X-Cache-Status` header - is it "HIT"?
- Are you testing the same endpoint/user/params?
- Is the endpoint in the cacheable paths list?
- Check logs for "Cache HIT" messages

### Redis not connecting?

The cache will automatically fall back to in-memory cache if Redis is unavailable.

In-memory cache works but:
- Doesn't persist across process restarts
- Not shared across multiple workers
- Use Redis in production for best results

## Test Suite

Run the test suite to verify everything works:

```bash
cd /Users/21cabbage/GlowupAI/backend

# All tests
python3 test_cache.py
python3 test_cache_integration.py
python3 test_cache_invalidation.py
python3 test_cache_headers.py
```

Expected results:
- ✓ Cache providing 3x speedup (basic)
- ✓ Cache providing 104x speedup (integration)  
- ✓ Cache invalidation working correctly
- ✓ Cache headers working correctly

## Production Deployment

1. **Enable cache** (default: enabled)
2. **Configure Redis** (recommended):
   ```bash
   export REDIS_URL=redis://your-redis-host:6379/0
   ```
3. **Monitor logs** for cache hits/misses
4. **Check metrics** via `/api/admin/cache/diagnostics`
5. **Invalidate cache** when data changes

## Need Help?

See full documentation: `CACHE_FIX_SUMMARY.md`
