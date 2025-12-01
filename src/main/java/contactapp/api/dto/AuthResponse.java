package contactapp.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for authentication operations (login/register).
 *
 * <p>Contains the JWT token and user information needed by the SPA
 * for subsequent authenticated requests and role-based UI rendering.
 *
 * <h2>Usage</h2>
 * <ul>
 *   <li>Return tokens via secure, HTTP-only cookies (with SameSite protection)
 *       instead of exposing them to localStorage/sessionStorage.</li>
 *   <li>Pair cookie-based storage with CSRF protections and refresh-token rotation
 *       or short-lived access tokens.</li>
 *   <li>Use the role and username fields to tailor the UI (admin nav, profile widgets)
 *       without duplicating authorization logic.</li>
 *   <li>Coordinate storage decisions with the security team and OWASP guidance before
 *       changing how this payload is persisted client-side.</li>
 * </ul>
 *
 * @param token JWT access token for Authorization header
 * @param username the authenticated user's username
 * @param email the authenticated user's email
 * @param role the user's role (USER or ADMIN)
 * @param expiresIn token expiration time in milliseconds
 */
@Schema(description = "Authentication response with JWT token and user info")
public record AuthResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String token,

        @Schema(description = "Username", example = "johndoe")
        String username,

        @Schema(description = "Email address", example = "john.doe@example.com")
        String email,

        @Schema(description = "User role", example = "USER")
        String role,

        @Schema(description = "Token expiration time in milliseconds", example = "86400000")
        long expiresIn
) {
}
