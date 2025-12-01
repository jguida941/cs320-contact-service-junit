package contactapp;

import contactapp.config.RateLimitingFilter;
import contactapp.security.Role;
import contactapp.security.User;
import contactapp.security.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link contactapp.api.AuthController}.
 *
 * <p>Tests REST endpoints for authentication operations including:
 * <ul>
 *   <li>Happy path for login and registration</li>
 *   <li>Validation errors (Bean Validation layer)</li>
 *   <li>Invalid credentials (401)</li>
 *   <li>Duplicate username/email conflicts (409)</li>
 * </ul>
 *
 * @see contactapp.api.AuthController
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // Clear rate limit buckets between tests to prevent 429 collisions
        rateLimitingFilter.clearBuckets();
    }

    // ==================== Registration Happy Path Tests ====================

    @Test
    void register_validRequest_returns201WithToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "newuser",
                                "email": "newuser@example.com",
                                "password": "securePassword123"
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.expiresIn").value(greaterThan(0)));
    }

    @Test
    void register_validRequest_userPersistedInDatabase() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "persisteduser",
                                "email": "persisted@example.com",
                                "password": "securePassword123"
                            }
                            """))
                .andExpect(status().isCreated());

        // Verify user was persisted
        var user = userRepository.findByUsername("persisteduser");
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo("persisted@example.com");
        assertThat(user.get().getRole()).isEqualTo(Role.USER);
    }

    // ==================== Login Happy Path Tests ====================

    @Test
    void login_validCredentials_returns200WithToken() throws Exception {
        // Create a user first
        createTestUser("loginuser", "login@example.com", "correctPassword123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "loginuser",
                                "password": "correctPassword123"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(notNullValue()))
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.expiresIn").value(greaterThan(0)));
    }

    @Test
    void login_adminUser_returnsAdminRole() throws Exception {
        createTestUser("adminuser", "admin@example.com", "adminPassword123", Role.ADMIN);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "adminuser",
                                "password": "adminPassword123"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ==================== Login Failure Tests ====================

    @Test
    void login_wrongPassword_returns401() throws Exception {
        createTestUser("testuser", "test@example.com", "correctPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "testuser",
                                "password": "wrongPassword"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_nonexistentUser_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "doesnotexist",
                                "password": "anyPassword123"
                            }
                            """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    // ==================== Registration Conflict Tests ====================

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        createTestUser("existinguser", "existing@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "existinguser",
                                "email": "different@example.com",
                                "password": "password123456"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'existinguser' is already taken"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        createTestUser("user1", "duplicate@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "differentuser",
                                "email": "duplicate@example.com",
                                "password": "password123456"
                            }
                            """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email 'duplicate@example.com' is already registered"));
    }

    // ==================== Validation Error Tests (Registration) ====================

    @ParameterizedTest(name = "register with invalid {0}")
    @MethodSource("invalidRegisterInputs")
    @SuppressWarnings("java:S1172") // fieldName used in @ParameterizedTest name for test output
    void register_invalidInput_returns400(
            final String fieldName,
            final String jsonBody,
            final String expectedMessageContains) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString(expectedMessageContains)));
    }

    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                // Username validation
                Arguments.of("username (blank)", """
                    {"username": "", "email": "test@example.com", "password": "password123"}
                    """, "username"),
                Arguments.of("username (too long)", """
                    {"username": "a".repeat(51), "email": "test@example.com", "password": "password123"}
                    """.replace("\"a\".repeat(51)", "\"" + "a".repeat(51) + "\""), "username"),

                // Email validation
                Arguments.of("email (blank)", """
                    {"username": "testuser", "email": "", "password": "password123"}
                    """, "email"),
                Arguments.of("email (invalid format)", """
                    {"username": "testuser", "email": "notanemail", "password": "password123"}
                    """, "email"),

                // Password validation
                Arguments.of("password (blank)", """
                    {"username": "testuser", "email": "test@example.com", "password": ""}
                    """, "password"),
                Arguments.of("password (too short)", """
                    {"username": "testuser", "email": "test@example.com", "password": "short"}
                    """, "password")
        );
    }

    // ==================== Validation Error Tests (Login) ====================

    @ParameterizedTest(name = "login with invalid {0}")
    @MethodSource("invalidLoginInputs")
    @SuppressWarnings("java:S1172") // fieldName used in @ParameterizedTest name for test output
    void login_invalidInput_returns400(
            final String fieldName,
            final String jsonBody,
            final String expectedMessageContains) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString(expectedMessageContains)));
    }

    static Stream<Arguments> invalidLoginInputs() {
        return Stream.of(
                Arguments.of("username (blank)", """
                    {"username": "", "password": "password123"}
                    """, "username"),
                Arguments.of("password (blank)", """
                    {"username": "testuser", "password": ""}
                    """, "password")
        );
    }

    // ==================== Malformed JSON Tests ====================

    @Test
    void register_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid JSON in request body"));
    }

    @Test
    void login_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid JSON in request body"));
    }

    // ==================== Boundary Tests ====================

    @Test
    void register_usernameAtMaxLength_accepted() throws Exception {
        final String maxUsername = "a".repeat(50);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("""
                            {
                                "username": "%s",
                                "email": "maxlen@example.com",
                                "password": "password123456"
                            }
                            """, maxUsername)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(maxUsername));
    }

    @Test
    void register_passwordAtMinLength_accepted() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "username": "minpwuser",
                                "email": "minpw@example.com",
                                "password": "12345678"
                            }
                            """))
                .andExpect(status().isCreated());
    }

    // ==================== Helper Methods ====================

    private void createTestUser(
            final String username,
            final String email,
            final String rawPassword) {
        createTestUser(username, email, rawPassword, Role.USER);
    }

    private void createTestUser(
            final String username,
            final String email,
            final String rawPassword,
            final Role role) {
        final String encodedPassword = passwordEncoder.encode(rawPassword);
        final User user = new User(username, email, encodedPassword, role);
        userRepository.save(user);
    }
}
