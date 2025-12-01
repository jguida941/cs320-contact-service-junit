package contactapp.persistence.store;

import contactapp.domain.Task;
import contactapp.persistence.entity.TaskEntity;
import contactapp.persistence.mapper.TaskMapper;
import contactapp.persistence.repository.TaskRepository;
import contactapp.security.User;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed {@link TaskStore}.
 *
 * <p>Supports per-user data isolation by filtering tasks based on the authenticated user.
 */
@Component
public class JpaTaskStore implements TaskStore {

    private final TaskRepository repository;
    private final TaskMapper mapper;

    public JpaTaskStore(final TaskRepository repository, final TaskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Checks if a task exists for a specific user.
     *
     * @param id the task ID
     * @param user the user who owns the task
     * @return true if the task exists for the given user
     */
    public boolean existsById(final String id, final User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return repository.existsByTaskIdAndUser(id, user);
    }

    @Override
    @Deprecated(forRemoval = true)
    public boolean existsById(final String id) {
        throw new UnsupportedOperationException("Use existsById(id, User) for scoped lookups");
    }

    /**
     * Saves a task for a specific user.
     *
     * @param aggregate the task to save
     * @param user the user who owns the task
     */
    @Transactional
    public void save(final Task aggregate, final User user) {
        if (aggregate == null) {
            throw new IllegalArgumentException("task aggregate must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        final Optional<TaskEntity> existing = repository.findByTaskIdAndUser(
                aggregate.getTaskId(), user);
        existing.ifPresentOrElse(
                entity -> {
                    mapper.updateEntity(entity, aggregate);
                    repository.save(entity);
                },
                () -> repository.save(mapper.toEntity(aggregate, user)));
    }

    @Transactional
    public void insert(final Task aggregate, final User user) {
        if (aggregate == null) {
            throw new IllegalArgumentException("task aggregate must not be null");
        }
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        repository.save(mapper.toEntity(aggregate, user));
    }

    @Override
    @Deprecated(forRemoval = true)
    public void save(final Task aggregate) {
        throw new UnsupportedOperationException(
                "Use save(Task, User) instead. This method requires a User parameter.");
    }

    /**
     * Finds a task by ID for a specific user.
     *
     * @param id the task ID
     * @param user the user who owns the task
     * @return optional containing the task if found
     */
    public Optional<Task> findById(final String id, final User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return repository.findByTaskIdAndUser(id, user).map(mapper::toDomain);
    }

    @Override
    @Deprecated(forRemoval = true)
    public Optional<Task> findById(final String id) {
        throw new UnsupportedOperationException(
                "Use findById(String, User) to enforce per-user authorization");
    }

    /**
     * Finds all tasks for a specific user.
     *
     * @param user the user who owns the tasks
     * @return list of tasks for the given user
     */
    public List<Task> findAll(final User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return repository.findByUser(user).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Task> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    /**
     * Deletes a task by ID for a specific user.
     *
     * @param id the task ID
     * @param user the user who owns the task
     * @return true if the task was deleted, false if not found
     */
    public boolean deleteById(final String id, final User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return repository.deleteByTaskIdAndUser(id, user) > 0;
    }

    @Override
    @Deprecated(forRemoval = true)
    public boolean deleteById(final String id) {
        throw new UnsupportedOperationException(
                "Use deleteById(String, User) to enforce per-user authorization");
    }

    @Override
    public void deleteAll() {
        repository.deleteAll();
    }
}
