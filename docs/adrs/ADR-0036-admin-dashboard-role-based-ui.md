# ADR-0036: Admin Dashboard and Role-Based UI

**Status**: Accepted  
**Date**: 2025-12-01  
**Owners**: Justin Guida  

**Related**: ADR-0017, ADR-0018, ADR-0025, [REQUIREMENTS.md](../REQUIREMENTS.md)

## Context
- The React SPA currently exposes a single dashboard experience for every authenticated user.
- Upcoming Phase 5 work introduces Spring Security + JWT-based roles (user vs. admin).
- Administrators need richer visibility (aggregate metrics, audit activity, user management) than standard CRUD operators.
- We want to expand functionality without spinning up a separate frontend or duplicating endpoints.

## Decision
- Reuse the existing `ui/contact-app` shell and introduce role-aware routing/navigation.
- Extend the authentication payload (`/auth/login` response + JWT claims) with roles; the SPA reads them on boot to toggle admin routes.
- Server controllers will add `/api/v1/admin/**` endpoints protected with `@PreAuthorize("hasRole('ADMIN')")` for metrics, audit feeds, or future management APIs.
- Admin-only UI modules (Phase 7) will surface aggregate stats, recent activity, and management actions that are hidden for regular users.
- The default user experience remains unchanged; admins simply see additional navigation items and pages once authenticated.

## Consequences
- Keeps a single SPA/JAR deployment while enabling differentiated experiences.
- Security remains centralized: server-side `@PreAuthorize` plus client-side route guards ensure defense in depth.
- Requires additional UI/UX work (charts, tables, filters) and end-to-end tests scoped to admin roles.
- Documentation and onboarding must highlight the admin feature set and required env configuration (e.g., seeded admin account).

## Alternatives Considered
- **Separate admin portal** – rejected to avoid duplicating build pipelines, deployments, and shared UI components.
- **Role-aware routing at initial boot only** – discarded because it would have forced a hard navigation split and complicated future feature flags.
- **Server-side enforcement without client awareness** – insufficient feedback for admins (links would 403 without explanation) and poor UX.
- **Feature-flag-only gating** – flags alone cannot guarantee unauthorized users never see protected routes; roles still need to flow end to end.

## Timeline & Rollout
- **Phase 5** – Embed roles inside JWTs and lock down `/api/v1/admin/**` endpoints with backend authorization. SPA remains unchanged so this is a backend-only release.
- **Phase 6** – SPA reads roles during bootstrapping to pre-compute route guards, but admin navigation/modules stay hidden until Phase 7. This lets us validate claims parsing without exposing UI.
- **Phase 7** – Enable admin-only UI modules and navigation links, optionally guarded by feature flags for canary rollouts. Incremental rollout includes migrations/tests that verify role claims, nav rendering, and backwards compatibility before fully enabling admin widgets.
