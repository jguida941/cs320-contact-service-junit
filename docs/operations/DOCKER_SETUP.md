# Docker Setup Guide

## Overview

This project includes a production-ready Docker configuration with:
- Multi-stage Dockerfile with layered JAR extraction
- docker-compose.yml with Spring Boot, PostgreSQL, and pgAdmin
- Prometheus metrics endpoint for monitoring
- Kubernetes-style health probes (liveness/readiness)

## Quick Start

### Prerequisites

- Docker 20.10+ and Docker Compose 2.0+
- At least 2GB free RAM for containers

### 1. Environment Configuration

Copy the example environment file and configure secrets:

```bash
cp .env.example .env
```

Edit `.env` and set secure values:

```bash
# Generate a strong JWT secret
openssl rand -base64 32

# Set strong passwords
POSTGRES_PASSWORD=<your-secure-password>
JWT_SECRET=<generated-jwt-secret>
```

### 2. Start the Stack

```bash
# Build and start all services
docker compose up -d

# View logs
docker compose logs -f app

# Check service health
docker compose ps
```

### 3. Verify Deployment

Access the following endpoints:

- **Application**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Prometheus Metrics**: http://localhost:8080/actuator/prometheus
- **OpenAPI Docs**: http://localhost:8080/swagger-ui.html
- **pgAdmin** (optional): http://localhost:5050

### 4. Stop the Stack

```bash
# Stop containers (keeps data)
docker compose down

# Stop and remove volumes (destroys data!)
docker compose down -v
```

## Docker Commands Reference

### Build

```bash
# Build Docker image manually
docker build -t contactapp:latest .

# Build with specific target
docker build --target builder -t contactapp:builder .
```

### Run Standalone

```bash
# Run container with environment variables
docker run -d \
  --name contactapp \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/contactapp \
  -e DB_USERNAME=contactapp \
  -e DB_PASSWORD=secret \
  -e JWT_SECRET=your-secret \
  contactapp:latest

# View logs
docker logs -f contactapp

# Execute shell in running container
docker exec -it contactapp sh

# Stop container
docker stop contactapp
docker rm contactapp
```

### Docker Compose Commands

```bash
# Start services in background
docker-compose up -d

# Start specific service
docker-compose up -d postgres

# Rebuild and restart
docker-compose up -d --build

# View logs
docker-compose logs -f app
docker-compose logs --tail=100 postgres

# Scale application (requires load balancer)
docker-compose up -d --scale app=3

# Execute commands in container
docker-compose exec app sh
docker-compose exec postgres psql -U contactapp -d contactapp

# Clean up everything
docker-compose down -v --remove-orphans
```

## Health Checks

### Liveness Probe
Checks if the application is running:
```bash
curl http://localhost:8080/actuator/health/liveness
```

### Readiness Probe
Checks if the application is ready to accept traffic:
```bash
curl http://localhost:8080/actuator/health/readiness
```

### Prometheus Metrics
```bash
# View all metrics
curl http://localhost:8080/actuator/prometheus

# Filter specific metrics
curl http://localhost:8080/actuator/prometheus | grep jvm_memory
```

## Production Deployment

### Security Checklist

- [ ] Remove pgAdmin service from docker-compose.yml
- [ ] Use Docker secrets instead of environment variables
- [ ] Set up network policies to restrict Prometheus endpoint access
- [ ] Enable HTTPS with TLS certificates
- [ ] Run security scan: `docker scan contactapp:latest`
- [ ] Set resource limits in docker-compose.yml
- [ ] Configure log aggregation (e.g., ELK stack)
- [ ] Set up monitoring alerts based on Prometheus metrics

### Resource Limits

The docker-compose.yml includes default resource limits:
- CPU: 0.5-2.0 cores
- Memory: 512MB-1GB

Adjust based on your workload:

```yaml
deploy:
  resources:
    limits:
      cpus: '4.0'
      memory: 2G
    reservations:
      cpus: '1.0'
      memory: 1G
```

### Kubernetes Deployment

For Kubernetes, use the health probes:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 5
```

## Troubleshooting

### Application won't start

```bash
# Check if port 8080 is already in use
lsof -i :8080

# View detailed logs
docker-compose logs app

# Check environment variables
docker-compose exec app env | grep -E "(SPRING|DATABASE|JWT)"
```

### Database connection issues

```bash
# Verify Postgres is healthy
docker-compose ps postgres

# Check Postgres logs
docker-compose logs postgres

# Test connection manually
docker-compose exec postgres psql -U contactapp -d contactapp -c "SELECT version();"
```

### Out of memory

```bash
# Check container resource usage
docker stats

# Increase heap size via JAVA_OPTS
JAVA_OPTS="-XX:MaxRAMPercentage=80.0" docker-compose up -d
```

### Rebuild from scratch

```bash
# Remove all containers, images, and volumes
docker-compose down -v --rmi all
docker system prune -a --volumes

# Rebuild
docker-compose up -d --build
```

## Development vs Production

This repository includes two docker-compose files:

- **docker-compose.dev.yml**: Lightweight Postgres only for local development
- **docker-compose.yml**: Full production stack with health checks, metrics, and pgAdmin

Use `docker-compose.dev.yml` when running the app via `mvn spring-boot:run`:

```bash
# Start dev database only
docker-compose -f docker-compose.dev.yml up -d

# Run application locally
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Performance Optimization

### Layered JAR Benefits

The Dockerfile uses Spring Boot's layered JAR feature:
- Dependencies change rarely → cached layer, fast rebuilds
- Application code changes frequently → small layer, fast uploads

Rebuild time comparison:
- Without layers: ~5 minutes (redownloads all dependencies)
- With layers: ~30 seconds (reuses dependency layers)

### Multi-stage Build

- Build stage: 700MB (JDK + Maven + deps)
- Runtime stage: 200MB (JRE only)

Reduces final image size by 70% and improves security.

## Monitoring Integration

### Prometheus Scrape Configuration

Add to your prometheus.yml:

```yaml
scrape_configs:
  - job_name: 'contactapp'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['contactapp:8080']
```

### Grafana Dashboard

Import the Spring Boot dashboard:
- Dashboard ID: 4701 (Spring Boot 2.x)
- Data source: Prometheus

### Custom Metrics

All metrics include these tags:
- `application`: contact-service
- `environment`: docker/staging/production

Filter by environment in Grafana:
```promql
http_server_requests_seconds_count{application="contact-service",environment="production"}
```

## Support

For issues related to:
- Docker configuration: See [Docker Documentation](https://docs.docker.com)
- Spring Boot Actuator: See [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- Prometheus: See [Prometheus Documentation](https://prometheus.io/docs)
