# Cal.com Architecture Blueprint for GlowUp AI

> **Research Date**: August 30, 2026  
> **Source**: Cal.com open-source repository (MIT licensed)  
> **Purpose**: Learn from Cal.com's architectural patterns and apply relevant concepts to GlowUp AI

---

## 🏗️ Cal.com Tech Stack Overview

### Core Technologies
- **Frontend Framework**: Next.js (App Router) + React.js
- **Styling**: Tailwind CSS (utility-first CSS)
- **Backend/API**: tRPC (type-safe RPC framework - no REST endpoints!)
- **Database**: PostgreSQL 13+ via Prisma ORM
- **Runtime**: Node.js 18+
- **Package Manager**: Yarn
- **Monorepo**: Turborepo for workspace coordination
- **Authentication**: NextAuth.js (now Auth.js)
- **Testing**: Vitest (unit) + Playwright (e2e)
- **Code Quality**: Biome (linting/formatting)

### Why This Stack Matters for GlowUp AI

**✅ What Translates Well:**
1. **Type Safety End-to-End**: tRPC eliminates API contract mismatches - changes in backend automatically reflect in frontend types
2. **Prisma ORM**: Schema-driven migrations make database evolution safe and trackable
3. **Monorepo Structure**: Shared code (UI components, types, utilities) stays DRY across web/mobile
4. **Modern React**: Next.js App Router + React Server Components = great performance

**❌ What Doesn't Apply:**
- You already have FastAPI backend (Python) - no need to switch to tRPC/Node.js
- Your Android app is native Kotlin/Compose, not React Native/web
- However, patterns still apply: type safety, layered architecture, shared utilities

---

## 📁 Cal.com Monorepo Architecture

```
cal.com/
├── apps/                           # Application layer
│   ├── web/                        # Main Next.js web app
│   │   ├── app/                    # Next.js App Router pages
│   │   ├── components/             # Page-specific components
│   │   ├── lib/                    # App-specific utilities
│   │   └── modules/                # Feature modules
│   ├── api/                        # Standalone API service
│   └── docs/                       # Documentation site
│
├── packages/                       # Shared libraries
│   ├── prisma/                     # Database schema & migrations
│   │   └── schema.prisma           # Single source of truth for DB
│   ├── trpc/                       # API layer (type-safe RPC)
│   ├── ui/                         # Shared React component library
│   ├── types/                      # Shared TypeScript types
│   ├── lib/                        # Core utilities (185 modules!)
│   ├── features/                   # Feature-specific logic (74 modules!)
│   │   ├── auth/
│   │   ├── bookings/
│   │   ├── calendars/
│   │   ├── eventtypes/
│   │   └── ...
│   ├── emails/                     # Email templates & sending
│   ├── i18n/                       # Internationalization
│   ├── app-store/                  # Plugin/integration system
│   └── ...
```

### Key Architectural Patterns

#### 1. **Feature-Based Organization**
Cal.com organizes code by **feature**, not by technical layer:
```
packages/features/bookings/
├── lib/                  # Business logic
├── components/           # UI components
├── hooks/                # React hooks
├── utils/                # Utilities
└── types.ts              # Types for this feature
```

**Why This Works:**
- All booking-related code lives together
- Easy to find everything related to a feature
- Can extract features into separate packages easily
- Reduces cognitive load (don't jump between folders)

**Apply to GlowUp AI:**
Your Android app already does this! Each feature has its own package:
- `feature/capture/` - everything capture-related
- `feature/routine/` - everything routine-related
- ✅ Keep this pattern!

#### 2. **Repository + Service Pattern**
Cal.com uses explicit layers:
- **Repository**: Data access (talks to Prisma/DB)
- **Service**: Business logic (orchestrates repositories)
- **Controller**: API endpoints (calls services)

Example:
```typescript
// Repository (data layer)
class BookingRepository {
  async findById(id: string) {
    return prisma.booking.findUnique({ where: { id } });
  }
}

// Service (business logic)
class BookingService {
  constructor(private bookingRepo: BookingRepository) {}
  
  async confirmBooking(id: string) {
    const booking = await this.bookingRepo.findById(id);
    // ... validation, email notifications, calendar sync ...
    return this.bookingRepo.update(id, { status: 'confirmed' });
  }
}

// tRPC Router (API endpoint)
export const bookingRouter = router({
  confirm: procedure
    .input(z.object({ id: z.string() }))
    .mutation(async ({ input }) => {
      return bookingService.confirmBooking(input.id);
    }),
});
```

**Apply to GlowUp AI:**
Your backend has this! `complete_service.py` is the service layer, but you could formalize it more:
```python
# Current structure
complete_service.py  # 1 big service (4000+ lines?)

# Could evolve to:
services/
  ├── capture_service.py
  ├── routine_service.py
  ├── insights_service.py
  └── billing_service.py

repositories/
  ├── capture_repository.py
  ├── user_repository.py
  └── product_repository.py
```

#### 3. **Type-Safe API with tRPC**
Cal.com's tRPC setup means:
- Backend defines procedure → Frontend gets types automatically
- No manual API client code
- No runtime errors from wrong payloads
- Auto-complete everywhere

**Your Current Setup (FastAPI + Retrofit):**
```kotlin
// Android needs manual DTOs
data class CaptureCreate(
    val user_id: String,
    val image_base64: String,
    // ... must match backend exactly
)

// If backend changes field name, Android won't know until runtime!
```

**Improvement Options:**
1. **Generate Kotlin DTOs from OpenAPI** - FastAPI generates OpenAPI schema → use `openapi-generator` to create Kotlin DTOs
2. **Add runtime validation** - Use Pydantic on backend + kotlinx.serialization validation on Android
3. **Contract testing** - Write tests that verify Android DTOs match backend schemas

---

## 🗄️ Database Architecture (Prisma)

Cal.com's Prisma schema is their **single source of truth**. Key patterns:

### Migration Strategy
```prisma
// schema.prisma defines everything
model User {
  id        Int      @id @default(autoincrement())
  email     String   @unique
  name      String?
  createdAt DateTime @default(now())
  bookings  Booking[]
}

model Booking {
  id        Int      @id @default(autoincrement())
  userId    Int
  user      User     @relation(fields: [userId], references: [id])
  startTime DateTime
  status    BookingStatus
  
  @@index([userId])
  @@index([startTime])
}
```

**Migrations:**
1. Change schema.prisma
2. Run `prisma migrate dev --name add_booking_status`
3. Prisma generates SQL migration file
4. Commit both schema + migration to git

**Your Current Setup:**
- Backend has `migrations/0001_*.sql` through `0004_*.sql`
- Applied on startup in order
- ✅ Similar pattern! But could add:
  - Schema definition file (like Prisma schema)
  - Rollback migrations (down migrations)
  - Better migration naming (what each migration does)

### Indexes & Performance
Cal.com adds indexes strategically:
```prisma
@@index([userId])           // Foreign key lookups
@@index([startTime])        // Time-based queries
@@index([email, deletedAt]) // Composite for soft deletes
```

**Apply to GlowUp AI:**
Check if your captures/metrics tables have indexes on:
- `user_id` (foreign keys)
- `captured_at` (time-based queries)
- `experiment_id` (filtering)

---

## 🔌 Cal.com's Plugin System (App Store)

One of Cal.com's most powerful features: **150+ integrations** as plugins

### How It Works
```
packages/app-store/
├── google-calendar/
│   ├── api/              # Backend integration code
│   ├── components/       # Settings UI
│   ├── lib/              # Utility functions
│   └── package.json      # App metadata
├── zoom/
├── stripe/
└── ...
```

Each app is self-contained with:
- Credential storage (OAuth tokens, API keys)
- Webhook handlers
- UI for configuration
- Metadata (name, description, category)

**Why This Matters for GlowUp AI:**

You could build a similar plugin system for:
- **Skincare Brands**: Cerave, The Ordinary, Curology integration
- **Wearables**: Oura ring, Apple Health, Google Fit (track sleep, stress)
- **E-commerce**: Amazon affiliate links, Sephora API
- **Dermatologists**: Export reports, telehealth integration

Example structure:
```
backend/integrations/
├── amazon_affiliate/
│   ├── api.py
│   ├── products.py
│   └── config.py
├── apple_health/
└── the_ordinary/
```

---

## 📱 Frontend Architecture Lessons

### Component Library (`packages/ui/`)
Cal.com has a shared component library used across all apps:
- `Button`, `Input`, `Dialog`, `Dropdown`
- Form components with validation
- Layout primitives
- Consistent design tokens

**Your Android App:**
You already have `core/ui/` with reusable components! ✅
- GlowButton, GlowCard, StatTile, etc.
- Honey design system tokens

Keep building this out - every new screen should use existing components first.

### Form Handling
Cal.com uses `react-hook-form` + `zod` for type-safe forms:
```typescript
const schema = z.object({
  email: z.string().email(),
  name: z.string().min(2),
});

const form = useForm({ resolver: zodResolver(schema) });
```

**Your Android App:**
Consider adding form validation library or DSL:
```kotlin
// Current: manual validation scattered everywhere
if (email.isBlank()) { /* error */ }

// Could use: inline validation DSL
val form = form {
  field(email) { notBlank() and emailFormat() }
  field(name) { minLength(2) }
}
```

---

## 🔐 Authentication & Authorization

Cal.com uses **NextAuth.js** (now Auth.js) for:
- Email/password auth
- OAuth providers (Google, GitHub, etc.)
- Session management
- CSRF protection

**Your Current Setup:**
- Firebase Auth (Email/Password + Google)
- Bearer token in requests
- Backend verifies via JWKS

✅ Similar security model! Both use:
1. OAuth for social login
2. JWT tokens for API auth
3. Server-side token verification

**Enhancement Idea:**
Add **refresh token rotation** (Firebase supports this):
```kotlin
// When access token expires (60 min)
if (response.code == 401) {
  val newToken = auth.currentUser?.getIdToken(forceRefresh = true)
  // Retry request with new token
}
```

---

## 🧪 Testing Strategy

### Unit Tests (Vitest)
Cal.com tests:
- Repository methods
- Service business logic
- Utility functions
- Schema validation

### E2E Tests (Playwright)
Cal.com tests:
- Full booking flow
- Calendar sync
- Payment processing
- Email notifications

**Your Backend:**
- 58 tests passing ✅
- Covers API endpoints + service logic

**Your Android App:**
- Unit tests exist (testDebugUnitTest)
- No e2e tests yet

**Recommendation:**
Add e2e tests for critical flows:
1. Sign up → Consent → First capture → View results
2. Add product → Log routine → Check timeline
3. Upgrade to Premium → Access locked features

---

## 📈 Scaling Patterns from Cal.com

### 1. **Background Jobs**
Cal.com uses **Inngest** for async jobs:
- Send confirmation emails
- Sync calendar events
- Process webhooks
- Cleanup old data

**Your App:**
You have WorkManager for:
- Upload captures from offline outbox
- Send reminders

Could add more jobs:
- Clean up old raw photos (SKINPROOF_RAW_RETENTION_DAYS)
- Reprocess captures with new model
- Send weekly recap emails
- Generate insights reports

### 2. **Caching Strategy**
Cal.com caches:
- User sessions (Redis)
- Availability calculations
- Integration credentials

**Your App:**
You have:
- Room database for offline data
- Memory cache in repositories (KeyedMemoryCache)

Could add:
- Redis for backend API caching
- CDN for serving photos (if using S3/GCS)

### 3. **Rate Limiting**
Cal.com API has:
- 120 requests/minute baseline
- Per-user rate limits
- WAF protection

**Your Backend:**
Currently no rate limiting! 

Add using fastapi-limiter:
```python
from fastapi_limiter import FastAPILimiter
from fastapi_limiter.depends import RateLimiter

@app.post("/api/captures")
async def create_capture(
    ...,
    _: None = Depends(RateLimiter(times=10, seconds=60))
):
    # Max 10 captures per minute per user
```

---

## 🎯 Key Takeaways for GlowUp AI

### ✅ What You're Already Doing Right (Inspired by Cal.com)
1. **Feature-based organization** - Android app structure matches Cal.com's pattern
2. **Shared UI components** - `core/ui/` is like Cal.com's `packages/ui/`
3. **Type safety** - Kotlin + Hilt DI provides compile-time safety
4. **Layered architecture** - Repository + ViewModel + UI separation
5. **Comprehensive testing** - Backend has good test coverage

### 🚀 What to Add (Learned from Cal.com)
1. **OpenAPI → Kotlin code generation** - Eliminate manual DTO sync
2. **Background job system** - Formal job queue for async tasks
3. **Rate limiting** - Protect backend from abuse
4. **Plugin/integration system** - Make brand/service integrations first-class
5. **Better migration system** - Named migrations with rollback support
6. **E2E testing** - Critical user flows tested end-to-end
7. **Monitoring & observability** - Structured logging, error tracking, metrics

### 💡 Future Scaling Ideas
1. **Multi-tenant architecture** - Support clinics/dermatologists managing multiple patients
2. **API v2 for partners** - Let skincare brands integrate with GlowUp AI
3. **Webhook system** - Notify external systems of events (new capture, experiment complete)
4. **Admin dashboard** - For support team to help users
5. **Data export API** - Let users export to other health tracking apps

---

## 📋 Action Items Priority

### P0 - Before Production Launch
- [ ] Add rate limiting to backend API
- [ ] Set up error tracking (Sentry, Rollbar, etc.)
- [ ] Add structured logging with request IDs
- [ ] Implement refresh token rotation
- [ ] Add e2e tests for critical flows

### P1 - First 3 Months Post-Launch
- [ ] Set up OpenAPI → Kotlin code generation
- [ ] Formalize background job system
- [ ] Add Redis caching for backend
- [ ] Build admin dashboard
- [ ] Add database migration rollback support

### P2 - Growth Phase (6-12 months)
- [ ] Plugin system for brand integrations
- [ ] API v2 for partners
- [ ] Multi-tenant support (clinics)
- [ ] Webhook system
- [ ] Data export API

---

## 🔗 Resources

- [Cal.com GitHub](https://github.com/calcom/cal.com) - Open source repository
- [Cal.com Docs](https://cal.com/docs) - API documentation
- [tRPC](https://trpc.io/) - Type-safe API framework
- [Prisma](https://prisma.io/) - Modern ORM
- [Turborepo](https://turbo.build/) - Monorepo build system

---

**Next**: Combine this with skincare/wellness app research to create final GlowUp AI growth strategy.
