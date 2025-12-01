package contactapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Filter that logs HTTP requests and responses for audit trail purposes.
 *
 * <p>This filter provides comprehensive request/response logging including:
 * <ul>
 *   <li>HTTP method and URI</li>
 *   <li>Request query parameters</li>
 *   <li>Response status code</li>
 *   <li>Request processing duration</li>
 *   <li>Client IP address</li>
 *   <li>User agent (optional)</li>
 * </ul>
 *
 * <p>Logging can be enabled/disabled via the {@code logging.request.enabled}
 * property in application.yml. This allows fine-grained control per environment:
 * disabled in production for performance, enabled in dev/staging for debugging.
 *
 * <p>The filter uses content caching wrappers to allow request/response body
 * inspection without consuming the streams.
 *
 * <p>Order is set to 10 to run after the CorrelationIdFilter (order 1) so
 * correlation IDs are already in the MDC.
 */
@Component
@Order(RequestLoggingFilter.REQUEST_LOGGING_FILTER_ORDER)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final int REQUEST_LOGGING_FILTER_ORDER = 10;

    private static final Set<String> SENSITIVE_QUERY_KEYS = Set.of(
            "token", "access_token", "refresh_token",
            "password", "pass", "pwd",
            "api_key", "apikey", "sessionid", "session_id",
            "secret", "auth");
    private static final int QUERY_VALUE_LIMIT = 2;
    private static final int IPV4_OCTET_COUNT = 4;
    private static final int IPV4_FIRST_OCTET_INDEX = 0;
    private static final int IPV4_SECOND_OCTET_INDEX = 1;
    private static final int IPV4_THIRD_OCTET_INDEX = 2;

    /**
     * Whether request/response logging is enabled.
     */
    private final boolean enabled;

    /**
     * Creates a new RequestLoggingFilter.
     *
     * @param enabled whether logging is enabled (from logging.request.enabled property)
     */
    public RequestLoggingFilter(
            @Value("${logging.request.enabled:false}") final boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Processes each request to log request/response details.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if an error occurs during filtering
     * @throws IOException if an I/O error occurs during filtering
     */
    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {

        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        // Wrap request and response to allow body reading
        final ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request);
        final ContentCachingResponseWrapper responseWrapper =
                new ContentCachingResponseWrapper(response);

        final long startTime = System.currentTimeMillis();

        try {
            // Log incoming request
            logRequest(requestWrapper);

            // Process request
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            final long duration = System.currentTimeMillis() - startTime;

            // Log response
            logResponse(responseWrapper, duration);

            // Copy cached response body to actual response
            responseWrapper.copyBodyToResponse();
        }
    }

    /**
     * Logs details about the incoming HTTP request.
     *
     * @param request the HTTP request wrapper
     */
    private void logRequest(final ContentCachingRequestWrapper request) {
        final String method = request.getMethod();
        final String uri = request.getRequestURI();
        final String queryString = sanitizeQueryString(request.getQueryString());
        final String clientIp = maskClientIp(RequestUtils.getClientIp(request));
        final String userAgent = sanitizeUserAgent(request.getHeader("User-Agent"));

        if (queryString != null && !queryString.isBlank()) {
            logger.info("HTTP Request: method={} uri={} query={} clientIp={} userAgent={}",
                    method, uri, queryString, clientIp, userAgent);
        } else {
            logger.info("HTTP Request: method={} uri={} clientIp={} userAgent={}",
                    method, uri, clientIp, userAgent);
        }
    }

    /**
     * Logs details about the HTTP response.
     *
     * @param response the HTTP response wrapper
     * @param duration request processing duration in milliseconds
     */
    private void logResponse(
            final ContentCachingResponseWrapper response,
            final long duration) {
        final int status = response.getStatus();
        logger.info("HTTP Response: status={} duration={}ms", status, duration);
    }

    private String sanitizeQueryString(final String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        return Arrays.stream(rawQuery.split("&"))
                .filter(part -> !part.isBlank())
                .map(this::sanitizeQueryParameter)
                .collect(Collectors.joining("&"));
    }

    private String sanitizeQueryParameter(final String parameter) {
        final String[] parts = parameter.split("=", QUERY_VALUE_LIMIT);
        final String key = parts[0];
        final String lowerKey = key.toLowerCase(Locale.ROOT);
        if (SENSITIVE_QUERY_KEYS.contains(lowerKey)) {
            return key + "=***";
        }
        if (parts.length == QUERY_VALUE_LIMIT) {
            return key + "=" + parts[1];
        }
        return key;
    }

    private String maskClientIp(final String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }
        if (clientIp.contains(":")) {
            // IPv6 or other format – avoid logging raw value
            return "masked";
        }
        final String[] octets = clientIp.split("\\.");
        if (octets.length == IPV4_OCTET_COUNT) {
            return String.format("%s.%s.%s.***",
                    octets[IPV4_FIRST_OCTET_INDEX],
                    octets[IPV4_SECOND_OCTET_INDEX],
                    octets[IPV4_THIRD_OCTET_INDEX]);
        }
        return "masked";
    }

    private String sanitizeUserAgent(final String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        // Remove control characters to avoid log injection
        return userAgent.replaceAll("[\\r\\n]", "").trim();
    }
}
