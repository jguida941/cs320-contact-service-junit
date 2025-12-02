# ADR-0040: Request Tracing and Logging Infrastructure

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: [ADR-0039](ADR-0039-phase5-security-observability.md),
[CorrelationIdFilter.java](../../src/main/java/contactapp/config/CorrelationIdFilter.java),
[RequestLoggingFilter.java](../../src/main/java/contactapp/config/RequestLoggingFilter.java),
[RequestUtils.java](../../src/main/java/contactapp/config/RequestUtils.java)

## Context

Production systems require comprehensive request tracing for debugging, monitoring, and audit purposes. This ADR documents the implementation of the request tracing infrastructure consisting of three components:

1. **CorrelationIdFilter** - Generates/propagates unique request identifiers
2. **RequestLoggingFilter** - Logs request/response metadata for audit trails
3. **RequestUtils** - Extracts client IP addresses from proxy headers

## Decisions

### 1. Correlation ID Strategy

**Implementation**: `CorrelationIdFilter` (Order = 1, highest precedence)

The filter extracts the `X-Correlation-ID` header from incoming requests or generates a UUID if missing. The ID is:
- Stored in SLF4J MDC for automatic inclusion in all log entries
- Added to the response header for client-side tracking
- Cleaned up after request completion to prevent thread pollution

**Validation Rules**:

| Rule          | Value             | Rationale                                                                             |
|---------------|-------------------|---------------------------------------------------------------------------------------|
| Max length    | 64 characters     | UUIDs are 36 chars; allows flexibility while preventing<br> DoS via oversized headers |
| Character set | `[a-zA-Z0-9\-_]+` | Alphanumeric plus hyphens/underscores; prevents log <br> injection attacks            |
        

**Rejected IDs**: Invalid correlation IDs (too long, invalid characters, empty) trigger UUID generation rather than error responses. This ensures tracing always works even with malformed client headers.

### 2. Request Logging Strategy

**Implementation**: `RequestLoggingFilter` (Order = 10, after correlation ID)

The filter logs HTTP request/response metadata when enabled via `logging.request.enabled=true`.

**Logged Fields**:

| Field        | Source                       | Sanitation                  |
|--------------|------------------------------|-----------------------------|
| Method       | `request.getMethod()`        | None                        |
| URI          | `request.getRequestURI()`    | None                        |
| Query String | `request.getQueryString()`   | Sensitive params masked     |
| Client IP    | `RequestUtils.getClientIp()` | Last octet masked           |
| User Agent   | Header `User-Agent`          | Control characters stripped |
| Status       | `response.getStatus()`       | None                        |
| Duration     | Calculated                   | Milliseconds                |


**Sensitive Query Parameters** (automatically masked with `***`):
- `token`, `access_token`, `refresh_token`
- `password`, `pass`, `pwd`
- `api_key`, `apikey`
- `sessionid`, `session_id`
- `secret`, `auth`

**Log Format**:
```
HTTP Request: method=GET uri=/api/v1/contacts query=all=true clientIp=192.168.1.*** userAgent=Mozilla/5.0...
HTTP Response: status=200 duration=45ms
```

### 3. Client IP Detection

**Implementation**: `RequestUtils.getClientIp()`

Extracts the originating client IP behind reverse proxies:

**Header Priority**:
1. `X-Forwarded-For` (standard load balancer header) - takes first IP if comma-separated
2. `X-Real-IP` (nginx default header)
3. `request.getRemoteAddr()` (direct connection fallback)

**Security Considerations**:
- In production behind a reverse proxy, the proxy MUST overwrite (not append to) `X-Forwarded-For` to prevent client spoofing
- Headers with value "unknown" are skipped (some proxies set this for unavailable IPs)

**IP Masking**:
| IP Type | Masking |
|---------|---------|
| IPv4 | First 3 octets shown, last masked: `192.168.1.***` |
| IPv6 | Fully masked as `masked` |
| Empty/null | Returns `unknown` |
| Other formats | Returns `masked` |

## Filter Ordering

Filters execute in this order to ensure correlation IDs are available for logging:

```
Request → CorrelationIdFilter(1) → RateLimitingFilter(5) → RequestLoggingFilter(10) → ... → Controller
```

## Configuration

```yaml
# application.yml
logging:
  request:
    enabled: true  # Enable request/response logging
```

**Environment Recommendations**:

| Environment | `logging.request.enabled`                           |
|-------------|-----------------------------------------------------|
| Development | `true` - verbose debugging                          |
| Staging     | `true` - validate production behavior               |
| Production  | false` or rate-limited - performance and log volume |


## Test Coverage

| Class                      | Tests | Coverage Focus                                                                           |
|----------------------------|-------|------------------------------------------------------------------------------------------|
| `CorrelationIdFilterTest`  | 7     | UUID generation, header propagation, MDC lifecycle, boundary validation (64 vs 65 chars) |
| `RequestLoggingFilterTest` | 8     | Enabled/disabled modes, query string sanitization, IP masking, duration logging          |
| `RequestUtilsTest`         | 6     | Header priority, comma-separated IPs, null handling, "unknown" values                    |

## Consequences

### Positive
- End-to-end request tracing via correlation IDs
- Audit trails for compliance without exposing sensitive data
- Consistent IP detection across all filters
- Configurable logging per environment

### Trade-offs
- RequestLoggingFilter adds overhead (content caching wrappers)
- Query string parsing adds CPU cycles
- Correlation IDs in responses may leak internal tracing info (acceptable for debugging)

## Alternatives Considered

### Correlation ID
- **Spring Cloud Sleuth**: Rejected; adds unnecessary distributed tracing complexity for a monolith
- **Micrometer Tracing**: Considered but deferred; current MDC approach is simpler

### Request Logging
- **Spring Boot AccessLog**: Rejected; doesn't support MDC correlation or PII masking
- **Logstash Encoder only**: Rejected; lacks pre-response query sanitization

### IP Detection
- **Spring's `ForwardedHeaderFilter`**: Considered; more comprehensive but requires careful configuration to avoid security issues. Custom implementation provides explicit control.
