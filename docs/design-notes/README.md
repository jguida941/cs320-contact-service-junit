# Design Notes

This folder holds personal explanations of design decisions in plain language. These notes back up the formal ADRs and architecture docs but are not part of the graded deliverables.

## Quick Links

| Topic | Description |
|-------|-------------|
| [Testing Strategy](notes/testing-strategy-notes.md) | Quality gates, Testcontainers pattern, mutation testing, profile strategy |
| [Security Infrastructure](notes/security-infrastructure-notes.md) | JWT auth, CSRF, rate limiting, PII masking, security headers |
| [Frontend Architecture](notes/frontend-architecture-notes.md) | React stack, shadcn/ui, theme system, build integration |
| [REST API Layer](notes/rest-api-layer-notes.md) | Endpoints, HTTP status codes, validation strategy, error handling |
| [Observability Infrastructure](notes/observability-infrastructure-notes.md) | Request tracing, logging, rate limiting, PII masking |
| [CI/CD Operations](notes/ci-cd-operations-notes.md) | Jobs, commands, matrix verification, quality gates, self-hosted setup |
| [Backend Domain Reference](notes/backend-domain-reference.md) | Comprehensive validation pipelines, persistence flows, scenario coverage |

## All Design Notes

The actual note pages live under [`notes/`](notes/):

| File | Topic |
|------|-------|
| [testing-strategy-notes.md](notes/testing-strategy-notes.md) | Testing philosophy, quality gates, Testcontainers |
| [security-infrastructure-notes.md](notes/security-infrastructure-notes.md) | Authentication, authorization, observability |
| [frontend-architecture-notes.md](notes/frontend-architecture-notes.md) | React 19 + Vite + shadcn/ui stack |
| [validation-and-contact-notes.md](notes/validation-and-contact-notes.md) | Validation helper design |
| [contact-service-storage-notes.md](notes/contact-service-storage-notes.md) | Storage abstraction pattern |
| [contact-update-atomic-notes.md](notes/contact-update-atomic-notes.md) | Atomic update semantics |
| [task-entity-and-service-notes.md](notes/task-entity-and-service-notes.md) | Task domain design |
| [appointment-entity-and-service-notes.md](notes/appointment-entity-and-service-notes.md) | Appointment domain design |
| [ci-gates-notes.md](notes/ci-gates-notes.md) | CI quality gate implementation |
| [ci-metrics-script-notes.md](notes/ci-metrics-script-notes.md) | Metrics collection script |
| [docs-structure-notes.md](notes/docs-structure-notes.md) | Documentation organization |
| [mapper-pattern-notes.md](notes/mapper-pattern-notes.md) | Entity-domain mapping |
| [spring-boot-scaffold-notes.md](notes/spring-boot-scaffold-notes.md) | Spring Boot project structure |
| [spotbugs-notes.md](notes/spotbugs-notes.md) | SpotBugs integration |
| [test-design-notes.md](notes/test-design-notes.md) | Test organization patterns |
| [transactional-notes.md](notes/transactional-notes.md) | Transaction management |
| [two-layer-validation-notes.md](notes/two-layer-validation-notes.md) | DTO + domain validation |
| [boolean-return-notes.md](notes/boolean-return-notes.md) | Service method return design |
| [contact-entity-notes.md](notes/contact-entity-notes.md) | Contact domain object |
| [rest-api-layer-notes.md](notes/rest-api-layer-notes.md) | REST endpoints, HTTP status codes, validation |
| [observability-infrastructure-notes.md](notes/observability-infrastructure-notes.md) | Request tracing, logging, rate limiting |
| [ci-cd-operations-notes.md](notes/ci-cd-operations-notes.md) | CI/CD jobs, commands, matrix, caching |
| [backend-domain-reference.md](notes/backend-domain-reference.md) | Validation pipelines, persistence flows, test scenarios |

## Relationship to ADRs

These notes provide informal, plain-language explanations that complement the formal [Architecture Decision Records](../adrs/README.md). Use ADRs for graded deliverables and official decisions; use these notes for deeper understanding.
