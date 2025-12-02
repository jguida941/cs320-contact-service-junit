package contactapp.persistence.mapper;

import contactapp.domain.Project;
import contactapp.persistence.entity.ProjectEntity;
import contactapp.security.User;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Project} domain objects and {@link ProjectEntity} rows.
 *
 * <p>{@link #toDomain(ProjectEntity)} reuses the domain constructor so corrupted
 * database rows still go through {@link contactapp.domain.Validation}.
 */
@Component
public class ProjectMapper {

    /**
     * Converts a domain Project to an entity, associating it with the given user.
     *
     * @param domain the domain project
     * @param user the user who owns this project
     * @return the entity representation
     */
    public ProjectEntity toEntity(final Project domain, final User user) {
        if (domain == null) {
            return null;
        }
        return new ProjectEntity(
                domain.getProjectId(),
                domain.getName(),
                domain.getDescription(),
                domain.getStatus(),
                user);
    }

    /**
     * Converts a domain Project to an entity using the existing user from the entity.
     *
     * <p>This method should only be used when updating an existing entity that
     * already has a user association. For new entities, use {@link #toEntity(Project, User)}.
     *
     * @param domain the domain project
     * @throws UnsupportedOperationException always; kept for binary compatibility only
     * @deprecated Use {@link #toEntity(Project, User)} instead. This stub intentionally throws to
     * discourage callers from forgetting to include a {@link User} association.
     */
    @Deprecated
    public ProjectEntity toEntity(final Project domain) {
        if (domain == null) {
            return null;
        }
        // This is used for updates where the entity already exists with a user
        // The JpaStore will handle fetching the existing entity and preserving the user
        throw new UnsupportedOperationException(
                "Use toEntity(Project, User) instead. This method requires a User parameter.");
    }

    public Project toDomain(final ProjectEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Project(
                entity.getProjectId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStatus());
    }

    /**
     * Updates an existing entity with the latest mutable fields from the domain aggregate.
     *
     * @param target the entity to mutate; must not be null
     * @param source the domain project providing the latest data; must not be null
     */
    public void updateEntity(final ProjectEntity target, final Project source) {
        if (target == null) {
            throw new IllegalArgumentException("target entity must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("source project must not be null");
        }
        target.setName(source.getName());
        target.setDescription(source.getDescription());
        target.setStatus(source.getStatus());
    }
}
