# Task entity and service notes

Related: [Task.java](../../../src/main/java/contactapp/Task.java), [TaskService.java](../../../src/main/java/contactapp/TaskService.java), [ADR-0010](../../adrs/ADR-0010-task-validation-and-tests.md), [ADR-0011](../../adrs/ADR-0011-taskservice-singleton-and-crud-tests.md)

## Why Task exists
- Mirrors the Contact model for a simpler domain (id/name/description) and reuses the shared `Validation` helpers.
- Keeps ID immutable so map keys stay stable; name/description remain mutable but validated.

## Fields and constraints
- `taskId`: required, trimmed, length 1–10, immutable.
- `name`: required, trimmed, length 1–20, mutable.
- `description`: required, trimmed, length 1–50, mutable.
- All limits are defined as constants to avoid magic numbers.

## Validation and atomicity
- Constructor and setters call `Validation.validateLength` so messages and trimming are consistent.
- `update(newName, newDescription)` validates both first, then assigns both together; invalid input leaves state unchanged.

## Service summary
- `TaskService` is a lazy singleton backed by `ConcurrentHashMap<String, Task>`.
- `addTask` rejects null and uses `putIfAbsent` for uniqueness.
- `deleteTask`/`updateTask` trim/validate IDs; updates delegate to `Task.update(...)`.
- `getDatabase()` returns `Map.copyOf(...)`; `clearAllTasks()` resets state for tests.

## Tests hit
- `TaskTest`: constructor trimming, setter/update happy paths, invalid constructor/setter/update cases (null/blank/over-length) and atomic update rejection.
- `TaskServiceTest`: singleton, add/duplicate/null add, delete success/blank/missing, update success/blank/missing/trimmed IDs, clear-all.
