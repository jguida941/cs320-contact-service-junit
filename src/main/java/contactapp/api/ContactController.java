package contactapp.api;

import contactapp.api.dto.ContactRequest;
import contactapp.api.dto.ContactResponse;
import contactapp.api.dto.ErrorResponse;
import contactapp.api.exception.DuplicateResourceException;
import contactapp.api.exception.ResourceNotFoundException;
import contactapp.domain.Contact;
import contactapp.service.ContactService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import static contactapp.domain.Validation.MAX_ID_LENGTH;

/**
 * REST controller for Contact CRUD operations.
 *
 * <p>Provides endpoints at {@code /api/v1/contacts} per ADR-0016.
 *
 * <h2>Endpoints</h2>
 * <ul>
 *   <li>POST /api/v1/contacts - Create a new contact (201 Created)</li>
 *   <li>GET /api/v1/contacts - List all contacts (200 OK)</li>
 *   <li>GET /api/v1/contacts/{id} - Get contact by ID (200 OK / 404 Not Found)</li>
 *   <li>PUT /api/v1/contacts/{id} - Update contact (200 OK / 404 Not Found)</li>
 *   <li>DELETE /api/v1/contacts/{id} - Delete contact (204 No Content / 404 Not Found)</li>
 * </ul>
 *
 * <h2>Validation</h2>
 * <p>Uses two layers of validation:
 * <ol>
 *   <li>Bean Validation on request DTOs ({@code @Valid})</li>
 *   <li>Domain validation via {@link contactapp.domain.Validation} in Contact constructor</li>
 * </ol>
 *
 * @see ContactRequest
 * @see ContactResponse
 * @see ContactService
 */
@RestController
@RequestMapping(value = "/api/v1/contacts", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Contacts", description = "Contact CRUD operations")
@Validated
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring-managed singleton service is intentionally stored without copy"
)
public class ContactController {

    private final ContactService contactService;

    /**
     * Creates a new ContactController with the given service.
     *
     * @param contactService the service for contact operations
     */
    public ContactController(final ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Creates a new contact.
     *
     * @param request the contact data
     * @return the created contact
     * @throws DuplicateResourceException if a contact with the given ID already exists
     */
    @Operation(summary = "Create a new contact")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contact created",
                    content = @Content(schema = @Schema(implementation = ContactResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Contact with this ID already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse create(@Valid @RequestBody final ContactRequest request) {
        // Domain constructor validates via Validation.java
        final Contact contact = new Contact(
                request.id(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.address()
        );

        if (!contactService.addContact(contact)) {
            throw new DuplicateResourceException(
                    "Contact with id '" + request.id() + "' already exists");
        }

        return ContactResponse.from(contact);
    }

    /**
     * Returns all contacts.
     *
     * @return list of all contacts
     */
    @Operation(summary = "Get all contacts")
    @ApiResponse(responseCode = "200", description = "List of contacts")
    @GetMapping
    public List<ContactResponse> getAll() {
        return contactService.getAllContacts().stream()
                .map(ContactResponse::from)
                .toList();
    }

    /**
     * Returns a contact by ID.
     *
     * @param id the contact ID
     * @return the contact
     * @throws ResourceNotFoundException if no contact with the given ID exists
     */
    @Operation(summary = "Get contact by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contact found",
                    content = @Content(schema = @Schema(implementation = ContactResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contact not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ContactResponse getById(
            @Parameter(description = "Contact ID", schema = @Schema(minLength = 1, maxLength = MAX_ID_LENGTH))
            @NotBlank @Size(min = 1, max = MAX_ID_LENGTH) @PathVariable final String id) {
        return contactService.getContactById(id)
                .map(ContactResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contact not found: " + id));
    }

    /**
     * Updates an existing contact.
     *
     * @param id      the contact ID (from path)
     * @param request the updated contact data
     * @return the updated contact
     * @throws ResourceNotFoundException if no contact with the given ID exists
     */
    @Operation(summary = "Update an existing contact")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contact updated",
                    content = @Content(schema = @Schema(implementation = ContactResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Contact not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ContactResponse update(
            @Parameter(description = "Contact ID", schema = @Schema(minLength = 1, maxLength = MAX_ID_LENGTH))
            @NotBlank @Size(min = 1, max = MAX_ID_LENGTH) @PathVariable final String id,
            @Valid @RequestBody final ContactRequest request) {

        if (!contactService.updateContact(
                id,
                request.firstName(),
                request.lastName(),
                request.phone(),
                request.address())) {
            throw new ResourceNotFoundException("Contact not found: " + id);
        }

        return getById(id);
    }

    /**
     * Deletes a contact by ID.
     *
     * @param id the contact ID
     * @throws ResourceNotFoundException if no contact with the given ID exists
     */
    @Operation(summary = "Delete a contact")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contact deleted"),
            @ApiResponse(responseCode = "404", description = "Contact not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @Parameter(description = "Contact ID", schema = @Schema(minLength = 1, maxLength = MAX_ID_LENGTH))
            @NotBlank @Size(min = 1, max = MAX_ID_LENGTH) @PathVariable final String id) {
        if (!contactService.deleteContact(id)) {
            throw new ResourceNotFoundException("Contact not found: " + id);
        }
    }
}
