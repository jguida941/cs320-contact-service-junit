package contactapp.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared Testcontainers Postgres support for Spring Boot tests.
 *
 * <p>This creates a SINGLE container instance shared across ALL test classes in the JVM.
 * The container starts once and remains active throughout the entire test suite execution.
 * This approach aligns with Spring's test context caching, preventing connection pool
 * issues when the same @SpringBootTest configuration is reused across multiple test classes.
 *
 * <p>Removing @Testcontainers/@Container annotations ensures the container lifecycle
 * matches Spring context lifecycle, avoiding port conflicts from container recreation.
 */
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
