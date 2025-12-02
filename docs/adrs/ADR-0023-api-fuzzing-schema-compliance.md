# ADR-0023: API Fuzzing Findings and Schema Compliance

**Status:** Accepted-Phase 2.5 | **Date:** 2025-12-01 | **Owners:** Justin Guida

## Context

Schemathesis API fuzzing discovered several mismatches between the OpenAPI contract
and the actual controller behavior. We need a durable record of the findings and the
mitigations so the CI fuzzing workflow remains actionable.

## Findings and Fixes

### 1. Jackson Type Coercion

**Issue:** Jackson coerced non-string JSON values into strings. Examples such as
`{"address": false}` and `{"description": 123}` were silently accepted, which violated
the OpenAPI schema.

**Fix:** Added `JacksonConfig` that disables coercion for textual types, causing Spring
MVC to return 400 responses when inputs do not match the schema.

### 2. Undocumented HTTP 400 Responses

**Issue:** When path variable validation failed, the API returned HTTP 400 but the OpenAPI
document only listed 200/404 (GET) or 204/404 (DELETE).

**Fix:** Added explicit `@ApiResponse(responseCode = "400", description = "Invalid ID format")`
to every GET/DELETE `{id}` endpoint so the rendered spec matches runtime behavior.

### 3. Appointment Date Format

**Issue:** The API expects timestamps with millisecond precision
(`yyyy-MM-dd'T'HH:mm:ss.SSSXXX`) while some clients omit milliseconds.

**Resolution:** Keep the strict format to avoid ambiguity. The Swagger description now calls
out the requirement and tests enforce it.

### 4. Path Variables with Control Characters

**Issue:** IDs containing control characters (newlines, null bytes) fail Bean Validation.

**Resolution:** Intentional security behavior; documented the restriction via
`@Schema(pattern = "\\S+")`.

### 5. Domain Trimming Behavior

**Issue:** Schemathesis noted that strings are trimmed inside the domain layer even though
Bean Validation runs on the raw payload.

**Resolution:** Documented this “lenient input” design in the DTO Javadoc so consumers know
what to expect.

## Decision

1. **Fail fast on schema violations** by disabling type coercion in Jackson.
2. **Document 400 responses** for every controller method that can emit them.
3. **Keep strict date formats** for appointments and document the behavior.
4. **Reject control characters** in IDs and describe the rule in OpenAPI.
5. **Document trimming behavior** so API consumers understand the two-stage validation.

## Consequences

- OpenAPI, Swagger UI, and runtime behavior now align, which keeps Schemathesis reports clean.
- `mvn verify` runs Schemathesis in CI (Phase 2.5) and all checks now pass (30,668 cases).
- DTO documentation now explicitly states that Bean Validation runs before domain trimming.
- Future regressions will fail the CI fuzzing job, giving early feedback.

## Related ADRs

- ADR-0021 – REST API Implementation
- ADR-0022 – Custom Error Controller
