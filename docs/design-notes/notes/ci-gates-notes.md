# CI gates and quality checks

(Related: [ADR-0004](../../adrs/ADR-0004-ci-gates-and-thresholds.md), [.github/workflows/java-ci.yml](../../.github/workflows/java-ci.yml))

File: docs/design-notes/notes/ci-gates-notes.md

## What runs in CI (`mvn verify`)
- **Surefire** - Maven’s test runner. It discovers every JUnit test class and executes them, so if I forget to run a test the CI will catch it.
- **Checkstyle** - static source-code checker. It enforces imports order, brace placement, no unused imports, etc. Failing Checkstyle means the code violates the style rules.
- **JaCoCo** - coverage agent that instruments bytecode to see which lines/branches ran. After the tests, a threshold check enforces minimum coverage.
- **SpotBugs** - bytecode/static analysis. It scans the compiled `.class` files for bug patterns (null dereferences, equals/hashCode mismatches, unsafe statics, etc.).
- **PITest** - mutation testing. It mutates the compiled classes (e.g., flips `>` to `<`) and re-runs the tests to ensure the suite detects the injected faults.
- **OWASP Dependency-Check** - Software Composition Analysis (SCA). It inspects `pom.xml` dependencies, matches them against the CVE database, and fails if a dependency has a severe CVE (requires `NVD_API_KEY`).
- Separate workflows:
  - **CodeQL** - GitHub’s semantic security scan (SQL injection, path traversal, etc.).
  - **Dependabot** - opens PRs when new dependency versions or security fixes are available.

## Why each gate exists (practical examples)
- **Tests (`mvn test`)** - prove runtime behavior (e.g., `ContactServiceTest.testAddDuplicateContactFails()` verifies duplicates return `false`).
- **Checkstyle** - keeps the codebase consistent (e.g., a missing Javadoc or misordered imports fails the build so style issues are fixed immediately).
- **JaCoCo** - enforces coverage; if new code lacks tests, the coverage percentage drops and fails the threshold.
- **SpotBugs** - finds bytecode-level bugs like null derefs, broken equals/hashCode, unsafe static fields before they reach runtime.
- **PITest** - proves the tests catch faults. If flipping a comparison doesn’t cause a test to fail, PITest marks that mutant as “survived” so we know the suite needs improvement.
- **Dependency-Check** - alerts us to vulnerable libraries by listing CVE IDs (e.g., CVE-2024-12345) so we can upgrade or suppress with justification.
- **CodeQL** - deeper semantic security scan that spots injection or path traversal patterns the other tools might miss.

## Why this matches the assignment (and what I added)
- The rubric required unit tests. I added static analysis, and a full CI discipline with coverage, mutation, dependency scanning, and CodeQL.
- Running all gates on every push/PR makes regressions obvious immediately instead of discovering them manually.
