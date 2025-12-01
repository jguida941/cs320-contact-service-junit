package contactapp.service;

import contactapp.api.exception.DuplicateResourceException;
import contactapp.config.ApplicationContextProvider;
import contactapp.domain.Task;
import contactapp.domain.Validation;
import contactapp.persistence.store.InMemoryTaskStore;
import contactapp.persistence.store.JpaTaskStore;
import contactapp.persistence.store.TaskStore;
import contactapp.security.Role;
import contactapp.security.User;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for creating, updating, and deleting {@link Task} objects.
 *
 * <p>Persistence is delegated to {@link TaskStore} so Spring-managed JPA repositories
 * and legacy {@link #getInstance()} callers share the same behavior while
 * {@link #clearAllTasks()} stays package-private for test isolation.
 *
 * <h2>Why Not Final?</h2>
 * <p>This class was previously {@code final}. The modifier was removed because
 * Spring's {@code @Transactional} annotation uses CGLIB proxy subclassing,
 * which requires non-final classes for method interception.
 */
@Service
@Transactional
public class TaskService {

    private static TaskService instance;

    private final TaskStore store;
    private final boolean legacyStore;

    @org.springframework.beans.factory.annotation.Autowired
    public TaskService(final TaskStore store) {
        this(store, false);
    }

    private TaskService(final TaskStore store, final boolean legacyStore) {
        this.store = store;
        this.legacyStore = legacyStore;
        registerInstance(this);
    }

    private static synchronized void registerInstance(final TaskService candidate) {
        if (instance != null && instance.legacyStore && !candidate.legacyStore) {
            instance.getAllTasks().forEach(candidate::addTask);
        }
        instance = candidate;
    }

    /**
     * Returns the single shared TaskService instance.
     *
     * <p>Thread safety: The method is synchronized so that, if multiple threads
     * call it at the same time, only one will create the instance.
     *
     * <p>Note: When using Spring DI (e.g., in controllers), prefer constructor
     * injection over this method. This exists for backward compatibility.
     * Both access patterns share the same instance and backing store.
     *
     * <p>If called before Spring context initializes, lazily creates a service
     * backed by {@link InMemoryTaskStore}. This preserves backward
     * compatibility for legacy non-Spring callers.
     *
     * @return the singleton TaskService instance
     */
    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "Singleton intentionally exposes shared instance for backward compatibility")
    public static synchronized TaskService getInstance() {
        if (instance != null) {
            return instance;
        }
        final ApplicationContext context = ApplicationContextProvider.getContext();
        if (context != null) {
            return context.getBean(TaskService.class);
        }
        return new TaskService(new InMemoryTaskStore(), true);
    }

    /**
     * Gets the currently authenticated user from the Spring Security context.
     *
     * @return the authenticated user
     * @throws IllegalStateException if no user is authenticated
     */
    private User getCurrentUser() {
        final var context = SecurityContextHolder.getContext();
        final Authentication authentication = context != null ? context.getAuthentication() : null;
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user found");
        }
        final Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new IllegalStateException("Authenticated principal is not a User");
        }
        return (User) principal;
    }

    /**
     * Checks if the current user has ADMIN role.
     *
     * @param user the user to check
     * @return true if the user is an ADMIN
     */
    private boolean isAdmin(final User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    /**
     * Adds a new task to the persistent store for the authenticated user.
     *
     * <p>Duplicate IDs bubble up as {@link DuplicateResourceException} so the REST layer can
     * consistently translate them into HTTP 409 responses. This relies on database uniqueness
     * constraints instead of manual {@code existsById} checks to avoid TOCTOU races.
     *
     * @param task the task to add; must not be null
     * @return true if the task was added
     * @throws IllegalArgumentException if task is null
     * @throws DuplicateResourceException if a task with the same ID already exists
     */
    public boolean addTask(final Task task) {
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
        final String taskId = task.getTaskId();
        if (taskId == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }

        // Use user-aware store methods if available
        if (store instanceof JpaTaskStore) {
            final JpaTaskStore jpaStore = (JpaTaskStore) store;
            final User currentUser = getCurrentUser();
            try {
                jpaStore.insert(task, currentUser);
                return true;
            } catch (DataIntegrityViolationException e) {
                throw new DuplicateResourceException(
                        "Task with id '" + taskId + "' already exists", e);
            }
        }

        // Fallback for legacy in-memory store
        return attemptLegacySave(task);
    }

    private boolean attemptLegacySave(final Task task) {
        if (store.existsById(task.getTaskId())) {
            throw new DuplicateResourceException(
                    "Task with id '" + task.getTaskId() + "' already exists");
        }
        store.save(task);
        return true;
    }

    /**
     * Deletes a task by id for the authenticated user.
     *
     * <p>The id is validated and trimmed before removal so callers can pass
     * whitespace and still reference the stored entry.
     *
     * @param taskId id to remove; must not be blank
     * @return true if removed, false otherwise
     * @throws IllegalArgumentException if taskId is null or blank
     */
    public boolean deleteTask(final String taskId) {
        Validation.validateNotBlank(taskId, "taskId");
        final String trimmedId = taskId.trim();

        if (store instanceof JpaTaskStore) {
            final JpaTaskStore jpaStore = (JpaTaskStore) store;
            final User currentUser = getCurrentUser();
            return jpaStore.deleteById(trimmedId, currentUser);
        }

        return store.deleteById(trimmedId);
    }

    /**
     * Updates the name and description of an existing task for the authenticated user.
     *
     * @param taskId the id of the task to update
     * @param newName new task name
     * @param description new description
     * @return true if the task exists and was updated, false if no task with that id exists
     * @throws IllegalArgumentException if any new field value is invalid
     */
    public boolean updateTask(
            final String taskId,
            final String newName,
            final String description) {
        Validation.validateNotBlank(taskId, "taskId");
        final String normalizedId = taskId.trim();

        if (store instanceof JpaTaskStore) {
            final JpaTaskStore jpaStore = (JpaTaskStore) store;
            final User currentUser = getCurrentUser();

            final Optional<Task> task = jpaStore.findById(normalizedId, currentUser);
            if (task.isEmpty()) {
                return false;
            }
            final Task existing = task.get();
            existing.update(newName, description);
            jpaStore.save(existing, currentUser);
            return true;
        }

        // Fallback for legacy in-memory store
        final Optional<Task> task = store.findById(normalizedId);
        if (task.isEmpty()) {
            return false;
        }
        final Task existing = task.get();
        existing.update(newName, description);
        store.save(existing);
        return true;
    }

    /**
     * Returns an unmodifiable snapshot of the current task store.
     *
     * <p>Returns defensive copies of each Task to prevent external mutation
     * of internal state.
     *
     * @return unmodifiable map of defensive task copies
     */
    @Transactional(readOnly = true)
    public Map<String, Task> getDatabase() {
        return store.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Task::getTaskId,
                        Task::copy));
    }

    /**
     * Returns all tasks as a list of defensive copies for the authenticated user.
     *
     * <p>For authenticated users: returns only their tasks.
     * For ADMIN users: returns all tasks (when called from controllers with ?all=true).
     *
     * <p>Encapsulates the internal storage structure so controllers don't
     * need to access getDatabase() directly.
     *
     * @return list of task defensive copies
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasks() {
        if (store instanceof JpaTaskStore) {
            final JpaTaskStore jpaStore = (JpaTaskStore) store;
            final User currentUser = getCurrentUser();
            return jpaStore.findAll(currentUser).stream()
                    .map(Task::copy)
                    .toList();
        }

        return store.findAll().stream()
                .map(Task::copy)
                .toList();
    }

    /**
     * Returns all tasks across all users (ADMIN only).
     *
     * <p>This method should only be called by controllers when ?all=true
     * is specified and the authenticated user is an ADMIN. When the service is
     * still running in its legacy in-memory mode (before Spring starts), the
     * method provides the legacy behavior of returning the entire store without
     * requiring a security context.
     *
     * @return list of all task defensive copies
     * @throws AccessDeniedException if current user is not an ADMIN
     */
    @Transactional(readOnly = true)
    public List<Task> getAllTasksAllUsers() {
        if (legacyStore) {
            return store.findAll().stream()
                    .map(Task::copy)
                    .toList();
        }

        final User currentUser = getCurrentUser();
        if (!isAdmin(currentUser)) {
            throw new AccessDeniedException("Only ADMIN users can access all tasks");
        }

        return store.findAll().stream()
                .map(Task::copy)
                .toList();
    }

    /**
     * Finds a task by ID for the authenticated user.
     *
     * <p>The ID is validated and trimmed before lookup so callers can pass
     * values like " 123 " and still find the task stored as "123".
     *
     * @param taskId the task ID to search for
     * @return Optional containing a defensive copy of the task, or empty if not found
     * @throws IllegalArgumentException if taskId is null or blank
     */
    @Transactional(readOnly = true)
    public Optional<Task> getTaskById(final String taskId) {
        Validation.validateNotBlank(taskId, "taskId");
        final String trimmedId = taskId.trim();

        if (store instanceof JpaTaskStore) {
            final JpaTaskStore jpaStore = (JpaTaskStore) store;
            final User currentUser = getCurrentUser();
            return jpaStore.findById(trimmedId, currentUser).map(Task::copy);
        }

        return store.findById(trimmedId).map(Task::copy);
    }

    void clearAllTasks() {
        store.deleteAll();
    }
}
