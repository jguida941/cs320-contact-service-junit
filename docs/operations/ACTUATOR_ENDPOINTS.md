# Actuator Endpoints Reference

## Overview

Spring Boot Actuator provides production-ready monitoring and management capabilities. This document lists all enabled endpoints and their usage.

## Base URL

All actuator endpoints are available at: `http://localhost:8080/actuator`

## Enabled Endpoints

### 1. Health Endpoint

**URL**: `/actuator/health`

**Purpose**: Provides health status of the application and its dependencies (database, disk space, etc.)

**Response Example**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 300000000000,
        "threshold": 10485760,
        "exists": true
      }
    }
  }
}
```

**Usage**:
```bash
# Basic health check
curl http://localhost:8080/actuator/health

# Liveness probe (Kubernetes)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (Kubernetes)
curl http://localhost:8080/actuator/health/readiness
```

---

### 2. Info Endpoint

**URL**: `/actuator/info`

**Purpose**: Displays application information (version, build time, git commit, etc.)

**Configuration**: Add to `application.yml`:
```yaml
info:
  app:
    name: ${spring.application.name}
    version: @project.version@
    description: @project.description@
```

**Response Example**:
```json
{
  "app": {
    "name": "contact-service",
    "version": "1.0.0-SNAPSHOT",
    "description": "Contact, Task, and Appointment management service"
  }
}
```

**Usage**:
```bash
curl http://localhost:8080/actuator/info
```

---

### 3. Prometheus Endpoint

**URL**: `/actuator/prometheus`

**Purpose**: Exposes metrics in Prometheus format for scraping by Prometheus server

**Response Example** (abbreviated):
```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Survivor Space",application="contact-service",environment="docker"} 8388608.0
jvm_memory_used_bytes{area="heap",id="G1 Old Gen",application="contact-service",environment="docker"} 5.0331648E7

# HELP http_server_requests_seconds Duration of HTTP server request handling
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/v1/contacts",application="contact-service",environment="docker"} 42.0
http_server_requests_seconds_sum{exception="None",method="GET",outcome="SUCCESS",status="200",uri="/api/v1/contacts",application="contact-service",environment="docker"} 0.523
```

**Usage**:
```bash
# View all metrics
curl http://localhost:8080/actuator/prometheus

# Filter specific metrics
curl http://localhost:8080/actuator/prometheus | grep http_server_requests

# Count JVM metrics
curl http://localhost:8080/actuator/prometheus | grep -c jvm_
```

**Prometheus Configuration** (`prometheus.yml`):
```yaml
scrape_configs:
  - job_name: 'contactapp'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
    static_configs:
      - targets: ['app:8080']
        labels:
          service: 'contact-service'
```

---

### 4. Metrics Endpoint

**URL**: `/actuator/metrics`

**Purpose**: Provides detailed metrics in JSON format (alternative to Prometheus)

**Available Metrics**:
```bash
curl http://localhost:8080/actuator/metrics
```

Response shows available metric names:
```json
{
  "names": [
    "jvm.memory.used",
    "jvm.gc.pause",
    "http.server.requests",
    "system.cpu.usage",
    "process.uptime",
    "hikaricp.connections.active",
    "spring.data.repository.invocations"
  ]
}
```

**View Specific Metric**:
```bash
# JVM memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP request metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# Database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

**Response Example** (`/actuator/metrics/http.server.requests`):
```json
{
  "name": "http.server.requests",
  "description": "Duration of HTTP server request handling",
  "baseUnit": "seconds",
  "measurements": [
    {
      "statistic": "COUNT",
      "value": 42.0
    },
    {
      "statistic": "TOTAL_TIME",
      "value": 0.523
    },
    {
      "statistic": "MAX",
      "value": 0.089
    }
  ],
  "availableTags": [
    {
      "tag": "exception",
      "values": ["None", "IllegalArgumentException"]
    },
    {
      "tag": "method",
      "values": ["GET", "POST", "PUT", "DELETE"]
    },
    {
      "tag": "uri",
      "values": ["/api/v1/contacts", "/api/v1/tasks", "/api/v1/appointments"]
    },
    {
      "tag": "outcome",
      "values": ["SUCCESS", "CLIENT_ERROR", "SERVER_ERROR"]
    },
    {
      "tag": "status",
      "values": ["200", "201", "400", "404", "500"]
    }
  ]
}
```

---

## Custom Metrics Tags

All metrics include these common tags for filtering and grouping:

- **application**: `contact-service` (from `spring.application.name`)
- **environment**: `local`, `docker`, `staging`, or `production` (from `ENVIRONMENT` env var)

### Querying by Tag

**Prometheus PromQL**:
```promql
# Filter by application
http_server_requests_seconds_count{application="contact-service"}

# Filter by environment
http_server_requests_seconds_count{environment="production"}

# Multiple filters
rate(http_server_requests_seconds_count{
  application="contact-service",
  environment="production",
  status="200"
}[5m])
```

**Grafana Dashboard Variables**:
```
Environment: label_values(http_server_requests_seconds_count, environment)
Application: label_values(http_server_requests_seconds_count, application)
```

---

## Key Metrics to Monitor

### Application Performance

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `http.server.requests` | HTTP request count and duration | p95 > 1s |
| `http.server.requests{status="5xx"}` | Server error rate | > 1% of requests |
| `http.server.requests{status="4xx"}` | Client error rate | > 10% of requests |
| `spring.data.repository.invocations` | Database query count | - |

### JVM Health

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `jvm.memory.used{area="heap"}` | Heap memory usage | > 85% of max |
| `jvm.gc.pause` | GC pause duration | p99 > 1s |
| `jvm.threads.live` | Active thread count | > 200 |
| `jvm.threads.daemon` | Daemon thread count | - |
| `process.cpu.usage` | Process CPU usage | > 80% |

### Database Connection Pool

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `hikaricp.connections.active` | Active DB connections | Near max pool size |
| `hikaricp.connections.pending` | Waiting for connection | > 0 for 1min |
| `hikaricp.connections.timeout` | Connection timeout count | > 0 |

### System Resources

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `system.cpu.usage` | System CPU usage | > 90% |
| `disk.free` | Free disk space | < 10% |
| `process.uptime` | Application uptime | - |

---

## Example Prometheus Alerts

Create alerts in `prometheus-rules.yml`:

```yaml
groups:
  - name: contactapp_alerts
    interval: 30s
    rules:
      # High error rate
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5..",application="contact-service"}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "{{ $labels.application }} has {{ $value | humanizePercentage }} error rate"

      # Slow response time
      - alert: SlowResponseTime
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket{application="contact-service"}[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow response time detected"
          description: "95th percentile response time is {{ $value }}s"

      # High memory usage
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap",application="contact-service"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High heap memory usage"
          description: "JVM heap usage is {{ $value | humanizePercentage }}"

      # Database connection pool exhaustion
      - alert: ConnectionPoolExhaustion
        expr: hikaricp_connections_pending{application="contact-service"} > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Database connection pool exhausted"
          description: "{{ $value }} connections are waiting for availability"
```

---

## Security Considerations

### Production Configuration

In production, restrict actuator endpoints to monitoring systems only:

**application-prod.yml**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus  # Remove 'metrics' and 'info'
  endpoint:
    health:
      show-details: never  # Hide sensitive details
```

### Network Policies

**Docker**: Use internal networks
```yaml
networks:
  monitoring:
    internal: false  # Allow Prometheus to scrape
  app:
    internal: true   # Isolate application
```

**Kubernetes**: Use NetworkPolicy
```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-prometheus-scrape
spec:
  podSelector:
    matchLabels:
      app: contactapp
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              name: monitoring
      ports:
        - protocol: TCP
          port: 8080
```

### Authentication

Add Spring Security to protect actuator endpoints:

```java
@Configuration
public class ActuatorSecurityConfig {
    @Bean
    public SecurityFilterChain actuatorSecurity(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/prometheus").hasRole("MONITORING")
                .anyRequest().hasRole("ADMIN")
            )
            .httpBasic();
        return http.build();
    }
}
```

---

## Grafana Dashboards

### Recommended Dashboards

1. **Spring Boot 2.x Statistics** (ID: 4701)
   - JVM memory, GC, threads
   - HTTP request metrics
   - Database connection pool

2. **JVM Micrometer** (ID: 11892)
   - Detailed JVM metrics
   - Garbage collection analysis
   - Class loading stats

### Import Dashboard

1. Open Grafana → Dashboards → Import
2. Enter dashboard ID: `4701`
3. Select Prometheus data source
4. Import

### Custom Panels

**Request Rate**:
```promql
sum(rate(http_server_requests_seconds_count{application="contact-service"}[5m])) by (uri, method)
```

**Error Rate**:
```promql
sum(rate(http_server_requests_seconds_count{status=~"5..",application="contact-service"}[5m])) /
sum(rate(http_server_requests_seconds_count{application="contact-service"}[5m]))
```

**Response Time (p95)**:
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="contact-service"}[5m])) by (le, uri))
```

---

## Testing Endpoints

Use the provided test script:

```bash
#!/bin/bash
# test-actuator.sh

BASE_URL="http://localhost:8080/actuator"

echo "Testing Actuator Endpoints..."

echo -e "\n1. Health Check:"
curl -s $BASE_URL/health | jq '.'

echo -e "\n2. Liveness Probe:"
curl -s $BASE_URL/health/liveness | jq '.'

echo -e "\n3. Readiness Probe:"
curl -s $BASE_URL/health/readiness | jq '.'

echo -e "\n4. Info:"
curl -s $BASE_URL/info | jq '.'

echo -e "\n5. Metrics List:"
curl -s $BASE_URL/metrics | jq '.names | length' | xargs echo "Total metrics:"

echo -e "\n6. Prometheus Metrics:"
curl -s $BASE_URL/prometheus | head -20

echo -e "\n7. JVM Memory:"
curl -s $BASE_URL/metrics/jvm.memory.used | jq '.'

echo -e "\nAll tests complete!"
```

Make executable and run:
```bash
chmod +x test-actuator.sh
./test-actuator.sh
```

---

## Troubleshooting

### Endpoint Returns 404

Check that endpoints are exposed in `application.yml`:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
```

### Prometheus Endpoint Empty

Verify dependency in `pom.xml`:
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

### Health Shows DOWN

Check database connectivity:
```bash
docker-compose logs postgres
docker-compose exec app sh -c 'curl localhost:8080/actuator/health | jq .'
```

---

## References

- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Micrometer Prometheus Documentation](https://micrometer.io/docs/registry/prometheus)
- [Prometheus Query Language](https://prometheus.io/docs/prometheus/latest/querying/basics/)
- [Grafana Dashboard Gallery](https://grafana.com/grafana/dashboards/)
