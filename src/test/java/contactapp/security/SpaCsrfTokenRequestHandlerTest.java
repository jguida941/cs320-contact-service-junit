package contactapp.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;

/**
 * Unit tests for {@link SpaCsrfTokenRequestHandler}. These cover header-vs-parameter resolution
 * without needing Testcontainers, boosting coverage on the H2-only lanes.
 */
class SpaCsrfTokenRequestHandlerTest {

    private final SpaCsrfTokenRequestHandler handler = new SpaCsrfTokenRequestHandler();

    @Test
    @DisplayName("handle() stores the CsrfToken on the request via the delegate")
    void handleStoresTokenOnRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        HttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-value");

        handler.handle(request, response, () -> token);

        Object stored = request.getAttribute(CsrfToken.class.getName());
        assertThat(stored).isInstanceOf(CsrfToken.class);
        assertThat(((CsrfToken) stored).getToken()).isEqualTo("token-value");
    }

    @Test
    @DisplayName("resolveCsrfTokenValue uses header when present")
    void resolveUsesHeaderWhenPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-value");
        request.addHeader(token.getHeaderName(), "from-header");

        String resolved = handler.resolveCsrfTokenValue(request, token);

        assertThat(resolved).isEqualTo("from-header");
    }

    @Test
    @DisplayName("resolveCsrfTokenValue falls back to request parameter when header is absent")
    void resolveUsesParameterWhenHeaderMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        CsrfToken token = new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token-value");
        request.setParameter(token.getParameterName(), "from-param");

        String resolved = handler.resolveCsrfTokenValue(request, token);

        assertThat(resolved).isEqualTo("from-param");
    }
}
