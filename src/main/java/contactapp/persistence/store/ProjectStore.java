package contactapp.persistence.store;

import contactapp.domain.Project;
import contactapp.domain.ProjectStatus;
import contactapp.security.User;
import java.util.List;

/**
 * Marker interface tying {@link Project} aggregates to the generic {@link DomainDataStore}.
 */
public interface ProjectStore extends DomainDataStore<Project> {

    /**
     * Persists the aggregate on behalf of a specific user.
     *
     * <p>Default implementation throws so legacy in-memory stores are unaffected,
     * while JPA-backed stores can override with user-aware persistence logic.
     *
     * @param aggregate validated project aggregate
     * @param user owner of the aggregate
     */
    default void save(final Project aggregate, final User user) {
        throw new UnsupportedOperationException("User-scoped save is not supported");
    }

    /**
     * Finds all projects for a user with a specific status.
     *
     * <p>Default implementation throws so legacy in-memory stores are unaffected.
     *
     * @param user the user who owns the projects
     * @param status the status to filter by
     * @return list of projects matching the status
     */
    default List<Project> findByStatus(final User user, final ProjectStatus status) {
        throw new UnsupportedOperationException("Status-based queries are not supported");
    }
}
