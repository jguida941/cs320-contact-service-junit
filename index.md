# CS320 Repository Index

Index for easy navigation of the CS320 Milestone 1 codebase.

## Folders

| Path                                                                                         | Purpose                                                                                                                      |
|----------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------|
| [`src/`](src/)                                                                               | Java source tree. <br/>`src/main/java/contactapp` contains application code; <br/>`src/test/java/contactapp` contains tests. |
| [`docs/ci-cd/`](docs/ci-cd/)                                                                 | CI/CD design notes (pipeline plan plus `badges.md` for the badge helper).                                                    |
| [`docs/requirements/contact-requirements/`](docs/requirements/contact-requirements/)         | Contact assignment requirements (now collocated under `docs/`).                                                              |
| [`docs/requirements/appointment-requirements/`](docs/requirements/appointment-requirements/) | Appointment assignment requirements (object/service specs + checklist).                                                      |
| [`docs/requirements/task-requirements/`](docs/requirements/task-requirements/)               | Task assignment requirements (task object/service specs + checklist).                                                        |
| [`docs/architecture/`](docs/architecture/)                                                   | Feature design briefs (e.g., Task entity/service plan with Definition of Done).                                              |
| [`docs/adrs/`](docs/adrs/)                                                                   | Architecture Decision Records index plus individual ADR files.                                                               |
| [`docs/design-notes/`](docs/design-notes/)                                                   | Personal design note hub with supporting explanations under `docs/design-notes/notes/`.                                      |

## Key Files

| Path                                                                                                                 | Description                                                                               |
|----------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| [`src/main/java/contactapp/Contact.java`](src/main/java/contactapp/Contact.java)                                     | `Contact` domain object with all field validation rules.                                  |
| [`src/main/java/contactapp/ContactService.java`](src/main/java/contactapp/ContactService.java)                       | In-memory service that adds, updates, and deletes contacts.                               |
| [`src/main/java/contactapp/Validation.java`](src/main/java/contactapp/Validation.java)                               | Shared helper with not-blank, length, 10-digit, and date-not-past checks.                 |
| [`src/main/java/contactapp/Task.java`](src/main/java/contactapp/Task.java)                                           | Task domain object mirroring Contact-style validation (id/name/description).              |
| [`src/main/java/contactapp/TaskService.java`](src/main/java/contactapp/TaskService.java)                             | Task service with in-memory CRUD and validation/trim guards on IDs.                       |
| [`src/main/java/contactapp/Appointment.java`](src/main/java/contactapp/Appointment.java)                             | Appointment entity (id/date/description) with date-not-past validation.                   |
| [`src/main/java/contactapp/AppointmentService.java`](src/main/java/contactapp/AppointmentService.java)               | Appointment service singleton with in-memory CRUD and ID trim/validation guards.          |
| [`src/test/java/contactapp/ContactTest.java`](src/test/java/contactapp/ContactTest.java)                             | JUnit tests covering the `Contact` validation requirements.                               |
| [`src/test/java/contactapp/ContactServiceTest.java`](src/test/java/contactapp/ContactServiceTest.java)               | JUnit tests covering add, delete, and update behavior.                                    |
| [`src/test/java/contactapp/ValidationTest.java`](src/test/java/contactapp/ValidationTest.java)                       | Tests for the shared validation helper (length, numeric, and appointment date guards).    |
| [`src/test/java/contactapp/TaskTest.java`](src/test/java/contactapp/TaskTest.java)                                   | JUnit tests for Task (trimming, invalid inputs, and atomic update behavior).              |
| [`src/test/java/contactapp/TaskServiceTest.java`](src/test/java/contactapp/TaskServiceTest.java)                     | JUnit tests for TaskService singleton behavior and CRUD flows.                            |
| [`src/test/java/contactapp/AppointmentTest.java`](src/test/java/contactapp/AppointmentTest.java)                     | JUnit tests for Appointment entity validation and date rules.                             |
| [`src/test/java/contactapp/AppointmentServiceTest.java`](src/test/java/contactapp/AppointmentServiceTest.java)       | JUnit tests for AppointmentService singleton and CRUD flows.                              |
| [`pom.xml`](pom.xml)                                                                                                 | Maven project file defining dependencies and plugins.                                     |
| [`config/checkstyle/checkstyle.xml`](config/checkstyle/checkstyle.xml)                                               | Custom Checkstyle rules enforced in CI.                                                   |
| [`config/owasp-suppressions.xml`](config/owasp-suppressions.xml)                                                     | Placeholder suppression list for OWASP Dependency-Check.                                  |
| [`scripts/ci_metrics_summary.py`](scripts/ci_metrics_summary.py)                                                     | Prints the QA metrics table (tests/coverage/mutations/dependencies) in GitHub Actions.    |
| [`scripts/serve_quality_dashboard.py`](scripts/serve_quality_dashboard.py)                                           | Launches a local server for `target/site/qa-dashboard` when reading downloaded artifacts. |
| [`docs/architecture/2025-11-19-task-entity-and-service.md`](docs/architecture/2025-11-19-task-entity-and-service.md) | Task entity/service plan with Definition of Done and phase breakdown.                     |
| [`docs/adrs/README.md`](docs/adrs/README.md)                                                                         | ADR index summarizing ADR-0001 through ADR-0013.                                          |
| [`docs/design-notes/README.md`](docs/design-notes/README.md)                                                         | Landing page for informal design notes (individual topics in `docs/design-notes/notes/`). |
| [`docs/logs/backlog.md`](docs/logs/backlog.md)                                                                       | Backlog for reporting and domain enhancements.                                            |
| [`.github/workflows`](.github/workflows)                                                                             | GitHub Actions pipelines (CI, release packaging, CodeQL).                                 |

## Requirements & Notes

| Path                                                                                                                                           | Description                                         |
|------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| [`docs/requirements/contact-requirements/requirements.md`](docs/requirements/contact-requirements/requirements.md)                             | Contact assignment requirements.                    |
| [`docs/requirements/contact-requirements/requirements_checklist.md`](docs/requirements/contact-requirements/requirements_checklist.md)         | Contact requirements checklist.                     |
| [`docs/requirements/appointment-requirements/requirements.md`](docs/requirements/appointment-requirements/requirements.md)                     | Appointment assignment requirements.                |
| [`docs/requirements/appointment-requirements/requirements_checklist.md`](docs/requirements/appointment-requirements/requirements_checklist.md) | Appointment requirements checklist.                 |
| [`docs/requirements/task-requirements/requirements.md`](docs/requirements/task-requirements/requirements.md)                                   | Task assignment requirements (task object/service). |
| [`docs/requirements/task-requirements/requirements_checklist.md`](docs/requirements/task-requirements/requirements_checklist.md)               | Task requirements checklist.                        |
| [`index.md`](index.md)                                                                                                                         | Documentation index and navigation entry.           |
| [`docs/architecture/2025-11-24-appointment-entity-and-service.md`](docs/architecture/2025-11-24-appointment-entity-and-service.md)             | Appointment entity/service implementation record.   |
