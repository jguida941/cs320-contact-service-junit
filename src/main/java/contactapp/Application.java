package contactapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entrypoint for the Contact Service.
 *
 * <p>This class bootstraps the Spring context and starts the embedded Tomcat server.
 * All beans in the {@code contactapp} package and its sub-packages are automatically
 * scanned and registered thanks to {@link SpringBootApplication}.
 *
 * <h2>Why Spring Boot?</h2>
 * <ul>
 *   <li>Provides embedded server (no external Tomcat deployment needed)</li>
 *   <li>Auto-configuration reduces boilerplate for web, validation, and actuator</li>
 *   <li>Production-ready features: health checks, metrics, externalized config</li>
 *   <li>Prepares the codebase for REST API (Phase 2) and persistence (Phase 3)</li>
 * </ul>
 *
 * <h2>Running the Application</h2>
 * <pre>
 *   mvn spring-boot:run              # Development mode with hot reload
 *   java -jar target/*.jar           # Run packaged JAR
 *   java -jar target/*.jar --spring.profiles.active=prod  # Production profile
 * </pre>
 *
 * <h2>Available Endpoints (Phase 1)</h2>
 * <ul>
 *   <li>{@code GET /actuator/health} - Application health status</li>
 *   <li>{@code GET /actuator/info} - Application metadata</li>
 * </ul>
 *
 * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/">Spring Boot Reference</a>
 */
@SpringBootApplication
public class Application {

    /**
     * Application entrypoint.
     *
     * <p>Delegates to {@link SpringApplication#run(Class, String...)} which:
     * <ol>
     *   <li>Creates the Spring application context</li>
     *   <li>Scans for components ({@code @Service}, {@code @Controller}, etc.)</li>
     *   <li>Applies auto-configuration based on classpath dependencies</li>
     *   <li>Starts the embedded web server</li>
     * </ol>
     *
     * @param args command-line arguments (supports Spring Boot properties like --server.port=8081)
     */
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
