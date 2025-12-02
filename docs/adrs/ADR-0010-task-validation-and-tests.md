# ADR-0010: Task Validation and Test Coverage

**Status**: Accepted | **Date**: 2025-11-20 | **Owners**: Justin Guida


**Related**: [Task.java](../../src/main/java/contactapp/domain/Task.java),
[TaskTest.java](../../src/test/java/contactapp/domain/TaskTest.java),
[Validation.java](../../src/main/java/contactapp/domain/Validation.java),
[Task requirements](../requirements/task-requirements/requirements.md)

## Context
- Task requirements impose max lengths and non-null constraints on id, name, and
  description; earlier contact work showed how easy it is for constructor and
  setter validation to drift.
- Trimming and exception messages need to stay consistent because tests assert
  exact strings and we rely on normalization for map keys.
- Update behavior must be atomic so partially applied changes do not leak when
  validation fails.

## Decision
- Use the shared `Validation.validateLength` helper for all Task fields,
  trimming values before storing them to keep normalization consistent.
- Keep `taskId` immutable after construction; expose setters only for `name` and
  `description` and reuse the same validation pipeline in setters and
  `update(...)`.
- Implement `Task#update` to validate both inputs first, then assign both fields
  together, guaranteeing atomicity.
- Cover the contract with `TaskTest`:
  - `testSuccessfulCreationTrimsValues` proves normalization.
  - CSV- and MethodSource-driven parameterized tests exercise constructor,
    setter, and update validation paths with exact error messages.
  - `testUpdateRejectsInvalidValuesAtomically` asserts state remains unchanged
    after a failed update.

## Consequences
- Centralized validation plus trimming avoids divergence between constructors,
  setters, and updates; callers get predictable error strings for assertions and
  user feedback.
- Atomic updates prevent partially mutated Tasks, simplifying debugging and
  keeping service-level behavior deterministic.
- Parameterized tests make it easy to add edge cases, but any change to message
  text or limits requires updating multiple expectations.

## Alternatives considered
- **Inline validation per method** - rejected because it risks message drift and
  inconsistent trimming.
- **Allow partial updates** - rejected; it would complicate rollback logic and
  violate the atomicity requirement exercised in tests.
