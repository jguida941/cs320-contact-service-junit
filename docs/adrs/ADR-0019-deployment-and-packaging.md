# ADR-0019: Deployment and Packaging

**Status**: Accepted  
**Date**: 2025-11-29  
**Owners**: Justin Guida  

**Related**: ADR-0014, ADR-0015, ADR-0016, ADR-0017, ADR-0018, [REQUIREMENTS.md](../REQUIREMENTS.md)

## Context
- The current project is a library with no deployable runtime or packaging story.
- We need a consistent way to ship backend + frontend + database for dev, CI, and prod.

## Decision
- Package backend as a Spring Boot container image.
- Package frontend as a static build served via CDN/reverse proxy (or bundled with backend if needed).
- Use Dockerfiles for backend/UI and docker-compose for local dev (app + Postgres + optional pgAdmin).
- CI builds and pushes images to GitHub Container Registry; artifacts include OpenAPI docs and UI build.
- Release cadence: feature-based tags; semantic-ish versioning (MAJOR.MINOR.PATCH) when stabilized.

## Consequences
- Containerization standardizes environments and simplifies onboarding.
- Compose enables local parity with prod-like services.
- Adds CI steps (image build/push, vulnerability scans) and registry management overhead.
- Ties build to Docker availability; alternative packaging needed for non-container environments.
