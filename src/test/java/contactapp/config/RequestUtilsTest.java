package contactapp.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RequestUtils} that cover each header precedence branch.
 *
 * <p>Mutation testing previously highlighted these branches as NO_COVERAGE, so
 * these assertions guarantee we keep exercising the X-Forwarded-For, X-Real-IP
 * and remote address fallbacks.
 */
class RequestUtilsTest {

    @Test
    void getClientIp_prefersFirstXForwardedForEntry() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.1, 198.51.100.77");
        request.setRemoteAddr("10.0.0.1");

        assertThat(clientIp(request)).isEqualTo("203.0.113.1");
    }

    @Test
    void getClientIp_usesXRealIpWhenForwardedForMissing() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "198.51.100.5");
        request.setRemoteAddr("10.0.0.1");

        assertThat(clientIp(request)).isEqualTo("198.51.100.5");
    }

    @Test
    void getClientIp_fallsBackToRemoteAddress() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.9");

        assertThat(clientIp(request)).isEqualTo("10.0.0.9");
    }

    @Test
    void getClientIp_returnsUnknownWhenNoSourcesPresent() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(null);

        assertThat(clientIp(request)).isEqualTo("unknown");
    }

    @Test
    void getClientIp_ignoresUnknownHeaderValues() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "unknown");
        request.setRemoteAddr("192.0.2.9");

        assertThat(clientIp(request)).isEqualTo("192.0.2.9");
    }

    private String clientIp(final HttpServletRequest request) {
        return RequestUtils.getClientIp(request);
    }
}
