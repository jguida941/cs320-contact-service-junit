package contactapp.persistence.entity;

import contactapp.domain.ProjectStatus;
import contactapp.domain.Validation;
import contactapp.security.User;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * JPA entity mirroring the {@link contactapp.domain.Project} structure.
 *
 * <p>Domain objects remain final/value-focused while this mutable entity exists purely
 * for persistence. Column lengths mirror {@link Validation} constants so the database
 * schema always matches domain constraints.
 */
@Entity
@Table(
        name = "projects",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_projects_project_id_user_id",
                columnNames = {"project_id", "user_id"}))
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "JPA entity returns User reference for ORM relationship; "
                + "entity lifecycle is managed by Hibernate persistence context")
public class ProjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "project_id", length = Validation.MAX_ID_LENGTH, nullable = false)
    private String projectId;

    @Column(name = "name", length = Validation.MAX_PROJECT_NAME_LENGTH, nullable = false)
    private String name;

    @Column(name = "description", length = Validation.MAX_PROJECT_DESCRIPTION_LENGTH)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = Validation.MAX_STATUS_LENGTH, nullable = false)
    private ProjectStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Protected no-arg constructor required by JPA. */
    protected ProjectEntity() {
        // JPA only
    }

    public ProjectEntity(
            final String projectId,
            final String name,
            final String description,
            final ProjectStatus status,
            final User user) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(final ProjectStatus status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }
}
