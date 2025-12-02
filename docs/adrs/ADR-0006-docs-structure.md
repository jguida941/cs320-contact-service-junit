# ADR-0006: Documentation Structure

**Status**: Accepted | **Date**: 2025-11-20 | **Owners**: Justin Guida


**Related**: [README](../../README.md), [docs/requirements/](../requirements/), [docs/architecture/](../architecture/), [docs/adrs/](../adrs/), [CHANGELOG.md](../logs/CHANGELOG.md)

## Context
- Documentation started as a monolithic README, which quickly became unwieldy once requirements, CI notes, and design plans accumulated.
- Reviewers asked for dedicated design docs and ADRs so they could trace decisions without reading the entire README.
- Future milestones (e.g., Task feature) require a predictable place to publish plans and reference them from ADRs/PRs.

## Decision
- Adopt a structured `docs/` tree:
  - `docs/requirements/` for instructor-provided specs and checklists.
  - `docs/architecture/` for feature-specific design plans (e.g., `2025-11-19-task-entity-and-service.md`).
  - `docs/adrs/` for Architecture Decision Records, numbered sequentially with an index.
  - `docs/logs/` for changelog/backlog/history.
- Keep the README focused on onboarding, design highlights, and tooling instructions; cross-link to the deeper docs when needed.
- Require new features to land both an architecture brief and an ADR referencing it before implementation proceeds.

## Consequences
- Contributors can find requirements, design discussions, and decisions quickly, improving review efficiency.
- ADR numbering provides a canonical history, helping us avoid re-litigating settled questions.
- There is a small documentation overhead for each significant change, but it keeps the project audit-ready.

## Alternatives considered
- **Single README for everything** - rejected; navigation became painful and reviewers struggled to isolate decisions.
- **External wiki** - rejected because the course repository needs to be self-contained and accessible offline.
