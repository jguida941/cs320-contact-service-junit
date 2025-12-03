# Observability Infrastructure

> **Note**: This document covers request tracing, logging, and rate-limiting implementation. For formal decisions, see ADR-0039, ADR-0040, and ADR-0041.

## Filter Chain Order

```
Request → CorrelationIdFilter(1) → RateLimitingFilter(5) → RequestLoggingFilter(10) → JwtAuthFilter → Controllers
```

## CorrelationIdFilter

**Location**: `src/main/java/contactapp/config/CorrelationIdFilter.java`

- Extracts `X-Correlation-ID` header or generates UUID if missing/invalid.
- Stores in SLF4J MDC for automatic inclusion in all log entries.
- Sanitizes IDs: max 64 chars, alphanumeric + hyphens/underscores only (prevents log injection).

### Test Scenario Coverage

| Test | Description |
|------|-------------|
| `doFilterInternal_preservesValidHeader` | Verifies existing correlation IDs are propagated |
| `doFilterInternal_generatesIdWhenHeaderMissing` | Confirms UUID generation for missing headers |
| `doFilterInternal_generatesIdWhenHeaderInvalid` | Proves invalid characters trigger regeneration |
| `doFilterInternal_rejectsOverlyLongIds` | Tests the max length boundary rejection |
| `doFilterInternal_acceptsIdAtMaxLength` | Confirms 64-char IDs are accepted |
| `doFilterInternal_rejectsIdJustOverMaxLength` | Confirms 65-char IDs are rejected (boundary) |
| `doFilterInternal_trimsValidHeaderBeforePropagating` | Verifies whitespace trimming |

## RequestLoggingFilter

**Location**: `src/main/java/contactapp/config/RequestLoggingFilter.java`

- Logs HTTP method, URI, sanitized query string, masked client IP, and user agent.
- Configurable via `logging.request.enabled` property.
- Sensitive query parameters masked: `token`, `password`, `api_key`, `secret`, `auth`.
- IPv4 last octet masked (e.g., `192.168.1.***`), IPv6 fully masked.

### Test Scenario Coverage

| Test | Description |
|------|-------------|
| `doFilterInternal_logsRequestAndResponseWhenEnabled` | Verifies logging output format |
| `doFilterInternal_skipsLoggingWhenDisabled` | Confirms disabled filter passes through silently |
| `doFilterInternal_includesQueryStringInLogWhenPresent` | Tests query string inclusion |
| `doFilterInternal_logsRequestWithoutQueryString` | Confirms clean output without query params |
| `doFilterInternal_logsDurationInResponse` | Verifies duration is captured in milliseconds |
| `maskClientIp_masksIpv4LastOctet` | Proves IPv4 masking preserves first 3 octets |
| `maskClientIp_masksIpv6Addresses` | Confirms IPv6 addresses are fully masked |
| `sanitizeQueryString_redactsSensitiveParameters` | Tests sensitive param masking (token, password, etc.) |
| `sanitizeQueryString_returnsNullForBlankInput` | Confirms null handling |
| `sanitizeUserAgent_stripsControlCharacters` | Prevents log injection via user agent |

## RateLimitingFilter (ADR-0039)

**Location**: `src/main/java/contactapp/config/RateLimitingFilter.java`

**Token bucket algorithm** via Bucket4j with Caffeine bounded caches:

| Endpoint | Limit | Key |
|----------|-------|-----|
| Login | 5 req/min | Per IP |
| Register | 3 req/min | Per IP |
| API | 100 req/min | Per authenticated user |

**Response**: HTTP 429 with `Retry-After` header and JSON error body.

### Test Scenario Coverage

| Test | Description |
|------|-------------|
| `testLoginEndpoint_allowsRequestsWithinLimit` | Verifies requests under limit pass |
| `testLoginEndpoint_blocksRequestsExceedingLimit` | Confirms 429 after limit exceeded |
| `testLoginEndpoint_separateLimitsPerIp` | Proves each IP has independent bucket |
| `testRegisterEndpoint_enforcesRateLimit` | Tests registration endpoint limits |
| `testApiEndpoint_enforcesRateLimitPerUser` | Verifies per-user API limiting |
| `testApiEndpoint_separateLimitsPerUser` | Proves user isolation |
| `testApiEndpoint_passesThoughWhenNotAuthenticated` | Confirms unauthenticated passthrough |
| `testNonRateLimitedPath_alwaysPassesThrough` | Tests excluded paths |
| `testXForwardedForHeader_usedForIpExtraction` | Verifies proxy header support |
| `testXForwardedForHeader_handlesMultipleIps` | Tests comma-separated IP handling |
| `testRetryAfterHeader_setCorrectly` | Confirms Retry-After header format |
| `testClearBuckets_resetsAllLimits` | Tests bucket reset functionality |
| `testJsonResponseFormat` | Verifies 429 response body structure |
| `calculateWaitTimeUsesProbeEstimate` | Tests wait time calculation |
| `calculateWaitTime_returnsAtLeastOneSecond` | Confirms minimum 1s retry window |
| `clearBuckets_resetsBucketsAndAllowsNewRequests` | Verifies reset allows new requests |
| `calculateWaitTimeReturnsOneWhenTokensAvailable` | Edge case coverage |
| `testBucketCounting` | Verifies bucket token consumption |

## PiiMaskingConverter (ADR-0041)

**Location**: `src/main/java/contactapp/config/PiiMaskingConverter.java`

Custom Logback converter that masks PII in log messages:
- Phone numbers: shows last 4 digits (`***-***-1234`)
- Addresses: preserves city/state, masks street/zip (`*** Cambridge, MA ***`)

### Test Scenario Coverage

| Test | Description |
|------|-------------|
| `convert_masksPhonesAndAddresses` | Tests combined phone + address masking |
| `convert_masksSevenDigitPhoneFallback` | Verifies non-10-digit phone handling |
| `convert_handlesNullMessagesGracefully` | Confirms null safety |

## RequestUtils

**Location**: `src/main/java/contactapp/config/RequestUtils.java`

Utility for client IP extraction supporting reverse proxies.

**Header priority**: `X-Forwarded-For` → `X-Real-IP` → `RemoteAddr`

### Test Scenario Coverage

| Test | Description |
|------|-------------|
| `getClientIp_prefersFirstXForwardedForEntry` | Tests header priority |
| `getClientIp_usesXRealIpWhenForwardedForMissing` | Confirms fallback chain |
| `getClientIp_fallsBackToRemoteAddress` | Tests final fallback |
| `getClientIp_returnsUnknownWhenNoSourcesPresent` | Confirms default handling |

## JacksonConfig

**Location**: `src/main/java/contactapp/config/JacksonConfig.java`

### Test Scenario Coverage

| Test | Description |
|------|-------------|
| `objectMapperRejectsBooleanCoercion` | Confirms strict schema compliance |
| `objectMapperRejectsNumericCoercion` | Verifies string field type enforcement |

## Log Sanitization Strategy

All logging methods use inline validation to prevent log injection (CWE-117):

```java
private String sanitizeForLog(String value) {
    if (value == null) return "[null]";
    if (value.isBlank()) return "[empty]";
    String sanitized = value.replaceAll("[\\r\\n]", " ")
                            .replaceAll("[^A-Za-z0-9 .:@/_-]", "");
    if (sanitized.length() > 120) {
        sanitized = sanitized.substring(0, 120);
    }
    return value.equals(sanitized) ? value : "[unsafe-value]";
}
```

**Key protections:**
- Strip CR/LF and control characters before logging
- Validate against safe character patterns
- Truncate overly long values (max 120 chars)
- Return safe placeholder values for invalid input

## Related ADRs

- [ADR-0039](../../adrs/ADR-0039-phase5-security-observability.md) - Phase 5 Security and Observability
- [ADR-0040](../../adrs/ADR-0040-request-tracing-and-logging.md) - Request Tracing and Logging
- [ADR-0041](../../adrs/ADR-0041-pii-masking-in-logs.md) - PII Masking in Log Output