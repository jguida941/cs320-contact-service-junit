# ADR-0002: Contact Service Storage Model

**Status**: Accepted  
**Date**: 2025-11-19  
**Owners**: Justin Guida  

**Related**: [ContactService.java](../../src/main/java/contactapp/service/ContactService.java), [CHANGELOG.md](../logs/CHANGELOG.md)

## Context
- The application needs an in-memory store for contacts that enforces unique IDs and supports add/update/delete operations.
- Early prototypes instantiated new service objects per test, which made it easy to lose state between operations and complicated singleton expectations from the assignment.
- We also needed a structure that would behave under concurrent access because later milestones discuss multi-threaded callers and CI runs SpotBugs for concurrency hazards.

## Decision
- Implement `ContactService` as a lazily constructed singleton exposed through `getInstance()` and Spring DI, guaranteeing a single shared store for the application.
- Back the store with a static `ConcurrentHashMap<String, Contact>` so all access paths share the same data and put/get/remove operations are thread-safe and O(1) on average.
- Use `database.putIfAbsent` to enforce uniqueness atomically, `database.remove` for deletes, and direct lookups for updates.
- Expose an unmodifiable snapshot of defensive copies (via `Contact.copy()`) in `getDatabase()` so tests can observe state without mutating internal objects, and provide `clearAllContacts()` for cleanup.
- Normalize identifiers before any map operation so callers can pass whitespace-only variants (e.g., `" 123 "`) and still target the stored `"123"` entry; blank ids throw `IllegalArgumentException` consistently across add/delete/update.

## Consequences
- The singleton API matches assignment expectations and keeps service state consistent across UI and tests.
- `ConcurrentHashMap` avoids synchronization bottlenecks while providing safe concurrent access, and SpotBugs no longer reports exposure warnings.
- Returning a snapshot prevents external code from mutating the internal map accidentally, but it requires tests to use `clearAllContacts()` rather than clearing the snapshot itself.
- Any future persistent storage layer will need to preserve the singleton contract or add migration logic.

## Alternatives considered
- **Per-request service instances with plain `HashMap`** - rejected because uniqueness checks would race under concurrency and tests would observe isolated maps.
- **Synchronized wrappers (`Collections.synchronizedMap`)** - workable but heavier than required; `ConcurrentHashMap` provides finer-grained concurrency.
- **Exposing the raw map** - rejected because callers could corrupt state, and SpotBugs flagged `MS_EXPOSE_REP`.
