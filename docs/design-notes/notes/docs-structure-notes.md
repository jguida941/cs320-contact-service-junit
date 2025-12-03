# Docs structure

(Related: [ADR-0006](../../adrs/ADR-0006-docs-structure.md))

File: docs/design-notes/notes/docs-structure-notes.md

## Layout overview
- `docs/adrs/` - Architecture Decision Records (one ADR per major decision).
- `docs/architecture/` - feature design briefs/plans (e.g., the Task plan).
- `docs/CI-CD/` - CI/CD notes and pipeline plans.
- `docs/logs/` - changelog/backlog history.
- `docs/requirements/` - assignment/spec documents and checklists.
- `docs/design-notes/` - personal explanations (non-graded notes, like this file).
- Root `README.md`, `agents.md`, and `docs/INDEX.md` point to the key folders above.

## Why this helps
- Decisions, requirements, and design docs live in predictable locations, so reviewers don’t have to hunt through the repo.
- Your personal notes stay separate from the formal deliverables, keeping the repo tidy while still capturing lessons learned.
