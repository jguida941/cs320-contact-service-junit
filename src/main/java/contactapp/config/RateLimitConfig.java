package contactapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for API rate limiting.
 *
 * <p>This class binds rate limit settings from application.yml to strongly-typed
 * Java configuration. Rate limits are defined per endpoint pattern to protect
 * against various attack vectors:
 *
 * <ul>
 *   <li>Authentication endpoints (login/register): Low limits per IP to prevent
 *       brute force attacks and account enumeration</li>
 *   <li>API endpoints (/api/v1/**): Higher limits per authenticated user to prevent
 *       DoS while allowing legitimate usage</li>
 * </ul>
 *
 * <h2>Configuration Structure</h2>
 * <pre>
 * rate-limit:
 *   login:
 *     requests: 5
 *     duration-seconds: 60
 *   register:
 *     requests: 3
 *     duration-seconds: 60
 *   api:
 *     requests: 100
 *     duration-seconds: 60
 * </pre>
 *
 * <h2>Design Rationale</h2>
 * <p>Rate limits are tuned based on security best practices:
 * <ul>
 *   <li>Login: 5 attempts/min allows legitimate users with typos while blocking
 *       credential stuffing attacks</li>
 *   <li>Register: 3 attempts/min prevents automated account creation spam</li>
 *   <li>API: 100 req/min supports typical user workflows (CRUD operations) while
 *       preventing resource exhaustion</li>
 * </ul>
 *
 * @see RateLimitingFilter
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private static final int LOGIN_REQUEST_LIMIT = 5;
    private static final int REGISTER_REQUEST_LIMIT = 3;
    private static final int API_REQUEST_LIMIT = 100;
    private static final int DEFAULT_DURATION_SECONDS = 60;

    private EndpointLimit login = new EndpointLimit(LOGIN_REQUEST_LIMIT, DEFAULT_DURATION_SECONDS);
    private EndpointLimit register = new EndpointLimit(REGISTER_REQUEST_LIMIT, DEFAULT_DURATION_SECONDS);
    private EndpointLimit api = new EndpointLimit(API_REQUEST_LIMIT, DEFAULT_DURATION_SECONDS);

    /**
     * Gets the rate limit configuration for login endpoint.
     *
     * @return login rate limit
     */
    public EndpointLimit getLogin() {
        return login;
    }

    /**
     * Sets the rate limit configuration for login endpoint.
     *
     * @param login login rate limit
     */
    public void setLogin(final EndpointLimit login) {
        this.login = login;
    }

    /**
     * Gets the rate limit configuration for register endpoint.
     *
     * @return register rate limit
     */
    public EndpointLimit getRegister() {
        return register;
    }

    /**
     * Sets the rate limit configuration for register endpoint.
     *
     * @param register register rate limit
     */
    public void setRegister(final EndpointLimit register) {
        this.register = register;
    }

    /**
     * Gets the rate limit configuration for API endpoints.
     *
     * @return API rate limit
     */
    public EndpointLimit getApi() {
        return api;
    }

    /**
     * Sets the rate limit configuration for API endpoints.
     *
     * @param api API rate limit
     */
    public void setApi(final EndpointLimit api) {
        this.api = api;
    }

    /**
     * Represents rate limit settings for a specific endpoint or endpoint pattern.
     *
     * <p>Uses token bucket algorithm: each endpoint gets a bucket with a fixed
     * capacity (requests) that refills at a constant rate (capacity/duration).
     * When a request arrives, it consumes one token. If no tokens available,
     * request is rejected with 429 status.
     */
    public static class EndpointLimit {
        private int requests;
        private int durationSeconds;

        public EndpointLimit() {
        }

        public EndpointLimit(final int requests, final int durationSeconds) {
            this.requests = requests;
            this.durationSeconds = durationSeconds;
        }

        /**
         * Gets the maximum number of requests allowed within the duration window.
         *
         * @return maximum requests
         */
        public int getRequests() {
            return requests;
        }

        /**
         * Sets the maximum number of requests allowed within the duration window.
         *
         * @param requests maximum requests
         */
        public void setRequests(final int requests) {
            this.requests = requests;
        }

        /**
         * Gets the duration in seconds for the rate limit window.
         *
         * @return duration in seconds
         */
        public int getDurationSeconds() {
            return durationSeconds;
        }

        /**
         * Sets the duration in seconds for the rate limit window.
         *
         * @param durationSeconds duration in seconds
         */
        public void setDurationSeconds(final int durationSeconds) {
            this.durationSeconds = durationSeconds;
        }
    }
}
