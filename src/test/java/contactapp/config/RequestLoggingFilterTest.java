package contactapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests {@link RequestLoggingFilter} so the request/response logging behavior
 * (and {@link org.springframework.web.util.ContentCachingResponseWrapper#copyBodyToResponse()}) stays
 * mutation-proof. Without these assertions, PIT previously reported that calls
 * to {@code logRequest}, {@code logResponse}, and {@code copyBodyToResponse}
 * could be removed without failing tests.
 */
class RequestLoggingFilterTest {

    @Test
    void doFilterInternal_logsRequestAndResponseWhenEnabled() throws ServletException, IOException {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/tasks");
        request.setQueryString("debug=true");
        request.setContent("{\"payload\":true}".getBytes(StandardCharsets.UTF_8));
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(RequestLoggingFilter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        filter.doFilterInternal(request, response, new EchoResponseChain());

        logger.detachAppender(appender);

        assertThat(response.getContentAsString()).isEqualTo("response-body");
        assertThat(appender.list)
                .anyMatch(event -> event.getFormattedMessage().contains("HTTP Request"))
                .anyMatch(event -> event.getFormattedMessage().contains("HTTP Response"));
    }

    /**
     * Ensures IPv4 addresses mask the last octet so PIT cannot change the masking format without detection.
     */
    @Test
    void maskClientIp_masksIpv4LastOctet() {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final String masked = ReflectionTestUtils.invokeMethod(filter, "maskClientIp", "10.20.30.40");
        assertThat(masked).isEqualTo("10.20.30.***");
    }

    /**
     * Ensures IPv6 addresses are never logged raw (should always return "masked").
     */
    @Test
    void maskClientIp_masksIpv6Addresses() {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final String masked = ReflectionTestUtils.invokeMethod(filter, "maskClientIp", "2001:db8::1");
        assertThat(masked).isEqualTo("masked");
    }

    /**
     * Verifies sensitive query parameters are redacted so PIT cannot replace the "***" placeholder with raw values.
     */
    @Test
    void sanitizeQueryString_redactsSensitiveParameters() {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final String sanitized = ReflectionTestUtils.invokeMethod(
                filter,
                "sanitizeQueryString",
                "token=secret&limit=5");
        assertThat(sanitized).isEqualTo("token=***&limit=5");
    }

    /**
     * Verifies null/blank query strings return null, killing the EmptyObjectReturn mutants.
     */
    @Test
    void sanitizeQueryString_returnsNullForBlankInput() {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final Object sanitizedNull = ReflectionTestUtils.invokeMethod(filter, "sanitizeQueryString", (Object) null);
        final Object sanitizedBlank = ReflectionTestUtils.invokeMethod(filter, "sanitizeQueryString", " ");
        assertThat(sanitizedNull).isNull();
        assertThat(sanitizedBlank).isNull();
    }

    @Test
    void sanitizeQueryString_dropsBlankSegments() {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final String sanitized = ReflectionTestUtils.invokeMethod(
                filter,
                "sanitizeQueryString",
                "limit=5&&token=secret&");
        assertThat(sanitized).isEqualTo("limit=5&token=***");
    }

    @Test
    void sanitizeForLogReturnsDefaultWhenValueMissing() {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final String defaultedNull = ReflectionTestUtils.invokeMethod(filter, "sanitizeForLog", null, "fallback");
        final String defaultedBlank = ReflectionTestUtils.invokeMethod(filter, "sanitizeForLog", " \r\n ", "fallback");
        final String sanitized = ReflectionTestUtils.invokeMethod(filter, "sanitizeForLog", "/api/v1/tasks\r\n", "fallback");
        assertThat(defaultedNull).isEqualTo("fallback");
        assertThat(defaultedBlank).isEqualTo("fallback");
        assertThat(sanitized).isEqualTo("/api/v1/tasks");
    }

    @Test
    void sanitizeLogValueStripsControlCharactersAndReturnsNullWhenEmpty() {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final String sanitized = ReflectionTestUtils.invokeMethod(
                filter,
                "sanitizeLogValue",
                "Agent\r\nName");
        final Object empty = ReflectionTestUtils.invokeMethod(
                filter,
                "sanitizeLogValue",
                "\r\n\t ");
        assertThat(sanitized).isEqualTo("AgentName");
        assertThat(empty).isNull();
    }

    /**
     * Ensures sanitizeUserAgent strips control characters so PIT cannot remove the replacement without tests failing.
     */
    @Test
    void sanitizeUserAgent_stripsControlCharacters() {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final String sanitized = ReflectionTestUtils.invokeMethod(
                filter,
                "sanitizeUserAgent",
                "TestAgent\r\nMalicious");
        assertThat(sanitized).isEqualTo("TestAgentMalicious");
    }

    /**
     * Test removed: getSafeLogValue method was refactored out of RequestLoggingFilter.
     * The sanitization logic is now handled differently via getSafeUserAgent.
     */

    @Test
    void getSafeUserAgent_truncatesLongValues() throws Exception {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final int maxLength = (int) ReflectionTestUtils.getField(RequestLoggingFilter.class, "MAX_USER_AGENT_LENGTH");
        final String longUa = "X".repeat(maxLength + 10);

        final String sanitized = ReflectionTestUtils.invokeMethod(filter, "getSafeUserAgent", longUa);
        assertThat(sanitized).endsWith("...");
        assertThat(sanitized.length()).isEqualTo(maxLength + 3);

        final String blank = ReflectionTestUtils.invokeMethod(filter, "getSafeUserAgent", "   ");
        assertThat(blank).isEqualTo("unknown");
    }

    /**
     * Verifies that the query string is included in request logs when present.
     * PIT flagged a surviving mutant where negating the queryString null check
     * went undetected. This test ensures we actually log query strings.
     */
    @Test
    void doFilterInternal_includesQueryStringInLogWhenPresent() throws ServletException, IOException {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tasks");
        request.setQueryString("status=active&limit=10");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(RequestLoggingFilter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        filter.doFilterInternal(request, response, new EchoResponseChain());

        logger.detachAppender(appender);

        assertThat(appender.list)
                .anyMatch(event -> event.getFormattedMessage().contains("status=active"));
    }

    /**
     * Verifies that requests without query strings are logged without errors
     * and that the log format differs from requests with query strings.
     */
    @Test
    void doFilterInternal_logsRequestWithoutQueryString() throws ServletException, IOException {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tasks");
        // No query string set
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(RequestLoggingFilter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        filter.doFilterInternal(request, response, new EchoResponseChain());

        logger.detachAppender(appender);

        // Should log request without query string - verify format matches "METHOD URI from IP"
        assertThat(appender.list)
                .anyMatch(event -> {
                    final String msg = event.getFormattedMessage();
                    return msg.contains("HTTP Request") && msg.contains("GET") && msg.contains("/api/v1/tasks");
                });
    }

    /**
     * Verifies that duration is logged in response. This ensures PIT cannot
     * mutate the duration math without test failure.
     */
    @Test
    void doFilterInternal_logsDurationInResponse() throws ServletException, IOException {
        final RequestLoggingFilter filter = new RequestLoggingFilter(true);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/tasks");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(RequestLoggingFilter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        filter.doFilterInternal(request, response, new EchoResponseChain());

        logger.detachAppender(appender);

        // Verify response log includes duration (should be a number followed by "ms")
        assertThat(appender.list)
                .anyMatch(event -> {
                    final String msg = event.getFormattedMessage();
                    return msg.contains("HTTP Response") && msg.matches(".*\\d+.*ms.*");
                });
    }

    @Test
    void doFilterInternal_skipsLoggingWhenDisabled() throws ServletException, IOException {
        final RequestLoggingFilter filter = new RequestLoggingFilter(false);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(RequestLoggingFilter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        filter.doFilterInternal(request, response, new EchoResponseChain());

        logger.detachAppender(appender);

        assertThat(response.getContentAsString()).isEqualTo("response-body");
        assertThat(appender.list).isEmpty();
    }

    /**
     * Simple filter chain that writes a body so we can verify the caching wrapper
     * copies the buffered content back to the real response.
     */
    private static final class EchoResponseChain implements FilterChain {
        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response)
                throws IOException {
            ((HttpServletResponse) response).getWriter().write("response-body");
        }
    }
}
