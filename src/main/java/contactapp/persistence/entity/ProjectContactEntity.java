package contactapp.persistence.entity;

import contactapp.domain.ProjectContactConstraints;
import contactapp.domain.Validation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * JPA entity representing the many-to-many relationship between Projects and Contacts.
 *
 * <p>This junction table enables tracking stakeholders, clients, and other related
 * contacts for each project. The composite primary key (project_id, contact_id)
 * ensures a contact can only be added once per project.
 *
 * <h2>Composite Key Pattern</h2>
 * <p>Uses {@code @IdClass} to define a composite primary key consisting of:
 * <ul>
 *   <li>{@code projectId} - Foreign key to projects table</li>
 *   <li>{@code contactId} - Foreign key to contacts table</li>
 * </ul>
 */
@Entity
@Table(name = "project_contacts")
@IdClass(ProjectContactId.class)
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "JPA entity returns entity references for ORM relationship; "
                + "entity lifecycle is managed by Hibernate persistence context")
public class ProjectContactEntity {

    @Id
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Id
    @Column(name = "contact_id", nullable = false)
    private Long contactId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", insertable = false, updatable = false, nullable = false)
    private ProjectEntity project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", insertable = false, updatable = false, nullable = false)
    private ContactEntity contact;

    @Column(name = "role", length = ProjectContactConstraints.MAX_ROLE_LENGTH)
    private String role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Protected no-arg constructor required by JPA. */
    protected ProjectContactEntity() {
        // JPA only
    }

    @SuppressFBWarnings(
            value = "CT_CONSTRUCTOR_THROW",
            justification = "Constructor performs validation and intentionally throws before persisting invalid state")
    public ProjectContactEntity(
            final ProjectEntity project,
            final ContactEntity contact,
            final String role) {
        if (project == null || project.getId() == null) {
            throw new IllegalArgumentException("project must not be null and must have an ID");
        }
        if (contact == null || contact.getId() == null) {
            throw new IllegalArgumentException("contact must not be null and must have an ID");
        }
        this.projectId = project.getId();
        this.contactId = contact.getId();
        this.project = project;
        this.contact = contact;
        this.role = validateRole(role);
        this.createdAt = Instant.now();
    }

    private String validateRole(final String candidateRole) {
        if (candidateRole == null || candidateRole.trim().isEmpty()) {
            return null;
        }
        return Validation.validateTrimmedLengthAllowBlank(
                candidateRole, "role", 0, ProjectContactConstraints.MAX_ROLE_LENGTH);
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getContactId() {
        return contactId;
    }

    public ProjectEntity getProject() {
        return project;
    }

    public ContactEntity getContact() {
        return contact;
    }

    public String getRole() {
        return role;
    }

    public void setRole(final String role) {
        this.role = validateRole(role);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
