# CI metrics summary script

(Related: [ADR-0008](../../adrs/ADR-0008-ci-metrics-summary-script.md), [scripts/ci_metrics_summary.py](../../scripts/ci_metrics_summary.py))

File: docs/design-notes/notes/ci-metrics-script-notes.md

## What problem this solves
- Raw CI logs are long and hard to scan. Reviewers have to grep for tests, coverage, mutation score, and vulnerability counts in different files.
- We needed a single, friendly summary per matrix job so instructors and teammates can see quality gates at a glance.

## What the design is
- A Python script (`scripts/ci_metrics_summary.py`) runs after `mvn verify` in the CI workflow.
- It parses:
  - Surefire XML (tests run/passed/failed).
  - JaCoCo XML (line coverage).
  - PITest mutation reports (mutations killed/survived).
  - Dependency-Check JSON (vulnerable dependencies + severity counts).
- The script writes:
  - A Markdown table to the GitHub Actions job summary (`GITHUB_STEP_SUMMARY`).
  - A JSON file (`target/site/qa-dashboard/metrics.json`) that powers the downloadable dashboard.
- If a report is missing (e.g., Dependency-Check skipped), the script notes `_no data_` instead of failing, so CI stays readable even when a gate is temporarily disabled.

## Why this is useful
- Every workflow run gets a concise table with tests, coverage, mutation score, and dependency counts without digging through logs.
- The same data drives the React dashboard (`target/site/qa-dashboard/index.html`), so anyone downloading the artifact can see the full QA snapshot.
- Reviewers see drift immediately (e.g., mutation score dropping) and can act before merging.
