# ADR-0011: TaskService Singleton and CRUD Tests

> **Note**: This ADR describes the legacy in-memory implementation. See [ADR-0024](ADR-0024-persistence-implementation.md) for the current JPA persistence architecture.

**Status**: Accepted | **Date**: 2025-11-24 | **Owners**: Justin Guida

**Related**: [TaskService.java](../../src/main/java/contactapp/service/TaskService.java),
[TaskServiceTest.java](../../src/test/java/contactapp/service/TaskServiceTest.java),
[Task.java](../../src/main/java/contactapp/domain/Task.java),
[Task requirements](../requirements/task-requirements/requirements.md)

## Context
- Task operations (add, delete, update) must enforce unique ids and reuse Task
  validation; concurrency and cross-test state have caused issues in other
  services.
- The service needs a predictable lifecycle: one shared instance for the
  process and a way for tests to reset state between runs.
- API contracts return booleans rather than throwing when entries are missing,
  so callers and tests need those semantics documented.

## Decision
- Expose a process-wide singleton via `TaskService.getInstance()` with a
  synchronized accessor to avoid double creation under concurrent access.
- Store tasks in a `ConcurrentHashMap<String, Task>` keyed by trimmed ids to
  keep lookups deterministic; return an unmodifiable snapshot of defensive copies
  (via `Task.copy()`) from `getDatabase()` to prevent external mutation.
- `addTask` rejects null inputs (task and id) and refuses to overwrite existing ids; all mutators
  validate/trim ids so stored keys are normalized before map access.
- `deleteTask` and `updateTask` validate/trim ids, returning `false` instead of
  throwing when an entry is missing.
- Provide `clearAllTasks()` so tests can isolate state without recreating the
  JVM.
- Validate the behavior in `TaskServiceTest`:
  - Singleton identity, happy-path add/delete/update, and trimming of ids.
  - Duplicate add returns `false`; missing delete/update return `false` without
    mutation.
  - Blank ids throw; `clearAllTasks` actually empties the backing store.

## Consequences
- A singleton with a concurrent map yields thread-safe shared state but requires
  explicit resets in tests to avoid cross-test leakage.
- **Spring Boot Test Considerations (Added 2025-12-02)**: When using `@SpringBootTest`, the singleton pattern interacts with Spring's context caching. Static `instance` fields persist across test executions even when the database is cleared. Solution: `TestCleanupUtility.resetTestEnvironment()` resets singletons via reflection before clearing data, preventing `registerInstance()` from migrating stale data. See ADR-0047.
- Boolean return contracts simplify caller logic for missing entries; tests
  enforce duplicate/missing/trimmed-id branches to avoid accidental overwrites.
- Returning copies from `getDatabase()` protects internal state, at the cost of
  a small allocation per call.

## Alternatives considered
- **Instantiate TaskService per caller** - rejected; it would fragment state and
  complicate synchronization across the app.
- **Use a plain `HashMap` with synchronized methods** - workable but less
  scalable; `ConcurrentHashMap` allows concurrent reads/writes without coarse
  locks.
- **Throw exceptions on missing ids** - rejected in favor of simple boolean
  contracts that align with the existing service tests.
