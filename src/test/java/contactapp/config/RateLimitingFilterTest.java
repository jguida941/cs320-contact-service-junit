package contactapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.EstimationProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link RateLimitingFilter}.
 *
 * <p>Tests verify:
 * <ul>
 *   <li>Rate limits are enforced per IP for auth endpoints</li>
 *   <li>Rate limits are enforced per user for API endpoints</li>
 *   <li>429 response is returned when limit exceeded</li>
 *   <li>Retry-After header is set correctly</li>
 *   <li>Non-rate-limited paths pass through</li>
 * </ul>
 */
class RateLimitingFilterTest {

    private static final int TOO_MANY_REQUESTS = 429;
    private RateLimitingFilter filter;
    private RateLimitConfig config;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private StringWriter responseWriter;
    private Logger rateLimitLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() throws IOException {
        // Create configuration with test-friendly limits
        config = new RateLimitConfig();
        config.setLogin(new RateLimitConfig.EndpointLimit(2, 60));  // 2 requests per minute
        config.setRegister(new RateLimitConfig.EndpointLimit(2, 60));
        config.setApi(new RateLimitConfig.EndpointLimit(3, 60));  // 3 requests per minute

        filter = new RateLimitingFilter(config);

        // Mock HTTP components
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);

        // Setup response writer to capture JSON output
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        // Clear security context
        SecurityContextHolder.clearContext();

        rateLimitLogger = (Logger) LoggerFactory.getLogger(RateLimitingFilter.class);
    }

    @AfterEach
    void tearDownLogger() {
        if (logAppender != null) {
            rateLimitLogger.detachAppender(logAppender);
            logAppender.stop();
            rateLimitLogger.setLevel(null);
        }
    }

    @Test
    void testLoginEndpoint_allowsRequestsWithinLimit() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - make 2 requests (within limit)
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(2)).doFilter(request, response);
        verify(response, never()).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testLoginEndpoint_blocksRequestsExceedingLimit() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - make 3 requests (exceeds limit of 2)
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(2)).doFilter(request, response);
        verify(response).setStatus(TOO_MANY_REQUESTS);
        verify(response).setHeader(anyString(), anyString());  // Retry-After header
        verify(response).setContentType("application/json");

        final String jsonResponse = responseWriter.toString();
        assertThat(jsonResponse).contains("Rate limit exceeded");
        assertThat(jsonResponse).contains("retryAfter");
    }

    @Test
    void testLoginEndpoint_separateLimitsPerIp() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        // Act - IP 1 makes 2 requests
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Act - IP 2 makes 2 requests
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert - all requests should succeed (different IPs)
        verify(filterChain, times(4)).doFilter(request, response);
        verify(response, never()).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testRegisterEndpoint_enforcesRateLimit() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - make 3 requests (exceeds limit of 2)
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(2)).doFilter(request, response);
        verify(response).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testApiEndpoint_enforcesRateLimitPerUser() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/contacts");
        mockAuthenticatedUser("user1");

        // Act - make 4 requests (exceeds limit of 3)
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain, times(3)).doFilter(request, response);
        verify(response).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testApiEndpoint_separateLimitsPerUser() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/contacts");

        // Act - user1 makes 3 requests
        mockAuthenticatedUser("user1");
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Act - user2 makes 3 requests
        mockAuthenticatedUser("user2");
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert - all requests should succeed (different users)
        verify(filterChain, times(6)).doFilter(request, response);
        verify(response, never()).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testApiEndpoint_passesThoughWhenNotAuthenticated() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/v1/contacts");
        SecurityContextHolder.clearContext();  // No authentication

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert - should pass through without rate limiting
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testNonRateLimitedPath_alwaysPassesThrough() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // Act - make many requests to non-rate-limited path
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        // Assert - all should pass through
        verify(filterChain, times(10)).doFilter(request, response);
        verify(response, never()).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testXForwardedForHeader_usedForIpExtraction() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - make 3 requests (exceeds limit)
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert - should use X-Forwarded-For IP for rate limiting
        verify(response).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testXForwardedForHeader_handlesMultipleIps() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 192.168.1.1, 10.0.0.1");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - make requests
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert - should use first IP in X-Forwarded-For
        verify(filterChain, times(2)).doFilter(request, response);
    }

    @Test
    void testRetryAfterHeader_setCorrectly() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - exceed rate limit
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert - Retry-After header should be set
        verify(response).setHeader(anyString(), anyString());
        verify(response).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void testClearBuckets_resetsAllLimits() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - exhaust rate limit
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Clear buckets and verify cleared state
        filter.clearBuckets();
        assertThat(filter.getIpBucketCount()).isZero();
        assertThat(filter.getUserBucketCount()).isZero();

        // Make more requests after clearing - bucket count will increase
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert - should allow requests again after clearing (4 total passed)
        verify(filterChain, times(4)).doFilter(request, response);
    }

    @Test
    void testBucketCounting() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        // Act - create buckets for different IPs
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        filter.doFilterInternal(request, response, filterChain);

        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        assertThat(filter.getIpBucketCount()).isEqualTo(2);
        assertThat(filter.getUserBucketCount()).isZero();

        // Add authenticated user bucket
        when(request.getRequestURI()).thenReturn("/api/v1/contacts");
        mockAuthenticatedUser("testuser");
        filter.doFilterInternal(request, response, filterChain);

        assertThat(filter.getUserBucketCount()).isEqualTo(1);
    }

    @Test
    void testMultipleEndpointPaths_independentLimits() throws ServletException, IOException {
        // Arrange
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - exhaust login limit
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Try register endpoint with same IP
        when(request.getRequestURI()).thenReturn("/api/auth/register");
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert - both endpoints should have independent limits
        verify(filterChain, times(4)).doFilter(request, response);
    }

    @Test
    void testJsonResponseFormat() throws ServletException, IOException {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Act - exceed limit
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        // Assert - verify JSON structure
        final String json = responseWriter.toString();
        assertThat(json).contains("\"error\":");
        assertThat(json).contains("\"retryAfter\":");
        assertThat(json).contains("Rate limit exceeded");
        verify(response).setContentType("application/json");
        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    void calculateWaitTimeUsesProbeEstimate() throws Exception {
        final Bucket bucket = mock(Bucket.class);
        final EstimationProbe probe = EstimationProbe.canNotBeConsumed(0L, Duration.ofSeconds(3).toNanos());
        when(bucket.estimateAbilityToConsume(1)).thenReturn(probe);

        final long waitSeconds = invokeCalculateWaitTime(bucket);

        assertThat(waitSeconds).isEqualTo(4); // 3 seconds rounded up + safety padding
    }

    /**
     * Integration-style check that the real calculateWaitTime implementation never returns zero when a bucket is empty.
     */
    @Test
    void calculateWaitTime_returnsAtLeastOneSecond() {
        final Bandwidth bandwidth = Bandwidth.builder()
                .capacity(1)
                .refillGreedy(1, Duration.ofSeconds(2))
                .build();
        final Bucket bucket = Bucket.builder()
                .addLimit(bandwidth)
                .build();
        bucket.tryConsume(1);

        final long waitSeconds = ReflectionTestUtils.invokeMethod(filter, "calculateWaitTime", bucket);

        assertThat(waitSeconds).isGreaterThanOrEqualTo(1L);
    }

    /**
     * Verifies clearBuckets fully resets the internal caches so the next request passes through instead of
     * continuing to return 429.
     */
    @Test
    void clearBuckets_resetsBucketsAndAllowsNewRequests() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("198.51.100.10");

        // Exhaust the bucket (limit 2) to force a rejection
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        verify(response).setStatus(TOO_MANY_REQUESTS);

        filter.clearBuckets();
        assertThat(filter.getIpBucketCount()).isZero();

        // Reset mocks for the follow-up request
        reset(response, filterChain);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(TOO_MANY_REQUESTS);
    }

    @Test
    void calculateWaitTimeReturnsOneWhenTokensAvailable() throws Exception {
        final Bucket bucket = mock(Bucket.class);
        final EstimationProbe probe = EstimationProbe.canBeConsumed(1L);
        when(bucket.estimateAbilityToConsume(1)).thenReturn(probe);

        assertThat(invokeCalculateWaitTime(bucket)).isEqualTo(1);
    }

    @Test
    void getSafeLogValue_rejectsUnsafeCharacters() throws Exception {
        final Method method = RateLimitingFilter.class.getDeclaredMethod("getSafeLogValue", String.class);
        method.setAccessible(true);

        final String sanitized = (String) method.invoke(filter, "user\r\nname");

        assertThat(sanitized).isEqualTo("[unsafe-value]");
    }

    @Test
    void getSafeLogValue_handlesBoundaries() throws Exception {
        final Method method = RateLimitingFilter.class.getDeclaredMethod("getSafeLogValue", String.class);
        method.setAccessible(true);
        final Field field = RateLimitingFilter.class.getDeclaredField("MAX_LOG_LENGTH");
        field.setAccessible(true);
        final int maxLength = field.getInt(null);

        assertThat((String) method.invoke(filter, (Object) null)).isEqualTo("[null]");
        assertThat((String) method.invoke(filter, "   ")).isEqualTo("[empty]");

        final String exact = "a".repeat(maxLength);
        assertThat((String) method.invoke(filter, exact)).isEqualTo(exact);

        final String longer = "b".repeat(maxLength + 5);
        assertThat((String) method.invoke(filter, longer))
                .isEqualTo("b".repeat(maxLength) + "...");
    }

    @Test
    void calculateWaitTimeShortCircuitsWhenTokenAvailable() throws Exception {
        final Bucket bucket = mock(Bucket.class);
        final EstimationProbe probe = mock(EstimationProbe.class);
        when(bucket.estimateAbilityToConsume(1)).thenReturn(probe);
        when(probe.getRemainingTokens()).thenReturn(1L);
        when(probe.getNanosToWaitForRefill()).thenReturn(1_000_000_000L);

        final long waitSeconds = invokeCalculateWaitTime(bucket);

        assertThat(waitSeconds).isEqualTo(1);
        verify(probe, never()).getNanosToWaitForRefill();
    }

    @Test
    void doFilterInternal_logsSanitizedClientIp() throws ServletException, IOException {
        attachLogAppender(Level.DEBUG);
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1\r\n10.0.0.2");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(logAppender.list)
                .anyMatch(event -> event.getFormattedMessage().contains("[unsafe-value]"));
    }

    @Test
    void doFilterInternal_logsSanitizedPathWhenRateLimitExceeded() throws ServletException, IOException {
        attachLogAppender(Level.WARN);
        // Use unsafe char that survives trim() - ! is not in SAFE_LOG_PATTERN
        when(request.getRequestURI()).thenReturn("/api/auth/login!");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        // Exceed limit
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);
        filter.doFilterInternal(request, response, filterChain);

        assertThat(logAppender.list)
                .anyMatch(event -> event.getFormattedMessage().contains("[unsafe-value]"));
    }

    /**
     * Invokes the private {@code calculateWaitTime} helper via reflection to
     * keep PIT from mutating the Retry-After math unchecked.
     */
    private long invokeCalculateWaitTime(final Bucket bucket) throws Exception {
        final Method method = RateLimitingFilter.class.getDeclaredMethod("calculateWaitTime", Bucket.class);
        method.setAccessible(true);
        return (long) method.invoke(filter, bucket);
    }

    /**
     * Mocks an authenticated user in the security context.
     *
     * @param username the username to mock
     */
    private void mockAuthenticatedUser(final String username) {
        final Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(username);

        final SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    private void attachLogAppender(final Level level) {
        logAppender = new ListAppender<>();
        logAppender.start();
        rateLimitLogger.setLevel(level);
        rateLimitLogger.addAppender(logAppender);
    }
}
