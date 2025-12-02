# ADR-0048: Testcontainers Single Container Lifecycle and Test Cleanup Order

**Status:** Accepted
**Date:** 2025-12-02
**Context:** Migration from H2 to PostgreSQL Testcontainers

## Problem

When switching from H2 (`test` profile) to PostgreSQL Testcontainers (`integration` profile), tests exhibited two critical issues:

### Issue 1: Container Lifecycle Mismatch
Tests hung for 30+ seconds per test class with connection timeouts:
- `TaskControllerTest` created Postgres container on port 49235 → tests passed
- `ContactControllerTest` created NEW container on port 49313
- BUT HikariPool-1 (Spring's cached connection pool) still had connections to port 49235 (dead container)
- Result: Every test waited 30+ seconds for connection timeout, 12+ minutes for full test suite

**Root Cause:** Using `@Testcontainers` + `@Container` annotations creates a NEW container for EACH test class, but Spring Boot's test context caching REUSES the same ApplicationContext (and HikariCP connection pool) across test classes with identical `@SpringBootTest` configuration. This caused port mismatches between the container and the cached connection pool.

### Issue 2: Test Cleanup Order Violations
After fixing container lifecycle, 181 tests failed with:
```
ERROR: null value in column "user_id" of relation "tasks" violates not-null constraint
```

**Root Cause:** `TestCleanupUtility` was deleting users FIRST, then attempting to delete tasks/contacts/appointments. H2 was lenient about constraint violations during cleanup, but PostgreSQL strictly enforces NOT NULL constraints and foreign key relationships.

## Decision

### Fix 1: Single Static Container Pattern
Remove `@Testcontainers/@Container` annotations and use a static initializer in `PostgresContainerSupport` to create ONE container that lives for the ENTIRE test suite:

```java
public abstract class PostgresContainerSupport {
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine");
        postgres.start();
    }

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

**Why This Works:**
- Container starts ONCE when the class loads
- Container remains alive for ALL test classes in the JVM
- Aligns with Spring's test context caching behavior
- Single port (e.g., 52292) used throughout entire test suite
- HikariCP connection pool never needs to reconnect

### Fix 2: Correct Test Cleanup Order
Updated `TestCleanupUtility.cleanAll()` to delete child records BEFORE parent records:

```java
@Transactional
public void cleanAll() {
    // Step 1: Clear security contexts
    clearSecurityContexts();

    // Step 2: Reset singletons BEFORE clearing data
    resetAllSingletons();

    // Step 3: Clear all service data FIRST (tasks/contacts/appointments reference users)
    clearAllServiceData();

    // Step 4: Clean up test users LAST (FK constraints cascade delete)
    if (testUserSetup != null) {
        testUserSetup.cleanup();
    }
}
```

**Why This Works:**
- Foreign key constraints require children (tasks/contacts/appointments) be deleted before parents (users)
- PostgreSQL enforces `ON DELETE CASCADE` strictly during cleanup
- H2 was lenient about this ordering, but correct order works for BOTH databases

## Alternatives Considered

### Alternative 1: Keep `@Testcontainers/@Container` Annotations
**Rejected:** This creates new containers per test class, which fundamentally conflicts with Spring's context caching. Would require disabling context caching entirely, dramatically slowing down tests (each test class would bootstrap Spring from scratch).

### Alternative 2: Disable Testcontainers Reuse
**Rejected:** `.withReuse(false)` still creates new containers per test class, doesn't solve the lifecycle mismatch.

### Alternative 3: Delete Users First in Cleanup
**Rejected:** Violates foreign key constraint ordering. While H2 allowed this, PostgreSQL correctly enforces referential integrity during cleanup.

## Consequences

### Positive
- ✅ Tests complete in ~15 seconds instead of 12+ minutes (48x speedup)
- ✅ Single container instance reduces Docker overhead
- ✅ Aligns container lifecycle with Spring context caching
- ✅ Correct cleanup order works for both H2 and PostgreSQL
- ✅ Exposes constraint violations that H2 was hiding (better test quality)

### Negative
- ⚠️ Container starts once and never stops during test suite (acceptable tradeoff)
- ⚠️ All tests share same Postgres instance (mitigated by `@Transactional` + cleanup in `@BeforeEach`)

### Neutral
- 📝 Must use `PostgresContainerSupport` base class for all Postgres integration tests
- 📝 Developers must understand why static initializer is used instead of annotations

## Implementation Files
- `src/test/java/contactapp/support/PostgresContainerSupport.java` - Single static container pattern
- `src/test/java/contactapp/service/TestCleanupUtility.java` - Correct cleanup order
- `src/test/java/contactapp/support/SecuredMockMvcTest.java` - Extends PostgresContainerSupport

## References
- [Testcontainers Singleton Containers](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers)
- [Spring Boot Test Context Caching](https://docs.spring.io/spring-framework/reference/testing/testcontext-framework/ctx-management/caching.html)
- ADR-0047: Test Isolation Cleanup Utility (introduced `TestCleanupUtility`)
- ADR-0015: Database Choice and Profiles (introduced `integration` profile with Postgres)
