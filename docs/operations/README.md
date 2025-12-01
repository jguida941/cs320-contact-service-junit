# Operations Documentation

Guides for deploying, running, and monitoring the Contact Service application.

## Documents

| Document | Description |
|----------|-------------|
| [DOCKER_SETUP.md](DOCKER_SETUP.md) | Docker setup guide: building images, running with docker-compose, health checks, troubleshooting |
| [ACTUATOR_ENDPOINTS.md](ACTUATOR_ENDPOINTS.md) | Spring Boot Actuator endpoints reference: health, metrics, prometheus, liveness/readiness probes |

## Quick Reference

### Project Root Config Files

These files live at the project root (required by Docker tooling):

| File | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage production image (Eclipse Temurin 17 JRE, non-root user, layered JAR) |
| `docker-compose.yml` | Production-like stack: Postgres + App + optional pgAdmin |
| `docker-compose.dev.yml` | Development stack: Postgres only (for local `mvn spring-boot:run`) |
| `.env.example` | Environment variable template (copy to `.env` and customize) |
| `.dockerignore` | Excludes unnecessary files from Docker build context |

### Common Commands

```bash
# Development (Postgres + local Maven)
docker compose -f docker-compose.dev.yml up -d
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Production-like (full Docker stack)
cp .env.example .env  # Edit with secure values
docker compose up -d --build

# Check health
curl http://localhost:8080/actuator/health

# View Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# View logs
docker compose logs -f app

# Stop
docker compose down        # Keep data
docker compose down -v     # Remove data volumes
```

### Key Endpoints

| Endpoint | Purpose |
|----------|---------|
| `http://localhost:8080` | Application (React UI + REST API) |
| `http://localhost:8080/actuator/health` | Health check |
| `http://localhost:8080/actuator/health/liveness` | Kubernetes liveness probe |
| `http://localhost:8080/actuator/health/readiness` | Kubernetes readiness probe |
| `http://localhost:8080/actuator/prometheus` | Prometheus metrics |
| `http://localhost:8080/swagger-ui.html` | API documentation |
| `http://localhost:5050` | pgAdmin (if enabled in docker-compose.yml) |

## Related ADRs

- [ADR-0019: Deployment and Packaging](../adrs/ADR-0019-deployment-and-packaging.md)
- [ADR-0039: Phase 5 Security and Observability](../adrs/ADR-0039-phase5-security-observability.md)
