package contactapp;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import contactapp.api.AuthController;
import contactapp.api.dto.LoginRequest;
import contactapp.security.JwtService;
import contactapp.security.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Focused unit tests for {@link AuthController} branches that are hard to hit
 * with the full MVC stack (specifically the lambda used by
 * {@code orElseThrow} inside {@link AuthController#login(LoginRequest)}).
 *
 * <p>The mutation report flagged that the lambda could return {@code null}
 * without any test failing, so this class asserts the controller still throws
 * {@link BadCredentialsException} when the repository somehow fails to return
 * a user after authentication succeeds.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authenticationManager, userRepository, passwordEncoder, jwtService);
    }

    @Test
    void loginThrowsWhenRepositoryReturnsEmptyAfterAuthentication() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("ghost")).thenReturn(java.util.Optional.empty());

        final LoginRequest request = new LoginRequest("ghost", "secret");

        assertThatThrownBy(() -> controller.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }
}
