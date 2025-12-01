package contactapp.persistence.entity;

import contactapp.domain.Validation;
import contactapp.security.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Hibernate entity for {@link contactapp.domain.Task} persistence.
 */
@Entity
@Table(
        name = "tasks",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_tasks_task_id_user_id",
                columnNames = {"task_id", "user_id"}))
public class TaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", length = Validation.MAX_ID_LENGTH, nullable = false)
    private String taskId;

    @Column(name = "name", length = Validation.MAX_TASK_NAME_LENGTH, nullable = false)
    private String name;

    @Column(name = "description", length = Validation.MAX_DESCRIPTION_LENGTH, nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected TaskEntity() {
        // JPA only
    }

    public TaskEntity(
            final String taskId,
            final String name,
            final String description,
            final User user) {
        this.taskId = taskId;
        this.name = name;
        this.description = description;
        this.user = user;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(final String taskId) {
        this.taskId = taskId;
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

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }
}
