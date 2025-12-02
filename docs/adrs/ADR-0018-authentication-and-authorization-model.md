# ADR-0018: Authentication and Authorization Model

**Status**: Accepted | **Date**: 2025-11-29 | **Owners**: Justin Guida

**Related**: ADR-0014, ADR-0016, ADR-0017, ADR-0019

## Context
- The current library has no user identity or access control.
- Exposing APIs and a UI requires protecting mutations and user data.
- Early choices should be lightweight but leave room for stronger controls later.

## Decision
- Use stateless JWT bearer auth for API access; secure all mutating endpoints.
- Enforce role-based authorization with Spring Security + `@PreAuthorize` at controller/service boundaries.
- Predefined roles: `ROLE_USER` (read + own mutations), `ROLE_ADMIN` (full access); assigned at registration or by admin.
- Configure CORS for the SPA origin and apply standard security headers (CSP, HSTS, X-Content-Type-Options, X-Frame-Options).
- User store: in-memory users for dev; DB-backed users for prod with BCrypt-hashed passwords (via Spring Security's `BCryptPasswordEncoder`).
- BCrypt was selected because Spring Security ships a hardened encoder, the ops team already maintains FIPS-validated BCrypt implementations, and CI runners lack the Argon2 native libs we would otherwise need. Argon2 remains the long-term goal because of its memory hardness; the trade-off today is accepting a CPU-hard but memory-light algorithm in exchange for better portability, simpler dependency management, and predictable performance characteristics on our constrained dev/prod hosts.
- Secrets management: environment variables for dev/test; vault/cloud secret manager (e.g., HashiCorp Vault/AWS Secrets Manager) for prod.
- Token strategy: short-lived access tokens (e.g., 30 minutes); refresh tokens optional in a later iteration.
- Rate limiting to be applied at the gateway/reverse proxy layer when deployed.

### Migration & Rollback Plan

- All password rows include the BCrypt version marker (the `$2a$` prefix). When we are ready to introduce Argon2, we will add an `algorithm` column (or reuse the prefix) so services can detect the hash type per user. Legacy BCrypt hashes will continue to authenticate until the user logs in; at that point we will transparently rehash the password with Argon2 and update the row.
- To avoid locking ourselves into BCrypt forever, we will keep the password-encoder bean pluggable via configuration so we can stage Argon2 in lower environments first. Rollback simply swaps the encoder bean back to BCrypt and leaves existing BCrypt hashes untouched.
- During migration we will instrument authentication failures to detect "unknown hash prefix" errors which would indicate a partial rollout or corrupted data. Tests will cover both algorithms, and data migration scripts will include a dry-run mode to verify no unsupported hashes exist before switching production traffic.

## Consequences
- Provides a baseline security posture for the new UI/API.
- Adds login flow work on the frontend and token/header handling in the API client.
- Requires secure secret management and transport (HTTPS) in deployment.
- Future upgrades (OAuth2/SSO) will need migration paths if requirements grow.
