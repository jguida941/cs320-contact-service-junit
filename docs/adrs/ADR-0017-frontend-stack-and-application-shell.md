# ADR-0017: Frontend Stack and Application Shell

**Status**: Accepted | **Date**: 2025-11-29 | **Owners**: Justin Guida

**Related**: ADR-0016, [REQUIREMENTS.md](../REQUIREMENTS.md)

## Context
- The only UI in the repo is a QA metrics dashboard; no user-facing app exists.
- We need a maintainable stack for CRUD flows, form validation, routing, and API consumption.
- Frontend must align with the REST contract and support testing and packaging.

## Decision
- Use React with Vite and TypeScript for the product UI (separate from the QA dashboard).
- Structure under `ui/contact-app` with routing, shared layout, and feature modules for Contacts/Tasks/Appointments.
- Use a thin API client wrapper on `fetch` plus TanStack Query for data fetching/state.
- Keep styling lightweight with a small design system (CSS variables + utility classes) rather than a heavy component library.
- Add unit/component tests with Vitest + React Testing Library and E2E with Playwright.

## Consequences
- Fast dev/build cycle via Vite; TypeScript improves safety and refactors.
- Testing tools align with modern React practices.
- Adds bundling/CI steps and node toolchain requirements.
- Requires UX patterns and accessibility discipline; decisions on UI kit still open.
