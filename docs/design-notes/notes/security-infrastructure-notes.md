# Security Infrastructure

> **Note**: This document consolidates security implementation details. For formal decisions, see ADR-0018, ADR-0038, ADR-0039, ADR-0040, ADR-0041, and ADR-0043.

## Authentication Stack

### JWT Authentication (ADR-0018, ADR-0038)

**Token Flow**:
1. User submits credentials to `POST /api/auth/login`
2. Server validates credentials via `AuthenticationManager` + BCrypt
3. `JwtService` generates HMAC-SHA256 token
4. Token returned in HttpOnly `auth_token` cookie (30-minute TTL)
5. Subsequent requests: `JwtAuthenticationFilter` extracts token from cookie (or `Authorization: Bearer` header fallback)

**Configuration** (`application.yml`):
- `jwt.secret` - Base64-encoded secret (mandatory in production)
- `jwt.expiration=1800000` - 30 minutes default
- `app.auth.cookie.secure` - Separate from servlet session cookie

### Cookie Security (ADR-0043)

**Migration from localStorage**: Moved from localStorage (XSS vulnerability FRONTEND-SEC-01) to HttpOnly cookies.

| Attribute | Value | Purpose |
|-----------|-------|---------|
| HttpOnly | true | Prevents JavaScript access (XSS mitigation) |
| SameSite | Lax | CSRF protection for cross-origin requests |
| Secure | true (prod) | HTTPS only in production |

### CSRF Protection

**Double-submit cookie pattern**:
1. `SpaCsrfTokenRequestHandler` sets `XSRF-TOKEN` cookie
2. Frontend reads cookie value via JavaScript
3. Frontend sends value in `X-XSRF-TOKEN` header for state-changing requests
4. Server validates header matches cookie

**Endpoint**: `GET /api/auth/csrf-token` for explicit token retrieval.

## Per-User Data Isolation

All domain entities have `user_id` foreign keys. Services scope queries to the authenticated user:

```java
// Example from ContactService
public List<Contact> getAllContacts() {
    User currentUser = getCurrentUser();
    return store.findAllByUser(currentUser);
}
```

**Admin Override**: `?all=true` parameter (deprecated, use `POST /api/v1/admin/query` instead) allows admins to see all users' data.

## Rate Limiting (ADR-0039)

**Token bucket algorithm** via Bucket4j with Caffeine bounded caches:

| Endpoint | Limit | Key |
|----------|-------|-----|
| Login | 5 req/min | Per IP |
| Register | 3 req/min | Per IP |
| API | 100 req/min | Per authenticated user |

**Response**: HTTP 429 with `Retry-After` header and JSON error body.

## Request Tracing (ADR-0040)

### Filter Chain Order
```
Request → CorrelationIdFilter(1) → RateLimitingFilter(5) → RequestLoggingFilter(10) → JwtAuthFilter → Controllers
```

### Correlation IDs
- `CorrelationIdFilter` extracts `X-Correlation-ID` header or generates UUID
- Stored in SLF4J MDC for automatic inclusion in all log entries
- Sanitized: max 64 chars, alphanumeric + hyphens/underscores only

### Request Logging
- `RequestLoggingFilter` logs method, URI, masked query string, masked client IP
- Sensitive query parameters masked: `token`, `password`, `api_key`, `secret`, `auth`
- IPv4 last octet masked (e.g., `192.168.1.***`), IPv6 fully masked

## PII Masking (ADR-0041)

`PiiMaskingConverter` (Logback converter) masks sensitive data in log output:
- Phone numbers: shows last 4 digits (`***-***-1234`)
- Addresses: preserves city/state, masks street/zip (`*** Cambridge, MA ***`)

## Security Headers (SecurityConfig)

| Header | Value | Purpose |
|--------|-------|---------|
| Content-Security-Policy | script/style/img/font/connect/frame-ancestors/form-action/base-uri/object-src | XSS mitigation |
| Permissions-Policy | Disables geolocation, camera, microphone | Feature restriction |
| X-Content-Type-Options | nosniff | MIME sniffing prevention |
| X-Frame-Options | SAMEORIGIN | Clickjacking protection |
| Referrer-Policy | strict-origin-when-cross-origin | Referrer leakage prevention |

## CORS Configuration

```yaml
cors:
  allowed-origins: localhost:5173,localhost:8080  # Explicit origins required
```

**Requirements for SPA**:
- Explicit origins (no `*`) with `Access-Control-Allow-Credentials: true`
- `Access-Control-Allow-Headers: Authorization, Content-Type, X-XSRF-TOKEN, X-Request-ID`
- `Access-Control-Expose-Headers: X-Request-ID, X-Trace-ID`

## Password Security

**BCrypt** via Spring Security's `BCryptPasswordEncoder`:
- Portable across CI/dev/prod environments
- FIPS-validated implementations available
- Migration path to Argon2 includes `algorithm` column for dual-algorithm support

**Validation**: Passwords must be pre-hashed (BCrypt pattern `$2[aby]$...`). Raw passwords rejected at entity level.

## Authentication Endpoints

| Endpoint | Method | Description | Response |
|----------|--------|-------------|----------|
| `/api/auth/login` | POST | Authenticate user | 200 + HttpOnly JWT cookie |
| `/api/auth/register` | POST | Register new user | 201 + HttpOnly JWT cookie |
| `/api/auth/logout` | POST | Invalidate session | 204 No Content |
| `/api/auth/csrf-token` | GET | Get CSRF token | XSRF-TOKEN cookie value |

## Role-Based Access Control

- `@PreAuthorize("hasAnyRole('USER', 'ADMIN')")` on protected endpoints
- Admin-only endpoints use `hasRole('ADMIN')`
- Controllers annotated with `@SecurityRequirement(name = "bearerAuth")` for OpenAPI

## Related ADRs

- [ADR-0018](../../adrs/ADR-0018-authentication-and-authorization-model.md) - Authentication and Authorization Model
- [ADR-0038](../../adrs/ADR-0038-authentication-implementation.md) - Authentication Implementation
- [ADR-0039](../../adrs/ADR-0039-phase5-security-observability.md) - Phase 5 Security and Observability
- [ADR-0040](../../adrs/ADR-0040-request-tracing-and-logging.md) - Request Tracing and Logging
- [ADR-0041](../../adrs/ADR-0041-pii-masking-in-logs.md) - PII Masking in Log Output
- [ADR-0043](../../adrs/ADR-0043-httponly-cookie-authentication.md) - HttpOnly Cookie Authentication