# ADR-0012: Appointment Validation and Test Coverage

**Status**: Accepted | **Date**: 2025-11-24 | **Owners**: Justin Guida

**Related**: [Appointment.java](../../src/main/java/contactapp/domain/Appointment.java),
[AppointmentTest.java](../../src/test/java/contactapp/domain/AppointmentTest.java),
[Validation.java](../../src/main/java/contactapp/domain/Validation.java),
[Appointment requirements](../cs320-requirements/appointment-requirements/requirements.md)

## Context
- Appointment requirements demand an immutable ID (≤10 chars), a description (≤50 chars),
  and a `java.util.Date` that is required and cannot be in the past.
- Date validation differs from the string-only validators; without a dedicated guard,
  null/past dates could slip through constructors or updates.
- Tests need to assert both the normalization (trimming) and defensive copying of dates
  so external callers cannot mutate internal state.

## Decision
- Use `Validation.validateLength` for `appointmentId` and `description`; trim strings before storage and validate the trimmed value after a not-blank check.
- Add `Validation.validateDateNotPast(Date, String)` to enforce non-null and not-before-now semantics.
- Store and return dates via defensive copies (`new Date(date.getTime())`) to prevent external mutation.
- Keep `appointmentId` immutable after construction; allow description and date to change via
  validated setters/update, reusing the same helpers to avoid drift. `Appointment.update(...)`
  validates both inputs before assigning to preserve atomicity (no partial updates).
- Cover behavior in `AppointmentTest`:
  - Successful creation with trimmed ID/description and a future date (defensive copy).
  - Future/past/null date branches and description length enforcement.
  - Invalid updates leave state unchanged; date helpers are relative (no stale hard-coded years).

## Consequences
- Centralized date validation keeps constructor/setter/update paths aligned and messages consistent.
- Defensive copying ensures callers cannot mutate stored dates, but incurs a small allocation per set/get.
- Tests must create future dates (Calendar/epoch) to avoid flakiness.

## Alternatives considered
- **Keep date as String** - rejected; would complicate past-date checks and invite parsing bugs.
- **Inline date checks per method** - rejected to avoid drift between constructor and update paths.
