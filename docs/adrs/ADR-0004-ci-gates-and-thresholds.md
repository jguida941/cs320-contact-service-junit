# ADR-0004: CI Gates and Quality Thresholds

**Status**: Accepted  
**Date**: 2025-11-19  
**Owners**: Justin Guida  

**Related**: [pom.xml](../../pom.xml), [java-ci workflow](../../.github/workflows/java-ci.yml), [CI/CD plan](../ci-cd/ci_cd_plan.md), [README](../../README.md#static-analysis--quality-gates)

## Context
- The project must prove correctness with more than unit tests; instructors expect concrete coverage, mutation, static analysis, and security scanning metrics.
- Running these tools ad hoc caused regressions (e.g., JaCoCo dropping below 80% or Dependency-Check skipping when secrets were misconfigured).
- CI needed deterministic failure criteria so every pull request sees the same gates locally (`mvn verify`) and remotely (GitHub Actions).

## Decision
- Wire the Maven `verify` phase to run Checkstyle, SpotBugs (`spotbugs-maven-plugin` 4.9.7.0), JaCoCo (min instruction coverage 80%), PITest (mutation threshold 70%), and OWASP Dependency-Check (fail build on CVSS ≥ 7, cache under `.owasp-cache`), with `nvdApiDelay` and `dependency.check.skip` exposed for CI to tune or temporarily skip the feed without code changes.
- Document that Dependency-Check defaults to 3500ms `nvdApiDelay` when an NVD API key is configured and 8000ms without one, so throttling expectations are clear in both README and workflow flags.
- Add CodeQL and Dependabot workflows separate from the main Java CI job but required for merges through branch protection.
- Publish coverage/mutation/dependency metrics through `scripts/ci_metrics_summary.py` so each matrix leg reports a consistent KPI table.
- Require developers to run `mvn verify` locally before pushing so CI surprises are minimized.

## Consequences
- CI runs longer but now guarantees a baseline quality bar; regressions in coverage or mutation scores fail fast.
- Every workflow artifact (JaCoCo HTML, SpotBugs HTML, Dependency-Check report, PITest HTML) is bundled for download, simplifying triage.
- Developers occasionally need to update suppression files or adjust timeouts (e.g., `nvdApiDelay` for Dependency-Check) to keep pipelines stable, but the `dependency.check.skip` flag gives CI a controlled retry path instead of stalling on rate limits.
- Raising thresholds later (e.g., JaCoCo > 90%, PITest > 85%) will be straightforward because the infrastructure already exists.

## Alternatives considered
- **Only run tests in CI** - rejected because silent coverage/mutation regressions would go unnoticed until grading.
- **Gate solely on Codecov/PITest hosted services** - rejected since the course environment limits external integrations and we already have Maven plugins.
- **Run Dependency-Check manually** - rejected; embedding it keeps the SBOM and report history consistent.
