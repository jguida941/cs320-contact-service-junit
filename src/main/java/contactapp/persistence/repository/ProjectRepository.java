package contactapp.persistence.repository;

import contactapp.domain.ProjectStatus;
import contactapp.persistence.entity.ProjectEntity;
import contactapp.security.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data repository for persisting projects with auto-generated numeric primary keys while
 * preserving the natural {@code projectId} string used by legacy callers.
 *
 * <p>Supports per-user data isolation by filtering projects by user_id.
 */
@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {

    /**
     * Finds all projects belonging to a specific user.
     *
     * @param user the user who owns the projects
     * @return list of projects for the given user
     */
    List<ProjectEntity> findByUser(User user);

    /**
     * Finds all projects belonging to a specific user with a specific status.
     *
     * @param user the user who owns the projects
     * @param status the status to filter by
     * @return list of projects for the given user with the specified status
     */
    List<ProjectEntity> findByUserAndStatus(User user, ProjectStatus status);

    /**
     * Finds a project by ID and user.
     *
     * @param projectId the project ID
     * @param user the user who owns the project
     * @return optional containing the project if found
     */
    Optional<ProjectEntity> findByProjectIdAndUser(String projectId, User user);

    /**
     * Finds a project by ID without user scoping (legacy compatibility).
     *
     * @param projectId the project ID
     * @return optional containing the project if found
     * @deprecated Use {@link #findByProjectIdAndUser(String, User)} for tenant isolation
     */
    @Deprecated(forRemoval = true)
    Optional<ProjectEntity> findByProjectId(String projectId);

    /**
     * Checks if a project exists by ID and user.
     *
     * @param projectId the project ID
     * @param user the user who owns the project
     * @return true if the project exists for the given user
     */
    boolean existsByProjectIdAndUser(String projectId, User user);

    /**
     * Checks if a project exists by ID regardless of user.
     *
     * @param projectId the project ID
     * @return true if any project exists with the ID
     * @deprecated Use {@link #existsByProjectIdAndUser(String, User)} instead
     */
    @Deprecated(forRemoval = true)
    boolean existsByProjectId(String projectId);

    /**
     * Deletes a project by ID and user.
     *
     * @param projectId the project ID
     * @param user the user who owns the project
     * @return number of deleted records (0 or 1)
     */
    @Modifying
    @Transactional
    int deleteByProjectIdAndUser(String projectId, User user);
}
