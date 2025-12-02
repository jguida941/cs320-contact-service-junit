# ADR-0031: Mutation Testing to Validate Test Strength

**Status:** Accepted | **Date:** 2025-12-01 | **Owners:** Justin Guida

## Context
- Code coverage (JaCoCo) shows what lines execute, not whether tests actually catch bugs.
- A test that runs code but never asserts anything still counts as "covered."
- Needed confidence that tests would catch real bugs, not just execute code paths.

## Decision
- Add PITest (mutation testing) to the CI pipeline.
- Mutation testing works by deliberately breaking the code (mutations) and checking if tests fail.
- If a test doesn't fail when code is broken, the test is weak.
- Target: 95%+ mutation kill rate (achieved 99%).

## Example
```java
// Original code
if (name.length() > 10) throw new IllegalArgumentException();

// PITest mutates to:
if (name.length() >= 10) throw new IllegalArgumentException();  // boundary change

// If no test fails, we're missing a boundary test case
```

## Consequences
- Tests are proven to catch bugs, not just run code.
- Higher confidence in refactoring - if tests pass after mutation, they're strong.
- Slower CI (mutation testing is computationally expensive) but worth the confidence.
- Forces writing assertions, not just "happy path" tests.

## Why This Matters
Line coverage can be misleading. A project can have 100% coverage but weak tests. Mutation testing proves the tests actually work. This is especially important when using AI to generate tests - mutation testing validates the AI didn't just write tests that execute code without meaningful assertions.
