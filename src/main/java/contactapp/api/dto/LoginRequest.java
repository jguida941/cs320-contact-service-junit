package contactapp.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import static contactapp.domain.Validation.MAX_PASSWORD_LENGTH;
import static contactapp.domain.Validation.MAX_USERNAME_LENGTH;

/**
 * Request DTO for user login.
 *
 * <p>Uses Bean Validation annotations for API-layer validation.
 * The authentication service handles credential verification.
 *
 * <h2>Field Constraints</h2>
 * <ul>
 *   <li>username: required, 1-50 characters</li>
 *   <li>password: required, 1-255 characters (raw password, not hash)</li>
 * </ul>
 *
 * @param username the username
 * @param password the raw password (will be verified against stored hash)
 */
@Schema(description = "Login credentials")
public record LoginRequest(
        @Schema(description = "Username", example = "johndoe")
        @NotBlank(message = "username must not be null or blank")
        @Size(min = 1, max = MAX_USERNAME_LENGTH, message = "username length must be between {min} and {max}")
        String username,

        @Schema(description = "Password", example = "securePassword123")
        @NotBlank(message = "password must not be null or blank")
        @Size(min = 1, max = MAX_PASSWORD_LENGTH, message = "password length must be between {min} and {max}")
        String password
) {
}
