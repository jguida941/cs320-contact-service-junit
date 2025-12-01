package contactapp.api;

import contactapp.api.dto.AuthResponse;
import contactapp.api.dto.ErrorResponse;
import contactapp.api.dto.LoginRequest;
import contactapp.api.dto.RegisterRequest;
import contactapp.api.exception.DuplicateResourceException;
import contactapp.security.JwtService;
import contactapp.security.Role;
import contactapp.security.User;
import contactapp.security.UserRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations (login and registration).
 *
 * <p>Provides endpoints at {@code /api/auth} for user authentication per ADR-0018.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>POST /api/auth/login - Authenticate user and return JWT token (200 OK)</li>
 *   <li>POST /api/auth/register - Register new user and return JWT token (201 Created)</li>
 *   <li>POST /api/auth/logout - Invalidate session (204 No Content)</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <p>These endpoints are publicly accessible (no JWT required). The returned token
 * should be included in the Authorization header for subsequent API requests:
 * {@code Authorization: Bearer <token>}
 *
 * @see LoginRequest
 * @see RegisterRequest
 * @see AuthResponse
 * @see JwtService
 */
@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "User authentication and registration")
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring-managed singleton services are intentionally stored without copy"
)
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Creates a new AuthController with the required dependencies.
     *
     * @param authenticationManager Spring Security authentication manager
     * @param userRepository repository for user persistence
     * @param passwordEncoder encoder for password hashing (BCrypt)
     * @param jwtService service for JWT token generation
     */
    public AuthController(
            final AuthenticationManager authenticationManager,
            final UserRepository userRepository,
            final PasswordEncoder passwordEncoder,
            final JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request the login credentials
     * @return authentication response with JWT token and user info
     * @throws BadCredentialsException if credentials are invalid
     */
    @Operation(summary = "Authenticate user and get JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuthResponse login(@Valid @RequestBody final LoginRequest request) {
        // Authenticate via Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // Load user and generate token
        final User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        final String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                jwtService.getExpirationTime()
        );
    }

    /**
     * Registers a new user and returns a JWT token.
     *
     * @param request the registration data
     * @return authentication response with JWT token and user info
     * @throws DuplicateResourceException if username or email already exists
     */
    @Operation(summary = "Register new user and get JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registration successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Username or email already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody final RegisterRequest request) {
        // Check for existing username
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException(
                    "Username '" + request.username() + "' is already taken");
        }

        // Check for existing email
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(
                    "Email '" + request.email() + "' is already registered");
        }

        // Create new user with hashed password
        final User user = new User(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER
        );

        // Save with race condition handling - database constraint catches concurrent inserts
        try {
            userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request inserted the same username/email
            throw new DuplicateResourceException("Username or email already exists");
        }

        // Generate token for immediate login
        final String token = jwtService.generateToken(user);

        return new AuthResponse(
                token,
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                jwtService.getExpirationTime()
        );
    }

    /**
     * Logs out the current user.
     *
     * <p>For stateless JWT authentication, this endpoint provides a hook for:
     * <ul>
     *   <li>Future token blacklisting implementation</li>
     *   <li>Audit logging of logout events</li>
     *   <li>Client-side cache invalidation signal</li>
     * </ul>
     *
     * <p>Clients should clear their stored tokens after calling this endpoint.
     */
    @Operation(summary = "Logout current user")
    @ApiResponse(responseCode = "204", description = "Logout successful")
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
        // Stateless JWT - client clears token
        // Future: Add token to blacklist if implementing token revocation
    }
}
