# ADR-0014: Backend Framework and Persistence Stack

**Status**: Accepted  
**Date**: 2025-11-29  
**Owners**: Justin Guida  

**Related**: [REQUIREMENTS.md](../REQUIREMENTS.md), ADR-0015, ADR-0016, ADR-0018, ADR-0019

## Context
- The current codebase is an in-memory library with no HTTP layer or persistence.
- We need a production-ready runtime with REST endpoints, database access, migrations, and operational tooling.
- Choices here will shape how we structure packages, testing, and deployment for the new app/UI work.

## Decision
- Use Spring Boot 3 as the application framework for HTTP, DI, validation, and actuator endpoints.
- Use Spring Data JPA/Hibernate for persistence; keep existing domain validation as the source of truth.
- Use Flyway for database migrations and schema management.
- Keep environment-specific profiles (`dev`, `test`, `prod`) to isolate configs.
- Stick with a single Maven module for the backend; UI remains in `ui/app` as a separate frontend build.

## Consequences
- Gains mature ecosystem, auto-configured REST stack, and first-class observability hooks.
- Introduces JPA/Hibernate learning curve and schema migration workflow.
- Requires aligning testing strategy (unit + integration with Testcontainers/H2).
- Adds bootstrapping overhead but simplifies future features (security, metrics, health checks).
