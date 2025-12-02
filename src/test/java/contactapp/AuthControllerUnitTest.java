package contactapp;

import contactapp.api.AuthController;
import contactapp.security.JwtService;
import contactapp.security.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Narrow tests for {@link AuthController} helpers that PIT flagged as uncovered.
 * Integration tests exercise the public endpoints, but we also validate the
 * private cookie extraction logic to prevent regressions when the implementation
 * changes.
 */
class AuthControllerUnitTest {

    private final AuthController controller = new AuthController(
            mock(AuthenticationManager.class),
            mock(UserRepository.class),
            mock(PasswordEncoder.class),
            mock(JwtService.class));

    @Test
    void extractTokenFromCookiesReturnsValueWhenPresent() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("other", "noop"),
                new Cookie(AuthController.AUTH_COOKIE_NAME, "token-123")
        });

        final String token = ReflectionTestUtils.invokeMethod(controller, "extractTokenFromCookies", request);

        assertThat(token).isEqualTo("token-123");
    }

    @Test
    void extractTokenFromCookiesReturnsNullWhenMissing() {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("random", "value")
        });

        final String token = ReflectionTestUtils.invokeMethod(controller, "extractTokenFromCookies", request);

        assertThat(token).isNull();
    }
}
