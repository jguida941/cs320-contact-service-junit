# ADR-0008: CI Metrics Summary Script

Status: Accepted
Date: 2025-11-19
Owners: Justin Guida
Related: scripts/ci_metrics_summary.py, README.md#qa-summary, .github/workflows/java-ci.yml

## Context
- Our CI pipeline runs numerous quality gates (Surefire, JaCoCo, PITest, Dependency-Check) and emits multiple HTML/JSON/XML reports.
- Reviewers wanted a single view in GitHub Actions that highlights pass/fail state, coverage, mutation score, and dependency risk without digging through artifacts.
- We also wanted those metrics bundled into the downloadable QA dashboard artifact used for progress demos.

## Decision
- Maintain a dedicated helper (`scripts/ci_metrics_summary.py`) that:
  - Loads Maven outputs (Surefire XML, JaCoCo XML, `mutations.xml`, Dependency-Check JSON) and normalizes them into a consistent metrics object.
  - Writes a Markdown summary to `GITHUB_STEP_SUMMARY`, including coverage/mutation bar charts and dependency severity breakdowns.
  - Generates `target/site/qa-dashboard/metrics.json` (plus copies the React dashboard build when available) so artifacts contain a self-contained QA report.
  - Operates defensively: missing files or parse errors result in `_no data_` labels rather than build failures.
- Require every CI matrix leg to run the script after `mvn verify` so the QA summary is always present regardless of platform/JDK.

## Consequences
- Reviewers get instant visibility into test counts, line coverage, mutation score, and dependency findings for each runner without downloading artifacts.
- The same metrics feed the React QA dashboard, keeping local and hosted reporting in sync.
- Script now becomes part of the project’s contract; future tooling changes should extend the helper instead of bypassing it.
- Minor maintenance overhead exists when adding new gates (e.g., CodeQL results would need another loader), but the structure makes that straightforward.

## Alternatives considered
- **Rely on individual tool outputs** – rejected because the CI summary would require opening multiple artifacts per matrix leg.
- **Use third-party reporting services** – rejected due to external dependency overhead and the course requirement to keep everything self-contained.
- **Let Maven plugins emit the summary** – rejected; plugin output is verbose and not easily aggregated into a concise GitHub Actions summary.
