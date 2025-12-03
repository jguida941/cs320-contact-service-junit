package contactapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that eagerly loads the CSRF token on every request.
 *
 * <p>Spring Security 6+ defers CSRF token loading by default - the {@code XSRF-TOKEN}
 * cookie is only set when a state-changing request (POST, PUT, DELETE) is made.
 * This breaks SPAs that need to read the token from the cookie before making
 * their first state-changing request.
 *
 * <p>This filter forces the deferred token to be loaded on every request by
 * calling {@link CsrfToken#getToken()}, which triggers the
 * {@link org.springframework.security.web.csrf.CookieCsrfTokenRepository CookieCsrfTokenRepository}
 * to set the cookie.
 *
 * @see <a href="https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html">
 *      Spring Security CSRF Documentation</a>
 * @see <a href="https://stackoverflow.com/questions/74447118/csrf-protection-not-working-with-spring-security-6">
 *      Stack Overflow: CSRF protection not working with Spring Security 6</a>
 */
public final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final FilterChain filterChain) throws ServletException, IOException {
        // Force the deferred CSRF token to be loaded, which causes the cookie to be set
        final CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Calling getToken() triggers the deferred loading and cookie setting
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
