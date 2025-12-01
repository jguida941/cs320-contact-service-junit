package contactapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import contactapp.config.RateLimitingFilter;
import contactapp.security.JwtAuthenticationFilter;
import jakarta.servlet.Filter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Integration tests focused on {@link contactapp.security.SecurityConfig}.
 *
 * <p>PIT reported surviving mutants around CORS configuration, authentication
 * provider wiring, and filter ordering. These assertions ensure the custom
 * filters remain registered and the CORS defaults stay intact.
 */
@SpringBootTest(properties = "cors.allowed-origins=https://app.example.com,https://admin.example.com")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigIntegrationTest {

    @Autowired
    private FilterChainProxy filterChainProxy;
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private RateLimitingFilter rateLimitingFilter;
    @Autowired
    private CorsConfigurationSource corsConfigurationSource;
    @Autowired
    private AuthenticationProvider authenticationProvider;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void securityFilterChain_includesJwtAndRateLimitFiltersInOrder() {
        final List<Filter> filters = filterChainProxy.getFilters("/api/v1/tasks");

        assertThat(filters).contains(jwtAuthenticationFilter, rateLimitingFilter);
        assertThat(filters.indexOf(jwtAuthenticationFilter))
                .isLessThan(filters.indexOf(rateLimitingFilter));
    }

    @Test
    void corsConfigurationSource_appliesCustomOriginsAndHeaders() {
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/tasks");

        final CorsConfiguration configuration = corsConfigurationSource.getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins())
                .containsExactly("https://app.example.com", "https://admin.example.com");
        assertThat(configuration.getAllowedMethods())
                .containsExactlyInAnyOrder("GET", "POST", "PUT", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedHeaders())
                .containsExactlyInAnyOrder("Authorization", "Content-Type", "X-Requested-With");
        assertThat(configuration.getExposedHeaders()).contains("Authorization");
        assertThat(configuration.getMaxAge()).isEqualTo(3_600L);
        assertThat(configuration.getAllowCredentials()).isTrue();
    }

    @Test
    void authenticationProvider_usesBcryptEncoder() {
        final Object encoder = ReflectionTestUtils.getField(authenticationProvider, "passwordEncoder");
        assertThat(encoder).isNotNull();
        assertThat(encoder.getClass().getSimpleName()).contains("BCrypt");
    }

    @Test
    void spaRoutesAreAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/contacts"))
                .andExpect(status().isNotFound());
    }
}
