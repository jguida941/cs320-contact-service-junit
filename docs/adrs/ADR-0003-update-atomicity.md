# ADR-0003: Update Atomicity Strategy

Status: Accepted
Date: 2025-11-19
Owners: Justin Guida
Related: src/main/java/contactapp/Contact.java, src/main/java/contactapp/ContactService.java, docs/logs/CHANGELOG.md

## Context
- Requirements demand that updates never leave partially modified records; either every field change applies or none do.
- Early setter-based updates mutated fields incrementally, so a failure (e.g., invalid phone) could leave first/last name changed while other fields remained old values.
- Mutation tests and SpotBugs both highlighted the risk of inconsistent state when exceptions interrupt a chain of assignments.

## Decision
- Introduce `Contact#update(String newFirstName, String newLastName, String newPhone, String newAddress)` that validates every argument first, then assigns all fields only after validation succeeds.
- Modify `ContactService#updateContact(...)` to delegate to the new `Contact#update` helper rather than calling setters sequentially.
- Keep setters for individual field adjustments but guide all multi-field updates (including service-level updates) through the atomic helper.

## Consequences
- Callers now get all-or-nothing semantics; if validation fails, no state changes occur.
- The service implementation became simpler and more testable because it no longer repeats validation logic.
- Mutation testing can verify that skipping any of the field assignments or validations causes failures, increasing coverage quality.
- Developers must remember to use the `update` helper for multi-field operations; direct setter chains reintroduce the risk.

## Alternatives considered
- **Sequential setter calls with try/catch rollback** – rejected as overly complex and error-prone; keeping snapshot copies just to roll back was unnecessary.
- **Rebuilding objects (`new Contact(...)`) on every update** – rejected because IDs are immutable and replacing references would complicate the map and tests.
