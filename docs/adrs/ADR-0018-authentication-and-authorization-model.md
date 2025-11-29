# ADR-0018: Authentication and Authorization Model

**Status**: Accepted  
**Date**: 2025-11-29  
**Owners**: Justin Guida  

**Related**: ADR-0014, ADR-0016, ADR-0017, ADR-0019

## Context
- The current library has no user identity or access control.
- Exposing APIs and a UI requires protecting mutations and user data.
- Early choices should be lightweight but leave room for stronger controls later.

## Decision
- Use stateless JWT bearer auth for API access; secure all mutating endpoints.
- Enforce role-based authorization with Spring Security + `@PreAuthorize` at controller/service boundaries.
- Configure CORS for the SPA origin and apply standard security headers (CSP, HSTS, X-Content-Type-Options, X-Frame-Options).
- User store: in-memory users for dev; DB-backed users for prod with hashed passwords.
- Secrets management: environment variables for dev/test; vault/cloud secret manager (e.g., HashiCorp Vault/AWS Secrets Manager) for prod.
- Token strategy: short-lived access tokens (e.g., 30 minutes); refresh tokens optional in a later iteration.
- Rate limiting to be applied at the gateway/reverse proxy layer when deployed.

## Consequences
- Provides a baseline security posture for the new UI/API.
- Adds login flow work on the frontend and token/header handling in the API client.
- Requires secure secret management and transport (HTTPS) in deployment.
- Future upgrades (OAuth2/SSO) will need migration paths if requirements grow.
