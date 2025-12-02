# ADR-0016: API Style and Contract

**Status**: Accepted | **Date**: 2025-11-29 | **Owners**: Justin Guida

**Related**: ADR-0014, ADR-0015, ADR-0017, ADR-0018

## Context
- The project currently lacks an HTTP interface; consumers need a stable contract for Contacts, Tasks, and Appointments.
- We must support validation feedback, error consistency, and future client versions (web UI and potential integrations).

## Decision
- Expose JSON REST APIs for CRUD and basic search/filter endpoints.
- Use request/response DTOs with Bean Validation; keep domain validation as the authority.
- Provide OpenAPI/Swagger UI via springdoc-openapi for discoverability and client generation.
- Add a global exception handler to emit consistent error envelopes `{ "message": "..." }`.
- Resource/versioning: prefix endpoints with `/api/v1` (e.g., `/api/v1/contacts|tasks|appointments/{id}`).
- Pagination/sorting: `page`, `size`, `sort` query params; filtering via simple query params as needed.

## Consequences
- REST + OpenAPI keeps the contract explicit and testable.
- DTO + mapping layer prevents transport concerns from leaking into domain logic.
- Requires disciplined error model and versioning to avoid breaking clients.
- Adds documentation overhead but improves interoperability and testing.
