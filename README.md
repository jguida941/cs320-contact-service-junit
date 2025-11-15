# CS320 Milestone 1 – Contact Service

Small Java project for the CS320 Contact Service milestone. The work breaks down into two pieces:
1. Build the `Contact` and `ContactService` classes exactly as described in the requirements.
2. Prove every rule with unit tests (length limits, null checks, unique IDs, and add/update/delete behavior).

Everything is packaged under `contactapp`; production classes live in `src/main/java` and the JUnit tests in `src/test/java`.

## Getting Started
1. Open the folder in IntelliJ (or another Java IDE) and make sure it targets Java 17.
2. Add JUnit 5 to your test classpath if it isn’t already provided by the course template.
3. Run the tests in `src/test/java/contactapp` to verify your implementation.

## Folder Highlights
| Path                       | Description                                                |
|----------------------------|------------------------------------------------------------|
| `src/main/java/contactapp` | `Contact`, `ContactService`, and an optional `Main` class. |
| `src/test/java/contactapp` | Unit tests for the milestone.                              |
| `requirements/`            | Assignment write-up and checklist from the instructor.     |
| `docs/index.md`            | Quick reference guide for the repo layout.                 |

## Notes
- `package contactapp;` declaration at the top of every Java file so the folder layout and c/ompiler stay in sync.
- Every requirement from `requirements/requirements.md` has at least one test case to demonstrate full coverage.
- `docs/index.md` show all the key files and folders for easy navigation.
