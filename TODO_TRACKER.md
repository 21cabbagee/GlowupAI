# TODO Tracker - GlowUp AI

**Generated:** 2026-08-31  
**Purpose:** Organized list of technical debt, improvements, and enhancements identified during comprehensive code review

---

## Priority Key

- **P0 - CRITICAL:** Production blockers, security issues, data loss risks
- **P1 - HIGH:** Significant quality improvements, scalability concerns
- **P2 - MEDIUM:** Code quality, maintainability, technical debt
- **P3 - LOW:** Nice-to-haves, optimizations, polish

---

## P0 - CRITICAL (Must Fix Before Production)

### Security & Configuration

- [ ] **Enable authentication in production deployment**
  - Set `SKINPROOF_AUTH_REQUIRED=1` in production environment
  - Verify Firebase project configuration
  - Test token verification end-to-end
  - **File:** `backend/skinproof/config.py:114`
  - **Risk:** Unauthorized access to user data

- [ ] **Configure explicit CORS origins**
  - Remove localhost from ALLOWED_ORIGINS
  - Set actual production domain(s)
  - Test cross-origin requests
  - **File:** `backend/skinproof/config.py:76-95`
  - **Risk:** CSRF attacks, unauthorized API access

- [ ] **Replace MemoryPhotoStore for production**
  - Implement S3/GCS photo store
  - Add migration script for existing photos
  - Test encryption at rest
  - **File:** `backend/skinproof/photos.py:18-36`
  - **Risk:** Data loss on restart, memory exhaustion

- [ ] **Add maximum image size validation**
  - Enforce content-length limits at API boundary
  - Add pre-decode size checks
  - Return clear error messages for oversized uploads
  - **File:** `backend/skinproof/complete_api.py:412-418`
  - **Risk:** Resource exhaustion attacks

- [ ] **Remove legacy key file support in production**
  - Set `SKINPROOF_DISABLE_LEGACY_KEY_FILE=1`
  - Verify Gemini API key from environment only
  - **File:** `backend/skinproof/config.py:9-28`
  - **Risk:** Accidental credential exposure

---

## P1 - HIGH (Scalability & Reliability)

### Infrastructure & Deployment

- [ ] **Implement Redis-backed rate limiter**
  - Replace in-memory rate limiter for multi-process deployments
  - Add Redis connection pooling
  - Implement graceful fallback if Redis unavailable
  - **File:** `backend/skinproof/rate_limit.py:35-40`
  - **Benefit:** Accurate rate limiting in distributed deployment

- [ ] **Add circuit breaker for Gemini API calls**
  - Implement retry with exponential backoff
  - Add failure threshold before circuit opens
  - Monitor circuit breaker state
  - **File:** `backend/skinproof/google_ai.py:67-93`
  - **Benefit:** Graceful degradation on external service failures

- [ ] **Implement health check for external dependencies**
  - Add Gemini API health check
  - Add photo storage health check
  - Include in `/api/health` endpoint
  - **File:** `backend/skinproof/middleware.py:187-242`
  - **Benefit:** Early detection of infrastructure issues

- [ ] **Add database connection pool monitoring**
  - Track pool exhaustion
  - Alert on high connection wait times
  - Add pool metrics to observability
  - **File:** `backend/skinproof/postgres_db.py`
  - **Benefit:** Identify scalability bottlenecks

### Error Handling

- [ ] **Add explicit PIL exception handling**
  - Catch PIL image parsing errors
  - Return user-friendly error messages
  - Log malformed image details for debugging
  - **Files:** `backend/skinproof/capture.py:94-115`, `metrics.py:42-46`
  - **Benefit:** Better error messages, easier debugging

- [ ] **Log JSON parse failures in attribution engine**
  - Add logging when json.loads() fails
  - Include context (user_id, product_id)
  - Alert on high failure rates
  - **File:** `backend/skinproof/attribution.py:147-152`
  - **Benefit:** Detect data corruption early

- [ ] **Add React error boundaries**
  - Wrap top-level routes in error boundaries
  - Provide fallback UI for caught errors
  - Report errors to monitoring service
  - **Files:** `backend/web/app/**/*.tsx`
  - **Benefit:** Graceful frontend error handling

---

## P2 - MEDIUM (Code Quality & Maintainability)

### Documentation

- [ ] **Add docstrings to AttributionEngine methods**
  - Document `evaluate_user()` logic
  - Explain `evaluate_product()` algorithm
  - Add examples of evidence structure
  - **File:** `backend/skinproof/attribution.py`
  - **Benefit:** Easier onboarding, maintainability

- [ ] **Document noise floor calculation**
  - Explain statistical significance thresholds
  - Document why 1.5x noise floor matters
  - Add references to methodology
  - **File:** `backend/skinproof/metrics.py:9, 56, 110`
  - **Benefit:** Scientific transparency

- [ ] **Explain capture quality scoring algorithm**
  - Document component weights
  - Explain acceptance threshold (0.75)
  - Add rationale for each quality check
  - **File:** `backend/skinproof/capture.py:69-80`
  - **Benefit:** Easier to tune quality gates

- [ ] **Add API endpoint examples to docstrings**
  - Document expected request/response formats
  - Add curl examples for common workflows
  - Include error response examples
  - **File:** `backend/skinproof/complete_api.py`
  - **Benefit:** Better API documentation

### Code Organization

- [ ] **Consolidate duplicate rate limiters**
  - Remove `middleware.py:RateLimiter` 
  - Use `rate_limit.py` implementation consistently
  - Update imports across project
  - **Files:** `backend/skinproof/middleware.py`, `rate_limit.py`
  - **Benefit:** Single source of truth, less confusion

- [ ] **Create shared utilities module**
  - Consolidate `json_dumps()` functions
  - Consolidate datetime utilities (`now_iso()`, `iso()`, `parse_time()`)
  - Consolidate ID generation (`uid()`, `new_id()`)
  - **New File:** `backend/skinproof/utils.py`
  - **Benefit:** DRY principle, consistency

- [ ] **Extract constants from magic numbers**
  - Create `constants.py` module
  - Extract capture quality thresholds
  - Extract attribution thresholds
  - Extract rate limit configurations
  - **Files:** `capture.py`, `metrics.py`, `attribution.py`
  - **Benefit:** Easier configuration tuning

- [ ] **Standardize naming conventions**
  - Choose `uid()` OR `new_id()` (not both)
  - Standardize `_private` vs `public` method naming
  - Align `parse_time()` and `as_date()` naming
  - **Files:** Multiple modules
  - **Benefit:** Predictable API surface

### Refactoring

- [ ] **Refactor `create_complete_app()` function**
  - Extract route registration to `_register_routes(app, service)`
  - Extract middleware setup to `_configure_middleware(app, settings)`
  - Keep function under 100 lines
  - **File:** `backend/skinproof/complete_api.py:174-616`
  - **Benefit:** Improved readability, testability

- [ ] **Simplify `AttributionEngine.evaluate_product()`**
  - Extract evidence building to `_build_evidence()`
  - Extract label determination to `_determine_label()`
  - Use early returns for unclear cases
  - **File:** `backend/skinproof/attribution.py:45-141`
  - **Benefit:** Lower complexity, easier testing

- [ ] **Simplify `CaptureQuality.evaluate()`**
  - Use rule engine or strategy pattern
  - Extract each quality check to named function
  - Reduce nesting depth
  - **File:** `backend/skinproof/capture.py:35-81`
  - **Benefit:** Easier to add/remove quality checks

- [ ] **Reduce nesting in `GoogleGeminiVisionService.extract_products()`**
  - Use guard clauses and early returns
  - Extract validation to helper methods
  - **File:** `backend/skinproof/google_ai.py:106-134`
  - **Benefit:** Improved readability

### Testing

- [ ] **Add edge case tests for capture quality**
  - Test boundary values (exactly 12°, exactly 0.20 brightness)
  - Test extreme values (180° yaw, 0.0 brightness)
  - Test invalid inputs (negative values, NaN)
  - **File:** `backend/tests/test_core.py` (add test cases)
  - **Benefit:** Robust quality gate behavior

- [ ] **Add concurrency tests for rate limiter**
  - Test concurrent requests from same client
  - Verify token bucket accuracy under load
  - Test rate limit reset timing
  - **File:** `backend/tests/test_rate_limit.py` (new file)
  - **Benefit:** Confidence in rate limit correctness

- [ ] **Add integration tests for experiment workflow**
  - Test full experiment creation → capture → verdict flow
  - Test early stop detection
  - Test experiment cancellation
  - **File:** `backend/tests/test_experiments_integration.py` (new file)
  - **Benefit:** Catch workflow regressions

- [ ] **Add error path tests for photo storage**
  - Test malformed encryption keys
  - Test disk full scenarios
  - Test concurrent write conflicts
  - **File:** `backend/tests/test_photos.py` (add test cases)
  - **Benefit:** Robust error handling

---

## P3 - LOW (Optimizations & Enhancements)

### Performance

- [ ] **Optimize dashboard N+1 queries**
  - Combine multiple queries into single JOIN
  - Add database query profiling
  - Measure improvement with EXPLAIN QUERY PLAN
  - **File:** `backend/skinproof/complete_service.py` (dashboard methods)
  - **Benefit:** Faster dashboard load times

- [ ] **Add caching for frequently accessed data**
  - Cache user entitlements (1 min TTL)
  - Cache product catalog (5 min TTL)
  - Invalidate on updates
  - **Files:** `backend/skinproof/complete_service.py`
  - **Benefit:** Reduced database load

- [ ] **Consider lazy loading for dashboard data**
  - Return minimal data on initial load
  - Load history/verdicts on demand
  - Implement pagination for long histories
  - **File:** `backend/web/app/(product)/home/page.tsx`
  - **Benefit:** Faster initial page load

### Monitoring & Observability

- [ ] **Add business metrics to observability**
  - Track capture success/rejection rates
  - Track verdict label distribution
  - Track experiment completion rates
  - **File:** `backend/skinproof/observability.py`
  - **Benefit:** Product health visibility

- [ ] **Add structured logging for key events**
  - Log experiment starts/completions
  - Log premium upgrades/downgrades
  - Log consent grants/revocations
  - **Files:** `backend/skinproof/complete_service.py` (audit events)
  - **Benefit:** Business intelligence, debugging

- [ ] **Implement alerting thresholds**
  - Alert on high capture rejection rate
  - Alert on Gemini API failures
  - Alert on database connection pool exhaustion
  - **Infrastructure:** External monitoring system
  - **Benefit:** Proactive issue detection

### User Experience

- [ ] **Add loading states for slow operations**
  - Show progress for photo upload
  - Show progress for shelf scan processing
  - Add skeleton loaders for dashboard
  - **Files:** `backend/web/components/**/*.tsx`
  - **Benefit:** Better perceived performance

- [ ] **Improve error messages for capture rejections**
  - Use friendlier language in coaching tips
  - Add visual examples of good/bad captures
  - Prioritize most critical fix first
  - **File:** `backend/skinproof/capture.py:40-66`
  - **Benefit:** Higher capture success rate

- [ ] **Add tooltips for technical terms**
  - Explain "noise floor" to users
  - Explain "stabilization window" concept
  - Explain confidence scores
  - **Files:** `backend/web/app/**/*.tsx`
  - **Benefit:** Better user understanding

### Type Safety

- [ ] **Tighten TypeScript `any` types**
  - Review and type all `any` usages
  - Add strict null checks
  - Enable `noImplicitAny` in tsconfig
  - **Files:** `backend/web/**/*.ts`
  - **Benefit:** Fewer runtime type errors

- [ ] **Add Pydantic models for database rows**
  - Replace `row_dict()` with typed models
  - Add validation on database reads
  - Consider SQLAlchemy ORM adoption
  - **Files:** `backend/skinproof/db.py`, `postgres_db.py`
  - **Benefit:** Type safety for database layer

### Developer Experience

- [ ] **Add development seed data script**
  - Create realistic test users
  - Generate sample products
  - Create example captures and verdicts
  - **New File:** `backend/scripts/seed_dev_data.py`
  - **Benefit:** Faster local development setup

- [ ] **Add API integration tests for frontend**
  - Mock API responses in tests
  - Test error handling paths
  - Test loading/pending states
  - **Files:** `backend/web/**/*.test.tsx` (new files)
  - **Benefit:** Frontend reliability

- [ ] **Document local development setup**
  - Add DEVELOPMENT.md guide
  - Document environment variable requirements
  - Add troubleshooting section
  - **New File:** `DEVELOPMENT.md`
  - **Benefit:** Easier contributor onboarding

---

## Future Enhancements (Backlog)

### Features

- [ ] **Implement photo comparison view**
  - Side-by-side baseline vs. current
  - Highlight metric changes visually
  - Add slider for before/after

- [ ] **Add batch reprocessing for model updates**
  - Background job for all user captures
  - Progress tracking API
  - Email notification on completion

- [ ] **Implement product recommendation engine**
  - Based on similar user cohorts
  - Ingredient similarity matching
  - Personalized based on skin type

- [ ] **Add social proof features**
  - Anonymous cohort statistics
  - Popular products in user's category
  - Effectiveness ratings

### Infrastructure

- [ ] **Add blue-green deployment support**
  - Zero-downtime deployment strategy
  - Database migration coordination
  - Health check verification

- [ ] **Implement backup and disaster recovery**
  - Automated database backups
  - Point-in-time recovery testing
  - Photo storage replication

- [ ] **Add performance benchmarking suite**
  - Load testing scenarios
  - Database query performance baselines
  - API response time SLAs

---

## Completed Items ✓

*Items will be moved here as they are completed*

---

## Notes

- Review this tracker quarterly
- Update priorities based on user feedback
- Track completion in version control commits
- Link related issues to specific commits

**Last Updated:** 2026-08-31
