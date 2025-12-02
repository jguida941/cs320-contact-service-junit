# ADR-0044: Optimistic Locking for Concurrent Updates

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

## Context

The CODEBASE_AUDIT.md identified a race condition vulnerability (BACKEND-LOGIC-04): the "update resurrection bug" where concurrent operations could cause deleted records to reappear.

**Scenario:**
1. Request A reads Contact with ID "123"
2. Request B deletes Contact "123"
3. Request A saves its update to Contact "123"
4. Result: Contact "123" is resurrected (re-inserted)

This happens because the JPA save() method performs an upsert - if the record doesn't exist, it inserts.

## Decision

Add JPA `@Version` fields to all entity classes for optimistic locking.

### Changes

1. **Entity classes** (ContactEntity, TaskEntity, AppointmentEntity):
   - Added `@Version` annotated `Long version` field
   - Added getter for version field

2. **Database migration** (V7__add_version_columns.sql):
   - Added `version BIGINT DEFAULT 0 NOT NULL` to contacts, tasks, appointments

### How It Works

With `@Version`:
1. JPA automatically increments version on each update
2. UPDATE includes `WHERE version = ?` in the SQL
3. If version mismatch (record was modified/deleted), `OptimisticLockException` is thrown
4. No silent data resurrection - concurrent modification is detected

## Consequences

### Positive

- **Race Condition Prevention**: Concurrent updates are detected and rejected
- **No Resurrection Bug**: Deleted records stay deleted
- **JPA-Level Enforcement**: Works automatically at the persistence layer, while services translate `OptimisticLockException` into retries or user-facing 409 responses
- **Standard Pattern**: Well-understood optimistic locking pattern

### Negative

- **Version Mismatch Errors**: Legitimate concurrent updates will fail (need retry logic)
- **Schema Change**: Requires database migration

### Neutral

- **Performance**: Minimal overhead (one extra column in WHERE clause)
- **No Breaking Changes**: Existing APIs unchanged

## Migration Notes

- Existing records get `version = 0` via migration default
- First update increments to `version = 1`
- Service layers must catch `OptimisticLockException` (or the Spring `ObjectOptimisticLockingFailureException`) and either retry with backoff or surface a clear "someone else updated this record" message to clients

## References

- JPA `@Version` specification: https://jakarta.ee/specifications/persistence/
- CODEBASE_AUDIT.md: BACKEND-LOGIC-04
