package contactapp.persistence.repository;

import contactapp.persistence.entity.ProjectContactEntity;
import contactapp.persistence.entity.ProjectContactId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for managing project-contact relationships.
 *
 * <p>Supports many-to-many linking between projects and contacts, with optional
 * role metadata (e.g., CLIENT, STAKEHOLDER, VENDOR).
 */
@Repository
public interface ProjectContactRepository extends JpaRepository<ProjectContactEntity, ProjectContactId> {

    /**
     * Finds all contact relationships for a given project (by surrogate key).
     *
     * @param projectId the project's surrogate primary key (Long)
     * @return list of project-contact entities
     */
    List<ProjectContactEntity> findByProjectId(Long projectId);

    /**
     * Finds all project relationships for a given contact (by surrogate key).
     *
     * @param contactId the contact's surrogate primary key (Long)
     * @return list of project-contact entities
     */
    List<ProjectContactEntity> findByContactId(Long contactId);

    /**
     * Checks if a specific project-contact relationship exists.
     *
     * @param projectId the project's surrogate primary key (Long)
     * @param contactId the contact's surrogate primary key (Long)
     * @return true if the relationship exists
     */
    boolean existsByProjectIdAndContactId(Long projectId, Long contactId);
}
