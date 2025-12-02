package contactapp.api.dto;

import contactapp.domain.Project;
import contactapp.domain.ProjectStatus;
import java.util.Objects;

/**
 * Response DTO for Project data returned by the API.
 *
 * <p>Provides a clean separation between the domain object and the API contract.
 * Uses a factory method to convert from the domain object.
 *
 * @param id          the project's unique identifier
 * @param name        the project's name
 * @param description the project's description
 * @param status      the project's status
 */
public record ProjectResponse(
        String id,
        String name,
        String description,
        ProjectStatus status
) {
    /**
     * Creates a ProjectResponse from a Project domain object.
     *
     * @param project the domain object to convert (must not be null)
     * @return a new ProjectResponse with the project's data
     * @throws NullPointerException if project is null
     */
    public static ProjectResponse from(final Project project) {
        Objects.requireNonNull(project, "project must not be null");
        return new ProjectResponse(
                project.getProjectId(),
                project.getName(),
                project.getDescription(),
                project.getStatus()
        );
    }
}
