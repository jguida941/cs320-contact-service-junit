package contactapp.config;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility methods for HTTP request processing.
 *
 * <p>Provides shared functionality used across multiple filters to avoid
 * code duplication and ensure consistent behavior.
 */
public final class RequestUtils {

    private RequestUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts the client IP address from the HTTP request.
     *
     * <p>Checks standard proxy headers in order of preference:
     * <ol>
     *   <li>X-Forwarded-For (standard load balancer header)</li>
     *   <li>X-Real-IP (nginx default header)</li>
     *   <li>Remote address (direct connection)</li>
     * </ol>
     *
     * <p><b>Security Note:</b> In production behind a reverse proxy, ensure
     * X-Forwarded-For is set correctly and spoofing is prevented at the
     * proxy level. Configure your proxy to overwrite (not append to) the
     * X-Forwarded-For header to prevent client spoofing.
     *
     * @param request the HTTP request
     * @return the client IP address, never null
     */
    public static String getClientIp(final HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2...)
        // Take the first one which is the original client
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
