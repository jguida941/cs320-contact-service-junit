package contactapp.persistence.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for {@link ProjectContactEntity}.
 *
 * <p>JPA requires composite key classes to:
 * <ul>
 *   <li>Implement {@link Serializable}</li>
 *   <li>Have a public no-arg constructor</li>
 *   <li>Override {@link #equals(Object)} and {@link #hashCode()}</li>
 * </ul>
 */
public class ProjectContactId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long projectId;
    private Long contactId;

    /** Public no-arg constructor required by JPA. */
    public ProjectContactId() {
        // JPA only
    }

    public ProjectContactId(final Long projectId, final Long contactId) {
        this.projectId = projectId;
        this.contactId = contactId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(final Long projectId) {
        this.projectId = projectId;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(final Long contactId) {
        this.contactId = contactId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ProjectContactId that = (ProjectContactId) o;
        return Objects.equals(projectId, that.projectId)
                && Objects.equals(contactId, that.contactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectId, contactId);
    }
}
