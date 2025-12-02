# ADR-0005: SpotBugs Runtime Support

**Status**: Accepted | **Date**: 2025-11-20 | **Owners**: Justin Guida


**Related**: [pom.xml](../../pom.xml) (spotbugs-maven-plugin), [README](../../README.md#spotbugs), [CI/CD plan](../ci-cd/ci_cd_plan.md)

## Context
- SpotBugs is required for the course rubric, but the upstream launcher historically lagged when new JDK releases arrived (e.g., JDK 23).
- We needed a repeatable way to run SpotBugs locally and in CI without blocking development when the plugin momentarily breaks on the newest toolchains.
- CI now tests on Temurin 17/21, so SpotBugs must succeed across that matrix or be intentionally skipped with documentation.

## Decision
- Pin `spotbugs-maven-plugin` to 4.9.8.1 and run it during `mvn verify` with `effort=max`, `threshold=Low`, and `failOnError=true`.
- Default `spotbugs.skip` to `false` so analysis always runs, but allow overriding the property when local developers hit known JVM incompatibilities.
- Document in README and the CI/CD plan that SpotBugs currently passes on JDK 17/21; if a future runtime regresses, developers temporarily run it on JDK 17 while the upstream fix lands.
- Capture findings in CI artifacts (HTML + XML) and link them through the QA summary to keep visibility high.

## Consequences
- Builds fail immediately on new SpotBugs findings, which keeps the codebase clean but requires developers to understand and address bug-pattern reports.
- When a new JDK release breaks SpotBugs, developers can temporarily fall back to JDK 17 locally without disabling the CI gate.
- We must periodically bump the plugin once it supports newer bytecode levels to avoid missing genuine issues.

## Alternatives considered
- **Remove SpotBugs from `mvn verify`** - rejected; it’s a required rubric item and catches exposure/concurrency bugs.
- **Skip SpotBugs on newer JDKs indefinitely** - rejected; we want CI parity across the matrix, so we only skip when upstream issues exist and document the workaround.
