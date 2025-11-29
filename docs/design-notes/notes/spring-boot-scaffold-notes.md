# Spring Boot Scaffold Design Notes

## Overview
Phase 1 introduces Spring Boot infrastructure to convert the in-memory library into a runnable application with actuator endpoints.

## Key Decisions

### Why Spring Boot 3.4.12?
- Latest patch release in the 3.4.x line with security fixes
- Java 17+ compatibility (matches our JDK target)
- Mature ecosystem with extensive documentation
- ADR-0014 pre-selected Spring Boot as the backend framework

### Package Reorganization
Moved from flat `contactapp` to layered structure:
```
contactapp/
├── Application.java          # Entrypoint
├── domain/                   # Entities + validation
├── service/                  # Business logic
├── api/                      # REST controllers (Phase 2)
└── persistence/              # Repositories (Phase 3)
```

**Why**: Enforces architectural boundaries. Domain classes shouldn't depend on services; services shouldn't depend on controllers. Empty packages are placeholders for future phases.

### Backward Compatibility Strategy
Services have both:
1. `@Service` annotation for Spring DI
2. `getInstance()` static method for existing tests

**Why**: Many existing tests use `getInstance()`. Changing them would be high-churn with no functional benefit.

**How it works**: The backing `ConcurrentHashMap` is static, so all instances share the same data store. The public constructor registers `this` as the static singleton only if no instance exists (thread-safe via `synchronized` block). This ensures Spring DI and `getInstance()` always see the same data, regardless of initialization order or how many instances are created.

### Visibility Changes
Made `copy()` methods public on domain classes.

**Why**: Services are now in a different package (`contactapp.service`) from domain classes (`contactapp.domain`). Package-private methods can't cross package boundaries. Public `copy()` is acceptable since it creates immutable defensive copies.

### Actuator Lockdown
Only `/actuator/health` and `/actuator/info` exposed.

**Why OWASP guidance**:
- `/env` - May expose secrets via environment variables
- `/beans` - Reveals internal architecture
- `/metrics` - Aids attackers in profiling the system
- `/mappings` - Exposes all URL routes

Health and info are safe and operationally necessary (Kubernetes probes, ops dashboards).

### Profile Configuration
Three profiles: dev, test, prod

| Profile | Logging | Health Details |
|---------|---------|----------------|
| dev | DEBUG | always |
| test | WARN | when-authorized |
| prod | WARN | when-authorized |

**Why**: Dev needs verbose output for debugging. Test/prod should be quiet and secure by default.

## Testing Strategy

### ApplicationTest
Empty test body with `@SpringBootTest` annotation.

**Why it works**: Spring Boot loads the entire application context before running tests. If wiring fails, the context fails to load, and the test fails. An empty test body is sufficient to verify the context loads.

### ActuatorEndpointsTest
Uses MockMvc to verify HTTP responses from actuator endpoints.

**Why MockMvc**: Full server startup is slow. MockMvc simulates HTTP requests without binding to a port. Still exercises the full Spring MVC stack including security configuration.

### ServiceBeanTest
Injects services via `@Autowired` and verifies they're singletons.

**Why**: Proves component scanning works and beans are wired correctly. Catches misconfiguration before controller tests (Phase 2) fail with cryptic `NoSuchBeanDefinitionException`.

## Trade-offs

### Test Startup Time
Spring Boot tests take ~1 second to load context vs ~10ms for plain JUnit tests.

**Mitigation**: Context is cached across test classes with the same configuration. Only 3 Spring Boot test classes, so overhead is minimal.

### Dependency Footprint
Added ~10 Spring Boot dependencies to the classpath.

**Mitigation**: Spring Boot's dependency management ensures consistent versions. Tree-shaking at build time excludes unused classes from the final JAR.

### Public copy() Methods
Previously package-private, now public API.

**Mitigation**: `copy()` returns an immutable defensive copy. Exposing it publicly is harmless and follows the principle of safe APIs.

## Related ADRs
- ADR-0014: Backend Framework and Persistence Stack
- ADR-0020: Spring Boot Scaffold (new)
