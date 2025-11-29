package contactapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying actuator endpoint security configuration.
 *
 * <p>These tests ensure:
 * <ul>
 *   <li>/actuator/health is accessible (required for orchestrator probes)</li>
 *   <li>/actuator/info is accessible (required for operational visibility)</li>
 *   <li>Other actuator endpoints are locked down per security requirements</li>
 * </ul>
 *
 * <p>Security note: Actuator endpoints can expose sensitive information.
 * Only health and info are whitelisted in application.yml. These tests
 * verify that configuration is applied correctly.
 *
 * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html">
 *      Spring Boot Actuator Documentation</a>
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies /actuator/health returns 200 OK with status field.
     *
     * <p>The health endpoint is critical for Kubernetes liveness/readiness
     * probes and load balancer health checks. Must be accessible without
     * authentication for orchestrators to function.
     */
    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    /**
     * Verifies /actuator/info returns 200 OK.
     *
     * <p>The info endpoint provides build metadata and application version
     * for operational dashboards. Returns empty JSON if no info properties
     * are configured, but must still return 200.
     */
    @Test
    void infoEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    /**
     * Verifies /actuator/env is not exposed (returns 404).
     *
     * <p>The env endpoint exposes environment variables and system properties,
     * which may contain secrets. Must be disabled in production. A 404
     * indicates the endpoint is not mapped, as intended.
     */
    @Test
    void envEndpointIsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies /actuator/beans is not exposed (returns 404).
     *
     * <p>The beans endpoint lists all Spring beans and their dependencies,
     * which reveals internal architecture. Disabled to prevent information
     * disclosure per OWASP guidelines.
     */
    @Test
    void beansEndpointIsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isNotFound());
    }

    /**
     * Verifies /actuator/metrics is not exposed (returns 404).
     *
     * <p>The metrics endpoint exposes JVM and application metrics.
     * While useful for monitoring, it can reveal performance characteristics
     * that aid attackers. Disabled by default; enable selectively in Phase 2+
     * with proper authentication.
     */
    @Test
    void metricsEndpointIsNotExposed() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isNotFound());
    }
}
