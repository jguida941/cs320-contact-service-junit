# Task Entity and Service
Status: Draft
Owner: Justin Guida
Links: requirements/task-requirements/requirements.md, PR #123
Summary: Implements Task (id,name,description) and TaskService with singleton store and atomic updates.

## Implementation Plan Overview

## Definition of Done
- All unit tests pass.
- Line coverage ≥ 90% and mutation score ≥ 85%.
- Checkstyle and SpotBugs report no new issues.
- CodeQL and Dependency Check have no findings or documented risk notes.
- Public types and methods include Javadoc.
- Documentation and changelog updated to reflect Task feature.

## AI Collaboration Loop
- AI drafts designs, implementation steps, and code changes per phase.
- Developer reviews each phase, requests adjustments, and remains final decision-maker before merging.
- Testing and tooling feedback is summarized by AI; developer confirms before sign-off.

## Phase Breakdown

### Phase 1 – Domain & Validation Foundations
- Confirm requirements from `docs/requirements/task-requirements/requirements.md` and align with Contact patterns.
- Implement `Task` class:
  - Fields: `taskId` (immutable, ≤10 chars), `name` (mutable, ≤20), `description` (mutable, ≤50).
  - Constructor validates and trims inputs; delegate mutable fields to setters.
  - Provide getters, `setName`, `setDescription`, and `update(String newName, String newDescription)` for atomic updates.
- Reuse `Validation.validateLength` / `validateNotBlank`; extend Validation only if new shared helpers become necessary.

### Phase 2 – Service Layer Implementation
- Design `TaskService` mirroring `ContactService`:
  - Singleton with lazy-loaded `getInstance`.
  - Backing store `ConcurrentHashMap<String, Task>` for add/update/delete.
  - `addTask(Task)` enforces unique IDs via `putIfAbsent`.
  - `deleteTask(String taskId)` validates input then removes entry.
  - `updateTask(String taskId, String name, String description)` delegates to the Task instance and returns success boolean.
  - Provide `getDatabase()` snapshot and `clearAllTasks()` to aid testing and isolation.
- Ensure all public methods carry Javadoc consistent with Definition of Done.

### Phase 3 – Testing & Quality Gates
- Expand `TaskTest` with:
  - Happy-path construction, trimming behavior, setter updates, bulk update.
  - Negative cases for every constraint (null, blank, over-length) mirroring Contact tests.
- Build `TaskServiceTest` to verify add/delete/update flows, uniqueness enforcement, and exception propagation; use `clearAllTasks()` in lifecycle hooks.
- Run unit tests plus coverage/mutation tooling (e.g., JaCoCo, PIT) to meet thresholds; document results.
- Execute Checkstyle, SpotBugs, CodeQL, and Dependency Check; resolve or document findings.
- Update relevant docs (requirements checklist, README, CHANGELOG) to reflect Task feature completion.
