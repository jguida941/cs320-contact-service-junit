# Appointment Service Requirements

## Appointment Class
- Each appointment shall include a **unique appointment ID**.
  - Type: `String`
  - Constraints: required, immutable after construction, length ≤ 10 characters, non-null.
- Each appointment shall include an **appointment date**.
  - Type: `java.util.Date`
  - Constraints: required, must not be null, and must not be in the past (date must be >= current time).
- Each appointment shall include a **description**.
  - Type: `String`
  - Constraints: required, mutable, length ≤ 50 characters, non-null.

## Appointment Service
- The service shall provide an operation to **add appointments**.
  - An appointment may only be added when its appointment ID is unique within the service.
- The service shall provide an operation to **delete appointments by appointment ID**.
  - Behavior: operation is idempotent; return a clear “not found” indication when the ID does not exist (e.g., boolean `false` or HTTP 404), and succeed when the ID exists.
- The service shall provide an operation to **update an appointment’s mutable fields** (description and date).
  - Inputs: `appointmentId` (trimmed, required, ≤ 10 chars), `description` (required, ≤ 50 chars), `appointmentDate` (required, not in the past).
  - Behavior: return success when the appointment exists and both inputs pass validation; return a clear validation error when inputs are null/blank/over-length or the date is in the past; return not-found when the appointment ID does not exist.
  - The operation must not partially update state: if validation fails, the prior description and date remain unchanged.
