# GlowUp AI - Performance Test Scenarios

Last updated: 2026-08-30

## Overview
This document defines performance testing scenarios for GlowUp AI to ensure the app delivers a smooth, responsive experience under various conditions.

---

## Performance Goals

### App Performance Targets

| Metric | Target | Acceptable | Unacceptable |
|--------|--------|------------|--------------|
| Cold start time | < 1.5s | 1.5-3s | > 3s |
| Warm start time | < 800ms | 800ms-1.5s | > 1.5s |
| Screen transition | < 300ms | 300-500ms | > 500ms |
| API response time | < 500ms | 500ms-1s | > 1s |
| Image upload (1MB) | < 2s (WiFi) | 2-5s | > 5s |
| Image upload (1MB) | < 8s (4G) | 8-15s | > 15s |
| Dashboard load | < 1s | 1-2s | > 2s |
| History scroll (100 items) | 60 fps | 45-60 fps | < 45 fps |
| Memory usage (idle) | < 100MB | 100-150MB | > 150MB |
| Memory usage (peak) | < 250MB | 250-300MB | > 300MB |
| APK size | < 25MB | 25-30MB | > 30MB |
| Battery drain (30min active) | < 5% | 5-10% | > 10% |

### Backend Performance Targets

| Metric | Target | Acceptable | Unacceptable |
|--------|--------|------------|--------------|
| GET /health | < 50ms | 50-100ms | > 100ms |
| GET /dashboard | < 200ms | 200-500ms | > 500ms |
| POST /captures (no processing) | < 300ms | 300-800ms | > 800ms |
| Capture processing (full) | < 3s | 3-8s | > 8s |
| GET /history (50 items) | < 300ms | 300-700ms | > 700ms |
| Database query (simple) | < 10ms | 10-50ms | > 50ms |
| Database query (complex join) | < 100ms | 100-300ms | > 300ms |
| Concurrent requests (10 users) | < 500ms avg | 500ms-1s | > 1s |
| Concurrent requests (100 users) | < 1s avg | 1-2s | > 2s |

---

## Test Environment Setup

### Load Testing Tools
- **Backend:** Locust or k6 for HTTP load testing
- **Android:** Android Profiler (CPU, Memory, Network, Energy)
- **Monitoring:** Firebase Performance Monitoring
- **APK Analysis:** Android Studio APK Analyzer

### Test Devices
- **Low-end:** Pixel 3a, Android 10, 4GB RAM
- **Mid-range:** Pixel 5, Android 12, 8GB RAM
- **High-end:** Pixel 7 Pro, Android 13, 12GB RAM

### Network Conditions
- **WiFi:** 50 Mbps down, 10 Mbps up, 20ms latency
- **4G:** 10 Mbps down, 5 Mbps up, 100ms latency
- **3G:** 2 Mbps down, 1 Mbps up, 300ms latency
- **Offline:** Complete disconnect

---

## Android App Performance Tests

### 1. App Startup Performance

#### 1.1 Cold Start (P0)
**Scenario:** User launches app for first time or after clearing from memory

**Setup:**
```bash
# Force stop app
adb shell am force-stop com.glowup.ai

# Clear app data
adb shell pm clear com.glowup.ai

# Launch app and measure
adb shell am start-activity -W com.glowup.ai/.MainActivity
```

**Measurement:**
- Time to first frame (TTFF)
- Time to interactive
- Memory allocated during startup

**Expected:**
- TTFF < 1.5s
- Interactive < 2s
- Memory < 80MB after startup

**Test Cases:**
- [ ] Cold start on low-end device
- [ ] Cold start on high-end device
- [ ] Cold start with slow network
- [ ] Cold start with no network

---

#### 1.2 Warm Start (P0)
**Scenario:** User returns to app after backgrounding (activity in memory)

**Setup:**
```bash
# Launch app
adb shell am start-activity com.glowup.ai/.MainActivity

# Send to background
adb shell input keyevent KEYCODE_HOME

# Wait 5 seconds
sleep 5

# Relaunch and measure
adb shell am start-activity -W com.glowup.ai/.MainActivity
```

**Expected:**
- Resume time < 800ms
- No cold data fetching
- Cached UI state restored

**Test Cases:**
- [ ] Warm start after 5 seconds
- [ ] Warm start after 1 minute
- [ ] Warm start after 10 minutes

---

### 2. Screen Transition Performance

#### 2.1 Navigation Transitions (P1)
**Scenario:** User taps between bottom nav tabs

**Measurement:**
- Frame rate during transition
- Dropped frames
- Transition duration

**Expected:**
- 60 fps sustained
- < 2 dropped frames
- Transition < 300ms

**Test Cases:**
- [ ] Home → Routine tab
- [ ] Routine → Insights tab
- [ ] Insights → Discover tab
- [ ] Discover → Account tab
- [ ] Rapid tab switching (stress test)

**Tools:** Android Profiler, GPU Rendering profile

---

#### 2.2 Screen Load Performance (P0)
**Scenario:** User navigates to data-heavy screens

**Test Cases:**

| Screen | Max Load Time | Data Volume |
|--------|---------------|-------------|
| Dashboard | < 1s | Recent metrics, streak, achievements |
| History | < 1.5s | 50 captures with thumbnails |
| Routine Products | < 800ms | 20 products |
| Experiments List | < 800ms | 10 experiments |
| Q&A Threads | < 1s | 20 threads |
| Achievements | < 600ms | All achievements (locked + unlocked) |
| Account Settings | < 500ms | Profile data |

**Measurement:**
- Time from click to first content visible
- Time to fully interactive
- Number of API calls
- Memory allocated

**Tools:** Firebase Performance Monitoring custom traces

---

### 3. Capture Flow Performance

#### 3.1 Camera Launch (P0)
**Scenario:** User taps "New Capture" button

**Measurement:**
- Time from tap to camera preview visible
- Camera initialization time
- ML Kit face detection startup

**Expected:**
- Preview visible < 500ms
- Face detection active < 800ms
- No UI jank during camera preview

**Test Cases:**
- [ ] First camera launch (cold)
- [ ] Subsequent launches (warm)
- [ ] Low-end device camera performance
- [ ] Camera launch with low battery

---

#### 3.2 Capture Processing (P0)
**Scenario:** User takes photo and submits

**Steps:**
1. Capture photo
2. Preview shown
3. User confirms
4. Image encoded to base64
5. Upload to backend
6. Quality check
7. Metrics calculated
8. Results returned

**Measurement:**
- Image encoding time
- Upload time (by network condition)
- Backend processing time
- End-to-end time

**Expected:**
- Encoding < 200ms
- Upload < 2s (WiFi), < 8s (4G)
- Backend processing < 3s
- Total < 5s (WiFi), < 12s (4G)

**Test Cases:**
- [ ] Standard photo (1920x1080, ~1MB)
- [ ] High-res photo (4K, ~3MB)
- [ ] Low-res photo (720p, ~300KB)
- [ ] WiFi upload
- [ ] 4G upload
- [ ] 3G upload (expect degradation)
- [ ] Upload with 30% battery (power saving mode)

**Tools:** Android Profiler Network tab, backend logs

---

#### 3.3 Offline Capture (P1)
**Scenario:** User captures photo with no internet

**Measurement:**
- Time to save to Room database
- Time to display "saved locally" confirmation

**Expected:**
- Room insert < 100ms
- Confirmation visible < 200ms
- No blocking or freezing

**Test Cases:**
- [ ] Single offline capture
- [ ] Multiple offline captures (queue 5)
- [ ] Offline capture with full storage (handle gracefully)

---

### 4. List Scrolling Performance

#### 4.1 History Timeline Scroll (P1)
**Scenario:** User scrolls through capture history with 100+ items

**Measurement:**
- Sustained frame rate
- Jank (dropped frames)
- Memory usage during scroll

**Expected:**
- 60 fps sustained
- < 3 janky frames per scroll
- Memory growth < 10MB during scroll

**Test Cases:**
- [ ] Scroll 100 items
- [ ] Scroll 500 items
- [ ] Scroll with high-res thumbnails
- [ ] Fling scroll (fast scroll)
- [ ] Reverse scroll

**Optimization:**
- Lazy loading with Paging 3
- Image thumbnail caching (Coil)
- RecyclerView view recycling

---

#### 4.2 Product List Scroll (P2)
**Scenario:** User scrolls routine products list

**Expected:**
- Smooth scroll with 50+ products
- Thumbnail loading doesn't block scroll
- No memory leaks

**Test Cases:**
- [ ] Scroll 50 products
- [ ] Scroll with product images loading

---

### 5. Image Loading Performance

#### 5.1 Thumbnail Grid (P1)
**Scenario:** History screen loads 20 thumbnails simultaneously

**Measurement:**
- Time to load all thumbnails
- Memory usage
- Cache hit rate

**Expected:**
- All thumbnails visible < 2s
- Memory < 50MB increase
- Cache hit rate > 80% on subsequent loads

**Test Cases:**
- [ ] First load (cold cache)
- [ ] Second load (warm cache)
- [ ] Scroll and return (recycled views)

**Tools:** Coil image library with memory cache

---

#### 5.2 Full-Size Image Display (P1)
**Scenario:** User taps capture to view full size

**Measurement:**
- Time to load full-res image
- Memory peak
- Zoom/pan performance

**Expected:**
- Image visible < 1s
- Memory < 100MB increase
- Smooth zoom with no jank

**Test Cases:**
- [ ] Load 1MB image
- [ ] Load 3MB image
- [ ] Zoom in/out
- [ ] Pan around zoomed image

---

### 6. Background Task Performance

#### 6.1 WorkManager Sync (P1)
**Scenario:** Pending captures sync when internet restored

**Measurement:**
- Time to trigger sync after connectivity restored
- Upload queue processing time
- Battery impact

**Expected:**
- Trigger within 30 seconds of connectivity
- Process 5 pending uploads in < 1 minute
- Battery drain < 2% for sync

**Test Cases:**
- [ ] Sync 1 pending capture
- [ ] Sync 5 pending captures
- [ ] Sync with slow network (4G)
- [ ] Sync failure and retry logic

**Tools:** WorkManager Inspector, Battery Historian

---

#### 6.2 Notification Processing (P2)
**Scenario:** Firebase Cloud Messaging delivers notification

**Measurement:**
- Notification display latency
- Battery impact (idle listening)

**Expected:**
- Notification appears < 5s after send
- Idle battery drain < 0.5%/hour

---

### 7. Memory Management

#### 7.1 Memory Leak Detection (P0)
**Scenario:** Long-running app session

**Test:**
1. Launch app
2. Navigate through all screens
3. Perform captures (5x)
4. Add products (10x)
5. Scroll history
6. Return to dashboard
7. Check memory

**Expected:**
- Memory stabilizes (no continuous growth)
- GC runs periodically
- No leaked activities or fragments

**Tools:**
- Android Profiler Memory tab
- LeakCanary (in debug builds)

**Test Cases:**
- [ ] 30 minute active session
- [ ] Background for 1 hour, return
- [ ] Repeated screen navigations (100x)
- [ ] Repeated camera launches (50x)

---

#### 7.2 Memory Pressure (P1)
**Scenario:** Low memory device (2GB RAM)

**Test:**
- [ ] Load history with 100 captures
- [ ] Load all images in view
- [ ] Check for OutOfMemoryError
- [ ] Verify image cache eviction works

**Expected:**
- Graceful degradation
- LRU cache evicts old images
- No crashes

---

### 8. Database Performance

#### 8.1 Room Query Performance (P1)
**Scenario:** Fetch data from local database

**Queries to Test:**

| Query | Expected Time |
|-------|---------------|
| Get user profile | < 5ms |
| Get recent captures (10) | < 10ms |
| Get all products | < 20ms |
| Get capture history (100) | < 50ms |
| Get pending uploads (outbox) | < 10ms |

**Tools:**
- Room database profiler
- SQL EXPLAIN QUERY PLAN

**Test Cases:**
- [ ] Cold query (first run)
- [ ] Warm query (cached)
- [ ] Query with large dataset (500+ rows)

---

#### 8.2 Room Insert Performance (P1)
**Scenario:** Write operations

**Operations:**

| Operation | Expected Time |
|-----------|---------------|
| Insert single capture | < 20ms |
| Insert product | < 10ms |
| Insert routine event | < 10ms |
| Batch insert 10 captures | < 100ms |

**Test Cases:**
- [ ] Single inserts
- [ ] Batch inserts
- [ ] Concurrent writes (rare, but test)

---

### 9. Network Performance

#### 9.1 API Response Time (P0)
**Scenario:** Measure backend API latency from app

**Test Cases:**

| Endpoint | Network | Target Time |
|----------|---------|-------------|
| GET /health | WiFi | < 100ms |
| GET /dashboard | WiFi | < 500ms |
| POST /captures | WiFi | < 2s |
| GET /history | WiFi | < 500ms |
| GET /health | 4G | < 200ms |
| GET /dashboard | 4G | < 1s |
| POST /captures | 4G | < 8s |

**Tools:**
- OkHttp Interceptor logging
- Firebase Performance HTTP metrics

---

#### 9.2 Retry Logic Performance (P1)
**Scenario:** Network request fails, retry with backoff

**Test:**
1. Mock backend 500 error
2. Trigger API call
3. Verify exponential backoff
4. Measure retry timing

**Expected:**
- First retry after 1s
- Second retry after 2s
- Third retry after 4s
- User notified after 3 failures

**Test Cases:**
- [ ] Transient failure (succeeds on retry 2)
- [ ] Persistent failure (all retries fail)
- [ ] Network timeout (30s max)

---

### 10. Battery Performance

#### 10.1 Active Usage (P1)
**Scenario:** User actively using app for 30 minutes

**Activities:**
- Browse dashboard
- Take 3 captures
- Scroll history
- Navigate between tabs

**Measurement:**
- Battery drain percentage
- Power consumption (mW)
- Screen-on time contribution

**Expected:**
- Total drain < 5% in 30 minutes
- App ranks in "Good" battery usage category

**Tools:**
- Battery Historian
- Android Profiler Energy tab

---

#### 10.2 Background Drain (P0)
**Scenario:** App in background for 1 hour

**Background Tasks:**
- WorkManager periodic checks (15min interval)
- Firebase Cloud Messaging listening

**Expected:**
- Battery drain < 0.5% per hour
- No wakelocks held
- No excessive wakeups

**Test Cases:**
- [ ] Background 1 hour, no pending tasks
- [ ] Background 1 hour, with pending uploads
- [ ] Background 12 hours (overnight)

---

## Backend Performance Tests

### 11. Backend Load Testing

#### 11.1 Baseline Load Test (P0)
**Scenario:** Simulate typical user traffic

**Profile:**
- 100 concurrent users
- User flow:
  - Sign in
  - Load dashboard (GET /dashboard)
  - View history (GET /history)
  - Submit capture (POST /captures) - 10% of requests
  - Query products (GET /products)

**Load Test Script (Locust):**

```python
from locust import HttpUser, task, between

class GlowUpUser(HttpUser):
    wait_time = between(1, 5)
    
    def on_start(self):
        # Sign in
        self.user_id = "test_user_123"
    
    @task(10)
    def view_dashboard(self):
        self.client.get(f"/api/users/{self.user_id}/dashboard")
    
    @task(5)
    def view_history(self):
        self.client.get(f"/api/users/{self.user_id}/history")
    
    @task(3)
    def view_products(self):
        self.client.get(f"/api/users/{self.user_id}/products")
    
    @task(1)
    def submit_capture(self):
        # Mock base64 image
        payload = {
            "user_id": self.user_id,
            "image_base64": "iVBOR...",  # Truncated
            "is_baseline": False
        }
        self.client.post(f"/api/users/{self.user_id}/captures", json=payload)
```

**Expected:**
- P50 response time < 300ms
- P95 response time < 1s
- P99 response time < 2s
- Error rate < 0.1%
- CPU usage < 70%
- Memory usage < 80%

---

#### 11.2 Spike Load Test (P1)
**Scenario:** Sudden traffic spike (e.g., notification blast)

**Profile:**
- Ramp from 10 to 500 users in 1 minute
- Hold 500 users for 5 minutes
- Ramp down to 10 users

**Expected:**
- Auto-scaling triggers (if cloud deployment)
- Response time degrades gracefully
- No 500 errors
- Queue fills but doesn't overflow

---

#### 11.3 Capture Processing Load (P0)
**Scenario:** Multiple users submitting captures simultaneously

**Profile:**
- 50 concurrent capture uploads
- Each capture ~1MB base64 image
- Full quality check and metric calculation

**Expected:**
- Queue processes captures sequentially
- Average processing time < 5s per capture
- No timeouts (60s max)
- No memory exhaustion

**Test Cases:**
- [ ] 10 concurrent captures
- [ ] 50 concurrent captures
- [ ] 100 concurrent captures (stress test)

---

#### 11.4 Database Query Performance (P0)
**Scenario:** Measure database query latency under load

**Queries to Profile:**

```sql
-- Get dashboard (complex query)
SELECT * FROM users WHERE user_id = ?;
SELECT * FROM captures WHERE user_id = ? ORDER BY captured_at DESC LIMIT 10;
SELECT * FROM products WHERE user_id = ?;
SELECT * FROM experiments WHERE user_id = ? AND status = 'active';

-- Get history
SELECT * FROM captures WHERE user_id = ? ORDER BY captured_at DESC LIMIT 50;

-- Insert capture
INSERT INTO captures (...) VALUES (...);
```

**Expected:**
- Simple SELECT < 10ms
- Complex JOIN < 100ms
- INSERT < 20ms
- Under 100 concurrent queries: < 200ms

**Tools:**
- PostgreSQL EXPLAIN ANALYZE
- pg_stat_statements
- Database connection pool monitoring

---

### 12. Backend Stress Testing

#### 12.1 Database Connection Pool (P1)
**Scenario:** Exhaust connection pool

**Setup:**
- Connection pool max size: 20
- Simulate 50 concurrent requests (force queuing)

**Expected:**
- Requests queue gracefully
- No connection timeout errors
- Pool recovers after load drops

---

#### 12.2 Memory Leak Detection (P0)
**Scenario:** Long-running backend process

**Test:**
- Run backend for 12 hours under moderate load
- Monitor memory usage

**Expected:**
- Memory usage stabilizes
- No continuous growth
- Python GC runs periodically

**Tools:**
- memory_profiler
- psutil
- Prometheus + Grafana

---

### 13. API Endpoint Benchmarks

Run benchmarks for critical endpoints:

```bash
# Using Apache Bench
ab -n 1000 -c 10 https://api.glowup.ai/api/health

# Using wrk
wrk -t4 -c100 -d30s https://api.glowup.ai/api/users/test_user/dashboard
```

**Results Table:**

| Endpoint | Method | RPS (Requests/sec) | P50 | P95 | P99 |
|----------|--------|-------------------|-----|-----|-----|
| /health | GET | | | | |
| /dashboard | GET | | | | |
| /history | GET | | | | |
| /products | GET | | | | |
| /captures | POST | | | | |
| /experiments | GET | | | | |

**Expected RPS:**
- GET /health: > 500 RPS
- GET /dashboard: > 200 RPS
- POST /captures: > 50 RPS (limited by processing)

---

## Performance Monitoring in Production

### Real User Monitoring (RUM)

**Firebase Performance Monitoring:**
- [ ] Automatic traces for app start, screen rendering
- [ ] Custom traces for critical flows:
  - Capture upload
  - Dashboard load
  - History fetch

**Crashlytics:**
- [ ] Crash-free rate > 99.5%
- [ ] ANR (Application Not Responding) rate < 0.1%

**Backend Monitoring:**
- [ ] Prometheus metrics exported
- [ ] Grafana dashboards for:
  - Request latency
  - Error rate
  - Database query time
  - Queue depth

---

## Performance Test Checklist

Before release:

- [ ] App cold start < 2s on mid-range device
- [ ] Dashboard loads < 1s
- [ ] Capture upload < 5s on WiFi
- [ ] Capture upload < 12s on 4G
- [ ] History scrolls at 60 fps
- [ ] No memory leaks in 30min session
- [ ] Battery drain < 5% per 30min active use
- [ ] Backend handles 100 concurrent users
- [ ] API P95 latency < 1s
- [ ] Backend CPU < 70% under load
- [ ] Database queries < 100ms
- [ ] APK size < 30MB

---

## Performance Regression Tracking

Track metrics across releases:

| Version | App Size | Cold Start | Dashboard Load | Memory (Peak) |
|---------|----------|------------|----------------|---------------|
| 1.0.0 | 24MB | 1.8s | 900ms | 220MB |
| 1.1.0 | 25MB | 1.7s | 850ms | 210MB |
| 1.2.0 | | | | |

Flag regression if any metric increases > 10% from previous version.

---

## Sign-off

- [ ] All performance tests completed
- [ ] Results within acceptable range
- [ ] Performance report generated
- [ ] Regressions documented and addressed

**Performance Engineer:** ___________
**Date:** ___________
