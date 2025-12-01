# ADR-0041: PII Masking in Log Output

**Status**: Accepted
**Date**: 2025-12-01
**Owners**: Justin Guida

**Related**: [ADR-0039](ADR-0039-phase5-security-observability.md),
[ADR-0040](ADR-0040-request-tracing-and-logging.md),
[PiiMaskingConverter.java](../../src/main/java/contactapp/config/PiiMaskingConverter.java),
[logback-spring.xml](../../src/main/resources/logback-spring.xml)

## Context

Logging sensitive data (phone numbers, addresses) creates compliance risks (GDPR, CCPA) and security vulnerabilities. This ADR documents the PII masking strategy that automatically redacts sensitive patterns in log output.

## Decisions

### 1. Masking Implementation

**Approach**: Custom Logback converter that processes log messages before output.

The `PiiMaskingConverter` extends Logback's `MessageConverter` and applies regex-based pattern matching to redact:
1. **Phone numbers** - Preserves last 4 digits for debugging correlation
2. **Street addresses** - Preserves city and state for geographic context

### 2. Phone Number Masking

**Pattern**: Matches 10-digit phone numbers in various formats.

| Input Format | Example | Output |
|--------------|---------|--------|
| Raw digits | `6175551234` | `***-***-1234` |
| Hyphenated | `617-555-1234` | `***-***-1234` |
| Parentheses | `(617) 555-1234` | `***-***-1234` |
| Dots | `617.555.1234` | `***-***-1234` |
| Non-10-digit | `123456789` | `***-***-****` |

**Regex**: `\b(?:\(?\d{3}\)?[-.\s]?)?\d{3}[-.\s]?\d{4}\b`

**Design Rationale**:
- Last 4 digits retained for support ticket correlation (common practice)
- Non-10-digit numbers fully masked to avoid false positives

### 3. Address Masking

**Pattern**: Matches US street address formats with city, state, and optional ZIP.

| Input | Output |
|-------|--------|
| `123 Main St, Cambridge, MA 02139` | `*** Cambridge, MA ***` |
| `456 Oak Avenue, Boston, MA` | `*** Boston, MA ***` |

**Preserved**: City and state (useful for geographic debugging)
**Masked**: Street number/name and ZIP code (precise location data)

**Regex**: Street type keywords (St, Street, Ave, Avenue, Rd, Road, Blvd, Dr, Ln, Way, Ct, Court) followed by city, 2-letter state code, and optional ZIP.

### 4. Logback Configuration

```xml
<!-- logback-spring.xml -->
<conversionRule conversionWord="pii"
                converterClass="contactapp.config.PiiMaskingConverter" />

<pattern>%d{ISO8601} [%thread] %-5level %logger - %pii%n</pattern>
```

The `%pii` conversion word replaces the standard `%msg` to enable masking.

### 5. Profile-Specific Logging

| Profile | Format | PII Masking |
|---------|--------|-------------|
| `default`, `dev` | Console (human-readable) | Enabled |
| `prod`, `docker` | JSON (machine-parseable) | Enabled |
| `test` | Console (minimal) | Disabled for test assertions |

## Test Coverage

| Class | Tests | Coverage Focus |
|-------|-------|----------------|
| `PiiMaskingConverterTest` | 8 | Phone formats, address patterns, edge cases, null handling |

**Key Test Cases**:
- 10-digit phone in various formats → last 4 shown
- Non-10-digit phone → fully masked
- Street addresses with different keywords → city/state preserved
- Messages without PII → unchanged
- Null messages → returns null (no NPE)

## Consequences

### Positive
- Automatic PII protection without developer effort
- Compliance with data protection regulations
- Retains debugging context (last 4 digits, city/state)
- Works transparently with existing logging statements

### Trade-offs
- Regex processing adds CPU overhead per log statement
- False positives possible (e.g., "123 Main St" in unrelated text)
- Phone pattern is US-centric (international formats may escape masking)

### Limitations
- Only masks patterns in the log message; field names and exception stack traces are not processed
- Structured logging (JSON) masks values but leaves field names visible
- Does not mask email addresses (add pattern if required)

## Alternatives Considered

### Pattern-Based Masking
- **Logstash filters**: Rejected; requires external infrastructure
- **Log4j2 message pattern layout**: Considered but Logback is Spring Boot default
- **Field-level annotations**: Rejected; requires explicit marking on every field

### No Masking
- **Rely on log access controls**: Rejected; defense-in-depth requires masking
- **Don't log PII at all**: Rejected; debugging requires some context
