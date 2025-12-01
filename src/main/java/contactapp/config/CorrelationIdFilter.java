package contactapp.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that manages correlation IDs for request tracing.
 *
 * <p>This filter:
 * <ul>
 *   <li>Extracts the X-Correlation-ID header from incoming requests</li>
 *   <li>Generates a new UUID if the header is missing or empty</li>
 *   <li>Sets the correlation ID in the SLF4J MDC for inclusion in all log entries</li>
 *   <li>Adds the correlation ID to the response header for client-side tracking</li>
 *   <li>Clears the MDC after request processing to prevent thread pollution</li>
 * </ul>
 *
 * <p>The correlation ID enables end-to-end tracing across distributed systems
 * and simplifies debugging by linking all log entries for a single request.
 *
 * <p>Order is set to highest precedence (1) to ensure the correlation ID is
 * available for all subsequent filters and processing.
 */
@Component
@Order(CorrelationIdFilter.FILTER_ORDER)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final int FILTER_ORDER = 1;
    /**
     * MDC key for storing the correlation ID.
     */
    public static final String CORRELATION_ID_KEY = "correlationId";

    /**
     * HTTP header name for correlation ID.
     */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    /**
     * Maximum length for correlation IDs (UUID is 36 chars, allow some flexibility).
     */
    private static final int MAX_CORRELATION_ID_LENGTH = 64;

    /**
     * Pattern for valid correlation ID characters (alphanumeric, hyphens, underscores).
     * Prevents log injection attacks via control characters or newlines.
     */
    private static final Pattern VALID_CORRELATION_ID = Pattern.compile("^[a-zA-Z0-9\\-_]+$");

    /**
     * Processes each request to manage correlation IDs.
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

        try {
            // Extract and validate correlation ID from header or generate new one
            String correlationId = sanitizeCorrelationId(request.getHeader(CORRELATION_ID_HEADER));
            if (correlationId == null) {
                correlationId = UUID.randomUUID().toString();
            }

            // Set correlation ID in MDC for logging
            MDC.put(CORRELATION_ID_KEY, correlationId);

            // Add correlation ID to response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue with the request
            filterChain.doFilter(request, response);
        } finally {
            // Remove only our correlation ID to preserve other MDC values from other filters
            MDC.remove(CORRELATION_ID_KEY);
        }
    }

    /**
     * Sanitizes a correlation ID to prevent log injection attacks.
     *
     * <p>Validates that the correlation ID:
     * <ul>
     *   <li>Contains only alphanumeric characters, hyphens, and underscores</li>
     *   <li>Does not exceed the maximum length</li>
     *   <li>Is not empty or blank</li>
     * </ul>
     *
     * <p>If the input fails validation, returns null to trigger UUID generation.
     *
     * @param correlationId the correlation ID from the request header
     * @return the sanitized correlation ID, or null if invalid
     */
    private String sanitizeCorrelationId(final String correlationId) {
        if (correlationId == null || correlationId.trim().isEmpty()) {
            return null;
        }

        final String trimmed = correlationId.trim();

        // Reject if too long (prevents DoS via oversized headers)
        if (trimmed.length() > MAX_CORRELATION_ID_LENGTH) {
            return null;
        }

        // Reject if contains invalid characters (prevents log injection)
        if (!VALID_CORRELATION_ID.matcher(trimmed).matches()) {
            return null;
        }

        return trimmed;
    }
}
