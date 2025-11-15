# CS320 Milestone 1 – Contact Service
[![Java CI](https://github.com/jguida941/cs320-contact-service-junit/actions/workflows/java-ci.yml/badge.svg)](https://github.com/jguida941/cs320-contact-service-junit/actions/workflows/java-ci.yml)
[![CodeQL](https://github.com/jguida941/cs320-contact-service-junit/actions/workflows/codeql.yml/badge.svg)](https://github.com/jguida941/cs320-contact-service-junit/actions/workflows/codeql.yml)

Small Java project for the CS320 Contact Service milestone. The work breaks down into two pieces:
1. Build the `Contact` and `ContactService` classes exactly as described in the requirements.
2. Prove every rule with unit tests (length limits, null checks, unique IDs, and add/update/delete behavior) using the shared `Validation` helper so exceptions surface clear messages.

Everything is packaged under `contactapp`; production classes live in `src/main/java` and the JUnit tests in `src/test/java`.

## Getting Started
1. Install Java 17 and Apache Maven (3.9+).
2. Run `mvn verify` from the project root to compile everything, execute the JUnit suite, and run Checkstyle/SpotBugs/JaCoCo quality gates.
3. Open the folder in IntelliJ/VS Code if you want IDE assistance—the Maven project model is auto-detected.

## Folder Highlights
| Path                       | Description                                                |
|----------------------------|------------------------------------------------------------|
| `src/main/java/contactapp` | `Contact`, `ContactService`, and an optional `Main` class. |
| `src/test/java/contactapp` | Unit tests for the milestone.                              |
| `requirements/`            | Assignment write-up and checklist from the instructor.     |
| `docs/index.md`            | Quick reference guide for the repo layout.                 |
| `pom.xml`                  | Maven build file (dependencies, plugins, compiler config). |
| `config/checkstyle/...`    | Checkstyle configuration used by Maven/CI quality gates.   |

## Notes
- Keep the `package contactapp;` declaration at the top of every Java file so the folder layout and compiler stay in sync.
- `Validation.java` centralizes the not-blank, length, and 10-digit checks; the unit tests assert on those messages via AssertJ.
- Every requirement from `requirements/requirements.md` has at least one test case to demonstrate full coverage.
- GitHub Actions (`.github/workflows/java-ci.yml`) now runs `mvn -B verify` (tests in parallel + Checkstyle + JaCoCo + OWASP Dependency-Check + PITest mutation analysis) on every push/PR to `main`/`master` across Java 17/21 on both Ubuntu and Windows runners, with caching for faster builds and artifacts for all reports; an additional containerized job reruns the suite inside the official Maven image. Published releases automatically build a JAR, auto-generate release notes, and upload the artifact. Dependabot and CodeQL workflows keep dependencies up to date and scan for security issues (CodeQL alerts surface once the repo has code scanning enabled or is public). Dependency-Check reads the `NVD_API_KEY` environment variable (add it locally via `export NVD_API_KEY=...` and in GitHub Secrets) and caches its data; if the key is missing or the scan fails, the workflow re-runs with the check skipped so builds stay green.
- PITest runs with conservative timeouts/threads; hosted runners sometimes block the mutation minion, so CI includes a dedicated `mutation-test` job that targets a self-hosted runner (register one under GitHub Settings → Actions → Runners). Hosted jobs automatically retry with `-Dpit.skip=true` if they encounter environment-specific failures. If you need to skip mutation testing locally, run `mvn verify -Dpit.skip=true`.
- To run the mutation lane yourself: on the machine you want to use, follow GitHub’s self-hosted runner setup (`Settings → Actions → Runners → New self-hosted runner`), then:
  ```bash
  mkdir ~/actions-runner && cd ~/actions-runner
  curl -o actions-runner-osx-x64.tar.gz -L https://github.com/actions/runner/releases/latest/download/actions-runner-osx-x64.tar.gz
  tar xzf actions-runner-osx-x64.tar.gz
  ./config.sh --url https://github.com/jguida941/cs320-contact-service-junit --token <token from GitHub>
  echo 'export MAVEN_OPTS="--enable-native-access=ALL-UNNAMED -Djdk.attach.allowAttachSelf=true"' >> ~/.zshrc && source ~/.zshrc
  ./run.sh
  ```
  Leave `./run.sh` running so the `mutation-test` job can attach to your runner. When you’re done, Ctrl+C stops the runner.
- `docs/index.md` shows all the key files and folders for easy navigation.
