# CS320 Milestone 1 - Repository Index

Index for easy navigation of the CS320 Milestone 1 codebase.

## Planning & Requirements

| Path | Purpose |
|------|---------|
| [`REQUIREMENTS.md`](REQUIREMENTS.md) | **Master document**: scope, architecture, phases, checklist, code examples |
| [`ROADMAP.md`](ROADMAP.md) | Quick phase overview (points to REQUIREMENTS.md) |
| [`../agents.md`](../agents.md) | AI assistant entry point with constraints and stack decisions |

## Folders

| Path | Purpose |
|------|---------|
| [`../src/`](../src/) | Java source tree. `src/main/java/contactapp` contains application code; `src/test/java/contactapp` contains tests. |
| [`ci-cd/`](ci-cd/) | CI/CD design notes (pipeline plan plus `badges.md` for the badge helper). |
| [`requirements/contact-requirements/`](requirements/contact-requirements/) | Contact assignment requirements (milestone spec). |
| [`requirements/appointment-requirements/`](requirements/appointment-requirements/) | Appointment assignment requirements (object/service specs + checklist). |
| [`requirements/task-requirements/`](requirements/task-requirements/) | Task assignment requirements (task object/service specs + checklist). |
| [`architecture/`](architecture/) | Feature design briefs (e.g., Task entity/service plan with Definition of Done). |
| [`adrs/`](adrs/) | Architecture Decision Records index plus individual ADR files. |
| [`design-notes/`](design-notes/) | Personal design note hub with supporting explanations under `design-notes/notes/`. |
| [`logs/`](logs/) | Changelog and backlog. |

## Key Files

| Path | Description |
|------|-------------|
| [`../src/main/java/contactapp/Contact.java`](../src/main/java/contactapp/Contact.java) | `Contact` domain object with all field validation rules. |
| [`../src/main/java/contactapp/ContactService.java`](../src/main/java/contactapp/ContactService.java) | In-memory service that adds, updates, and deletes contacts. |
| [`../src/main/java/contactapp/Validation.java`](../src/main/java/contactapp/Validation.java) | Shared helper with not-blank, length, 10-digit, and date-not-past checks. |
| [`../src/main/java/contactapp/Task.java`](../src/main/java/contactapp/Task.java) | Task domain object mirroring Contact-style validation (id/name/description). |
| [`../src/main/java/contactapp/TaskService.java`](../src/main/java/contactapp/TaskService.java) | Task service with in-memory CRUD and validation/trim guards on IDs. |
| [`../src/main/java/contactapp/Appointment.java`](../src/main/java/contactapp/Appointment.java) | Appointment entity (id/date/description) with date-not-past validation. |
| [`../src/main/java/contactapp/AppointmentService.java`](../src/main/java/contactapp/AppointmentService.java) | Appointment service singleton with in-memory CRUD and ID trim/validation guards. |
| [`../src/test/java/contactapp/ContactTest.java`](../src/test/java/contactapp/ContactTest.java) | JUnit tests covering the `Contact` validation requirements. |
| [`../src/test/java/contactapp/ContactServiceTest.java`](../src/test/java/contactapp/ContactServiceTest.java) | JUnit tests covering add, delete, and update behavior. |
| [`../src/test/java/contactapp/ValidationTest.java`](../src/test/java/contactapp/ValidationTest.java) | Tests for the shared validation helper (length, numeric, and appointment date guards). |
| [`../src/test/java/contactapp/TaskTest.java`](../src/test/java/contactapp/TaskTest.java) | JUnit tests for Task (trimming, invalid inputs, and atomic update behavior). |
| [`../src/test/java/contactapp/TaskServiceTest.java`](../src/test/java/contactapp/TaskServiceTest.java) | JUnit tests for TaskService singleton behavior and CRUD flows. |
| [`../src/test/java/contactapp/AppointmentTest.java`](../src/test/java/contactapp/AppointmentTest.java) | JUnit tests for Appointment entity validation and date rules. |
| [`../src/test/java/contactapp/AppointmentServiceTest.java`](../src/test/java/contactapp/AppointmentServiceTest.java) | JUnit tests for AppointmentService singleton and CRUD flows. |
| [`../pom.xml`](../pom.xml) | Maven project file defining dependencies and plugins. |
| [`../config/checkstyle/checkstyle.xml`](../config/checkstyle/checkstyle.xml) | Custom Checkstyle rules enforced in CI. |
| [`../config/owasp-suppressions.xml`](../config/owasp-suppressions.xml) | Placeholder suppression list for OWASP Dependency-Check. |
| [`../scripts/ci_metrics_summary.py`](../scripts/ci_metrics_summary.py) | Prints the QA metrics table (tests/coverage/mutations/dependencies) in GitHub Actions. |
| [`../scripts/serve_quality_dashboard.py`](../scripts/serve_quality_dashboard.py) | Launches a local server for `target/site/qa-dashboard` when reading downloaded artifacts. |
| [`architecture/2025-11-19-task-entity-and-service.md`](architecture/2025-11-19-task-entity-and-service.md) | Task entity/service plan with Definition of Done and phase breakdown. |
| [`architecture/2025-11-24-appointment-entity-and-service.md`](architecture/2025-11-24-appointment-entity-and-service.md) | Appointment entity/service implementation record. |
| [`adrs/README.md`](adrs/README.md) | ADR index summarizing ADR-0001 through ADR-0019. |
| [`design-notes/README.md`](design-notes/README.md) | Landing page for informal design notes (individual topics in `design-notes/notes/`). |
| [`logs/backlog.md`](logs/backlog.md) | Backlog for reporting and domain enhancements. |
| [`logs/CHANGELOG.md`](logs/CHANGELOG.md) | Project changelog. |
| [`../.github/workflows`](../.github/workflows) | GitHub Actions pipelines (CI, release packaging, CodeQL). |

## Milestone Requirements (Original Assignments)

| Path | Description |
|------|-------------|
| [`requirements/contact-requirements/requirements.md`](requirements/contact-requirements/requirements.md) | Contact assignment requirements. |
| [`requirements/contact-requirements/requirements_checklist.md`](requirements/contact-requirements/requirements_checklist.md) | Contact requirements checklist. |
| [`requirements/appointment-requirements/requirements.md`](requirements/appointment-requirements/requirements.md) | Appointment assignment requirements. |
| [`requirements/appointment-requirements/requirements_checklist.md`](requirements/appointment-requirements/requirements_checklist.md) | Appointment requirements checklist. |
| [`requirements/task-requirements/requirements.md`](requirements/task-requirements/requirements.md) | Task assignment requirements (task object/service). |
| [`requirements/task-requirements/requirements_checklist.md`](requirements/task-requirements/requirements_checklist.md) | Task requirements checklist. |
