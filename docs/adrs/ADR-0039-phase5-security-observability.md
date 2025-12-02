# ADR-0039: Phase 5 Security and Observability Implementation

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: [ADR-0018](ADR-0018-authentication-and-authorization-model.md),
[ADR-0038](ADR-0038-authentication-implementation.md),
[RateLimitingFilter.java](../../src/main/java/contactapp/config/RateLimitingFilter.java),
[CorrelationIdFilter.java](../../src/main/java/contactapp/config/CorrelationIdFilter.java),
[Dockerfile](../../Dockerfile)

## Context
- Phase 5 requires implementing remaining security and observability features beyond basic authentication.
- Per-user data isolation is needed to support multi-tenancy.
- Rate limiting protects against brute force and denial-of-service attacks.
- Structured logging with correlation IDs enables distributed tracing.
- Prometheus metrics support production monitoring.
- Docker packaging enables consistent deployments.

## Decisions

### 1. Per-User Data Isolation

**Implementation**:
- Added `user_id` column (NOT NULL, FK to users.id) to contacts, tasks, appointments tables via Flyway V5 migration.
- Updated JPA entities with `@ManyToOne User` relationship.
- Updated repositories with user-aware query methods (`findByUser`, `findByIdAndUser`, `existsByIdAndUser`, `deleteByIdAndUser`).
- Services automatically filter by authenticated user from `SecurityContextHolder`.
- ADMIN-only exports now use a dedicated `POST /api/v1/admin/query` endpoint with `{"includeAll": true}` in the JSON body plus the `X-Admin-Override: true` header so overrides are explicit, CSRF-protected, and fully auditable. Legacy `?all=true` support exists only during the migration window (target removal: **2026-02-01**) to avoid breaking older clients.

**Data Flow**:
```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant Database

    Client->>Controller: GET /api/v1/contacts
    Controller->>Service: getAllContacts()
    Service->>Service: getCurrentUser() from SecurityContext
    Service->>Repository: findByUser(user)
    Repository->>Database: SELECT * WHERE user_id = ?
    Database-->>Repository: User's contacts only
    Repository-->>Service: List<Contact>
    Service-->>Controller: Filtered contacts
    Controller-->>Client: JSON response
```

### 2. Rate Limiting with Bucket4j

**Configuration**:
| Endpoint | Limit | Window | Window Type | Key |
|----------|-------|--------|-------------|-----|
| /api/auth/login | 5/username, 100/IP | 60 sec | Sliding | Username + IP |
| /api/auth/register | 3 requests | 60 sec | Sliding | IP address |
| /api/v1/** | 100 requests | 60 sec | Sliding | Username |

**Window Type**: All rate limits use a **sliding window** (token bucket algorithm). Tokens replenish
continuously, so a user blocked at T=0 can make 1 request at T=12s (1/5 of the window).

**Design Rationale**:
- Login: Username buckets allow 5 attempts/min so users can fix typos while IP buckets (100/min) detect distributed attacks; repeated failures trigger a 15-minute account lockout.
- Register: 3 attempts/min prevents automated account creation.
- API: 100 req/min supports typical CRUD workflows while preventing resource exhaustion.

**Account Lockout Details**:
- **Trigger**: 5 failed logins for the same username within any rolling 15-minute window
- **Duration**: 15 minutes from the 5th failure
- **HTTP Response**: `401 Unauthorized` with `{"error": "Account temporarily locked", "lockoutUntil": "<ISO timestamp>"}`
- **Counter Reset**: Successful login resets the failed-login counter only; rate-limit buckets refill independently
- **Admin Override**: Support staff can clear locks via audited tooling

**Response Format**:
```json
{
  "error": "Rate limit exceeded. Please try again in 45 seconds.",
  "retryAfter": 45
}
```

### 3. Structured Logging with Correlation IDs

**Components**:
- `CorrelationIdFilter`: Extracts/generates X-Correlation-ID, sets MDC, adds to response.
- `RequestLoggingFilter`: Logs HTTP method, URI, status, duration (configurable via property).
- `PiiMaskingConverter`: Masks phone numbers (***-***-1234) and addresses in log messages.
- `logback-spring.xml`: Profile-specific logging (console in dev, JSON in prod).

**Log Format (Production)**:
```json
{
  "timestamp": "2025-12-01T15:15:23.456Z",
  "level": "INFO",
  "logger": "contactapp.api.ContactController",
  "message": "Creating contact with phone ***-***-1234",
  "correlationId": "abc-123-def",
  "thread": "http-nio-8080-exec-1",
  "application": "contact-service"
}
```

### 4. Prometheus Metrics

**Exposed Endpoints**:
- `/actuator/prometheus` - Prometheus-format metrics
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe

**Custom Tags**:
- `application`: contact-service
- `environment`: local/docker/staging/production

### 5. Security Headers

**Implemented Headers** (in `SecurityConfig.java`):
| Header | Value | Purpose |
|--------|-------|---------|
| Content-Security-Policy | `default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'self'; form-action 'self'; base-uri 'self'; object-src 'none'` | Prevents XSS, clickjacking, and data injection attacks |
| Permissions-Policy | `geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=()` | Disables browser features to reduce attack surface |
| X-Content-Type-Options | `nosniff` | Prevents MIME-type sniffing |
| X-Frame-Options | `SAMEORIGIN` | Prevents clickjacking |
| Referrer-Policy | `strict-origin-when-cross-origin` | Controls referrer header leakage |

**CSP Policy Breakdown**:
- `default-src 'self'`: Only load resources from same origin by default
- `script-src 'self'`: Only execute scripts from same origin (blocks inline scripts)
- `style-src 'self'`: Only allow stylesheet files from same origin (no inline styles) to maximize CSP effectiveness
- `img-src 'self' data:`: Allow images from same origin + data URIs
- `font-src 'self'`: Only load fonts from same origin
- `connect-src 'self'`: Only allow API calls to same origin
- `frame-ancestors 'self'`: Prevent embedding in iframes from other origins
- `form-action 'self'`: Restrict form submissions to same origin
- `base-uri 'self'`: Prevent base tag hijacking
- `object-src 'none'`: Block plugins (Flash, Java applets, etc.)

**Permissions-Policy Rationale**:
All browser features are disabled by default because the current application does not require them.
This maximizes security by reducing attack surface. If future features require specific capabilities:
- Create a feature request documenting the business need
- Security team reviews and approves the change
- Update `SecurityConfig.java` to enable only the required feature (e.g., `payment=(self)`)
- Document the change in this ADR with rationale

### 6. Docker Packaging

**Dockerfile Features**:
- Multi-stage build (builder + runtime)
- Eclipse Temurin 17 JRE runtime
- Layered JAR extraction for optimal caching
- Non-root user (appuser, UID 1001)
- Configurable JVM options via JAVA_OPTS

**docker-compose.yml Stack**:
- `postgres`: PostgreSQL 16 with health checks
- `app`: Spring Boot application with resource limits
- `pgadmin`: Optional database management UI

## Test Infrastructure

### Observability Testing
- **`@AutoConfigureObservability`**: Required for Prometheus actuator tests. Spring Boot test slices disable metrics by default, so this annotation explicitly enables the Micrometer registry for integration tests that verify `/actuator/prometheus` endpoints.
- **Micrometer Dependency**: Use `micrometer-registry-prometheus-simpleclient` (not just `micrometer-registry-prometheus`) for Micrometer 1.13+ compatibility. The simpleclient variant provides the necessary Prometheus exposition format support.

### Database Migration Testing
- **H2 Identity Sequence Reset**: V5 migration includes `ALTER TABLE users ALTER COLUMN id RESTART WITH 2` to avoid conflicts with the system user (id=1) seeded in V4. Without this, H2's auto-increment could attempt to reuse id=1, causing unique constraint violations in tests.

### Test Utilities
- **`@WithMockAppUser`**: Custom Spring Security annotation for tests requiring an authenticated user context. Automatically creates a mock `UserPrincipal` with configurable username, roles, and authorities.
- **`TestUserSetup`**: Test helper that persists User entities to satisfy foreign key constraints when testing contacts, tasks, and appointments. Ensures referential integrity in service and repository tests.
- **`TestUserFactory`**: Factory class for creating unique test users with randomized usernames and emails to avoid collisions in parallel test execution.

## Consequences

### Positive
- Multi-tenant data isolation ensures users only see their own data.
- Rate limiting protects against common attack vectors.
- Correlation IDs enable end-to-end request tracing.
- PII masking prevents sensitive data in logs.
- Prometheus metrics integrate with standard monitoring tools.
- Docker packaging enables consistent deployments.
- Security headers (CSP, X-Content-Type-Options, X-Frame-Options) mitigate XSS and clickjacking.

### Trade-offs
- Per-user isolation adds `instanceof JpaStore` checks in services.
- Rate limit buckets consume memory (mitigated by ConcurrentHashMap).
- JSON logging increases log volume vs plain text.

## Alternatives Considered

### Per-User Isolation
- **Separate databases per user**: Rejected; adds operational complexity.
- **Row-level security in PostgreSQL**: Rejected; requires DB-specific setup.
- **Spring Data JPA filters**: Considered but explicit user parameters are clearer.

### Rate Limiting
- **Spring Cloud Gateway**: Rejected; adds infrastructure dependency.
- **Redis-backed distributed rate limiting**: Deferred; single-instance sufficient for now.
- **Nginx rate limiting**: Rejected; want application-level control.

### Logging
- **Log4j2**: Rejected; Logback is Spring Boot default with adequate features.
- **Elastic APM agent**: Deferred; Prometheus/Micrometer sufficient for Phase 5.

## Files Changed

### New Files
- `src/main/resources/db/migration/V5__add_user_id_columns.sql`
- `src/main/java/contactapp/config/RateLimitConfig.java`
- `src/main/java/contactapp/config/RateLimitingFilter.java`
- `src/main/java/contactapp/config/CorrelationIdFilter.java`
- `src/main/java/contactapp/config/RequestLoggingFilter.java`
- `src/main/java/contactapp/config/PiiMaskingConverter.java`
- `src/main/resources/logback-spring.xml`
- `Dockerfile`
- `docker-compose.yml`
- `.env.example`
- `.dockerignore`

### Modified Files
- All entity classes (added user relationship)
- All repository classes (added user-aware methods)
- All service classes (added user isolation logic)
- All controllers (wired ADMIN overrides through a POST `/api/v1/admin/query` flow with JSON flags + headers instead of `?all=true`)
- `pom.xml` (bucket4j, micrometer-prometheus, logstash-encoder)
- `application.yml` (rate-limit, prometheus, logging config)
