# Security Reviews

Security audit reports and vulnerability assessments.

## Contents

| Document | Purpose |
|----------|---------|
| [security-review-2025-12-03-comprehensive.md](security-review-2025-12-03-comprehensive.md) | Full security audit with OWASP Top 10 analysis |
| [security-review-2025-12-03-git-diff.md](security-review-2025-12-03-git-diff.md) | Targeted review of recent code changes |

## Review Coverage

Security reviews analyze:
- Authentication and authorization implementation
- Input validation and sanitization
- OWASP Top 10 vulnerabilities
- Dependency vulnerabilities
- Configuration security

## Related

- [Threat Model](../architecture/threat-model.md) - Architectural threat analysis
- [ADR-0038](../adrs/ADR-0038-authentication-implementation.md) - Authentication implementation
- [ADR-0039](../adrs/ADR-0039-phase5-security-observability.md) - Security and observability
- [ADR-0043](../adrs/ADR-0043-httponly-cookie-authentication.md) - HttpOnly cookie authentication
