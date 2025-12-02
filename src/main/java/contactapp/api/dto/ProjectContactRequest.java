package contactapp.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import static contactapp.domain.Validation.MAX_ID_LENGTH;

/**
 * Request DTO for linking a contact to a project.
 *
 * <p>Validates contact ID and optional role description. The project ID is
 * provided via the path parameter in the REST endpoint.
 *
 * <h2>Field Constraints</h2>
 * <ul>
 *   <li>contactId: required, 1-10 characters</li>
 *   <li>role: optional, max 50 characters (e.g., CLIENT, STAKEHOLDER, VENDOR)</li>
 * </ul>
 *
 * @param contactId the contact to link (required)
 * @param role      optional role description
 */
public record ProjectContactRequest(
        @Schema(description = "Contact ID to link to the project")
        @NotBlank(message = "contactId must not be null or blank")
        @Size(min = 1, max = MAX_ID_LENGTH, message = "contactId length must be between {min} and {max}")
        String contactId,

        @Schema(description = "Optional role (e.g., CLIENT, STAKEHOLDER, VENDOR)")
        @Size(max = 50, message = "role length must not exceed {max}")
        String role
) {
}
