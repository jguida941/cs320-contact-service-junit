# Task Service Requirements Checklist

_Status updated: 2025-11-20_

## Task Class Requirements

- [x] Task ID is required, non-null, max length 10 characters, and immutable.
- [x] Task name is required, non-null, max length 20 characters, and updatable.
- [x] Task description is required, non-null, max length 50 characters, and updatable.

## Task Service Requirements
- [x] Service can add a task only when the task ID is unique.
- [x] Service can delete an existing task by task ID.
- [x] Service can update an existing task’s name (respecting constraints).
- [x] Service can update an existing task’s description (respecting constraints).
