package contactapp.persistence.mapper;

import contactapp.domain.Task;
import contactapp.persistence.entity.TaskEntity;
import contactapp.security.User;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Task} domain objects and {@link TaskEntity}.
 */
@Component
public class TaskMapper {

    /**
     * Converts a domain Task to an entity, associating it with the given user.
     *
     * @param domain the domain task
     * @param user the user who owns this task
     * @return the entity representation
     */
    public TaskEntity toEntity(final Task domain, final User user) {
        if (domain == null) {
            return null;
        }
        return new TaskEntity(
                domain.getTaskId(),
                domain.getName(),
                domain.getDescription(),
                user);
    }

    /**
     * Converts a domain Task to an entity.
     *
     * @param domain the domain task
     * @return the entity representation
     * @deprecated Use {@link #toEntity(Task, User)} instead
     */
    @Deprecated
    public TaskEntity toEntity(final Task domain) {
        if (domain == null) {
            return null;
        }
        throw new UnsupportedOperationException(
                "Use toEntity(Task, User) instead. This method requires a User parameter.");
    }

    public Task toDomain(final TaskEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Task(
                entity.getTaskId(),
                entity.getName(),
                entity.getDescription());
    }

    /**
     * Updates an existing entity with the latest mutable values from the domain aggregate.
     *
     * @param target entity to mutate
     * @param source validated domain task
     */
    public void updateEntity(final TaskEntity target, final Task source) {
        if (target == null) {
            throw new IllegalArgumentException("target entity must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("source task must not be null");
        }
        target.setName(source.getName());
        target.setDescription(source.getDescription());
    }
}
