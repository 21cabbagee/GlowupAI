# Dashboard History Count Cache Fix

## Problem
The dashboard was showing 1 capture when 2 actually existed in the database.

## Root Cause
The dashboard endpoint (`/api/users/{user_id}/dashboard`) had caching enabled with a 5-minute TTL. When a new capture was created, the cache was not invalidated, causing stale data to be served to users.

### Timeline of the Bug:
1. User views dashboard → 1 capture shown, response cached for 5 minutes
2. User creates a second capture → capture saved to database
3. User views dashboard again (within 5 minutes) → cached response returned, still showing only 1 capture
4. User would need to wait 5 minutes or manually clear cache to see the second capture

## Solution
Added cache invalidation logic to the capture creation endpoint that:

1. **Looks up the user's Firebase UID** from the database (needed because cache keys use Firebase UID, not internal user ID)

2. **Generates all possible cache keys** for the user's dashboard and history endpoints:
   - Dashboard with no query params
   - Dashboard with `vertical=skin`
   - Dashboard with `vertical=hair`
   - History with no query params
   - History with `vertical=skin`
   - History with `vertical=hair`

3. **Deletes all those cache keys** so the next request will bypass the cache and fetch fresh data

## Files Modified

### 1. `/backend/glowupai/performance.py`
- Added `delete_pattern()` method to `RedisCache` class for pattern-based cache deletion
- Added `generate_cache_key_for_user()` static method to `CacheMiddleware` class for generating cache keys outside of request context

### 2. `/backend/glowupai/complete_api.py`
- Added cache invalidation logic in the `capture()` endpoint (lines ~569-592)
- Invalidates dashboard and history caches after a capture is successfully created

## Testing
Created `test_cache_fix.py` to verify:
- Cache key generation matches the middleware logic
- All necessary cache variations are invalidated
- The fix covers common user scenarios

## Future Optimizations
1. **More granular cache keys**: Include a version number or timestamp in cache keys that can be incremented
2. **Cache tags**: Use Redis cache tags to group related cache entries for easier invalidation
3. **Event-based invalidation**: Use a pub/sub pattern to notify all instances when cache should be invalidated
4. **Reduce cache TTL**: Consider reducing the 5-minute TTL for frequently-updated endpoints

## Impact
- ✅ Users will now see updated capture counts immediately after creating a capture
- ✅ No more stale dashboard data
- ✅ Analytics and engagement metrics will reflect current state
- ⚠️ Slight performance impact: each capture creation now makes an additional database query (to get firebase_uid) and 6 cache deletions

## Verification Steps
1. Create a capture → verify dashboard shows it
2. Create another capture → verify dashboard immediately shows both
3. Check logs for "Cache invalidated after capture creation" messages
4. Monitor cache hit rate to ensure invalidation is working
