# Task Service Requirements

## Task Class
- Each task shall include a **unique task ID**.
  - Type: `String`
  - Constraints: required, immutable after construction, length ≤ 10 characters, non-null.
- The task shall include a **name**.
  - Type: `String`
  - Constraints: required, mutable, length ≤ 20 characters, non-null.
- The task shall include a **description**.
  - Type: `String`
  - Constraints: required, mutable, length ≤ 50 characters, non-null.

## Task Service
- The service shall provide an operation to **add tasks**.
  - A task may only be added when its task ID is unique within the service.
- The service shall provide an operation to **delete tasks by task ID**.
- The service shall provide an operation to **update tasks by task ID**.
  - Updatable fields: task name and task description (subject to their respective constraints above).
