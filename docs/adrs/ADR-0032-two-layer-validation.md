# ADR-0032: Two-Layer Validation Strategy

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

**Related**: ADR-0001, ADR-0021

## Context
- REST APIs receive untrusted input that must be validated before processing.
- Domain objects have their own validation rules that enforce business invariants.
- We needed to decide: validate once (in DTOs OR domain) or validate twice (in both)?

## Decision
Validate in **two layers**:

### Layer 1: DTO Bean Validation (API boundary)
```java
public record ContactRequest(
    @NotBlank @Size(max = 10) String id,
    @NotBlank @Size(max = 10) String firstName,
    @Pattern(regexp = "\\d{10}") String phone
) {}
```
- Fast feedback to API consumers
- Standard Jakarta Bean Validation annotations
- Returns 400 Bad Request with field-specific errors
- Validates BEFORE any business logic runs

### Layer 2: Domain Constructor Validation (business layer)
```java
public Contact(String id, String firstName, ...) {
    Validation.validateLength(id, "contactId", 1, 10);
    Validation.validateLength(firstName, "firstName", 1, 10);
    // ...
}
```
- Domain objects are ALWAYS valid - can't construct an invalid Contact
- Throws IllegalArgumentException with precise messages
- Acts as safety net if someone bypasses the API (tests, migrations, future code)

## Why Two Layers?

| Single Layer (DTO only)                          | Single Layer (Domain only) | Two Layers (our choice) |
|--------------------------------------------------|----------------------------|-------------------------|
| Domain can be constructed invalid via tests/code | Poor API error messages    | Best of both worlds     |
| No safety net                                    | 500 errors instead of 400s | Defense in depth        |
| Violates "domain objects are always valid"       | Harder to debug            | Redundant but safe      |

## Consequences
- **Positive**: Domain objects are guaranteed valid regardless of entry point
- **Positive**: API consumers get proper 400 errors with field names
- **Positive**: Defense in depth - bugs in one layer caught by the other
- **Negative**: Validation logic appears in two places (mitigated by sharing constants via `Validation.MAX_*`)
- **Negative**: Slightly more code

## How They Stay In Sync
```java
// DTO uses domain constants
public record ContactRequest(
    @Size(max = Validation.MAX_ID_LENGTH) String id,
    // ...
) {}
```
If we change `MAX_ID_LENGTH` from 10 to 15, both layers update automatically.

## Interview Explanation
"We validate at the API layer for good error messages and at the domain layer to guarantee objects are always valid. The DTO catches bad input early with proper 400 errors, but the domain constructor is the real safety net - you literally cannot create an invalid Contact object, whether through the API, tests, or future code paths."
