# ADR-0030: Pattern-First Development with AI Assistance

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

## Context
- Building three similar entities (Contact, Task, Appointment) with consistent patterns.
- AI tools can accelerate development but risk inconsistency if used without structure.
- Needed a way to learn fundamentals while also being productive.

## Decision
- Build the first implementation (Contact, ContactService, ContactTest) manually to understand the patterns.
- Establish conventions: validation approach, test structure, error messages, service methods.
- Use AI to mirror these patterns for subsequent entities (Task, Appointment).
- All AI-generated code must pass the same CI quality gates as manual code.

## Consequences
- Deep understanding of the first entity; can explain Contact/ContactService in detail.
- Consistent patterns across all three domains because AI followed established conventions.
- Faster development for entities 2 and 3 without sacrificing quality.
- CI gates catch any AI mistakes or deviations from the pattern.

## Lessons Learned
- AI is most effective when given a clear pattern to follow, not when starting from scratch.
- The "build one, mirror many" approach scales well for repetitive structures.
- Understanding the first implementation deeply is essential - can't debug what you don't understand.
