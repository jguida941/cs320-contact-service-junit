package contactapp.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService} so PIT covers token generation/validation logic.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String rawSecret = "test-secret-key-that-is-long-enough";
        String base64 = Base64.getEncoder().encodeToString(rawSecret.getBytes(StandardCharsets.UTF_8));
        ReflectionTestUtils.setField(jwtService, "secretKey", base64);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600_000L);
        userDetails = User.withUsername("tester")
                .password("ignored")
                .roles("USER")
                .build();
    }

    @Test
    void generateTokenContainsUsernameClaim() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("tester");
    }

    @Test
    void isTokenValidReturnsTrueForValidToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    void isTokenValidReturnsFalseForDifferentUser() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = User.withUsername("other").password("ignored").roles("USER").build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void extractUsernameThrowsWhenTokenExpired() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1000L);
        String token = jwtService.generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.extractUsername(token))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void extractClaimReturnsCustomClaim() {
        String token = jwtService.generateToken(Map.of("role", "ADMIN"), userDetails);

        Object role = jwtService.extractClaim(token, claims -> claims.get("role"));
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void fallbackToUtf8SecretWhenBase64DecodingFails() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", "plain-secret-key-that-is-long-enough-1234567890");
        ReflectionTestUtils.setField(service, "jwtExpiration", 1000L);

        String token = service.generateToken(userDetails);

        assertThat(service.extractUsername(token)).isEqualTo("tester");
    }

    /**
     * Ensures {@code isTokenExpired} returns false for freshly minted tokens so PIT cannot flip the boolean.
     */
    @Test
    void isTokenExpiredReturnsFalseForFreshToken() {
        String token = jwtService.generateToken(userDetails);

        boolean expired = ReflectionTestUtils.invokeMethod(jwtService, "isTokenExpired", token);

        assertThat(expired).isFalse();
    }
}
