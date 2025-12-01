package contactapp.persistence.store;

import contactapp.domain.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Fallback task store used before Spring wires the JPA layer.
 */
public class InMemoryTaskStore implements TaskStore {

    private final Map<String, Task> database = new ConcurrentHashMap<>();

    @Override
    public boolean existsById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }
        return database.containsKey(id);
    }

    @Override
    public void save(final Task aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("task aggregate must not be null");
        }
        final String taskId = aggregate.getTaskId();
        if (taskId == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }
        final Task copy = Optional.ofNullable(aggregate.copy())
                .orElseThrow(() -> new IllegalStateException("task copy must not be null"));
        final Task existing = database.putIfAbsent(taskId, copy);
        if (existing != null) {
            throw new DataIntegrityViolationException("Task with id '" + taskId + "' already exists");
        }
    }

    @Override
    public Optional<Task> findById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }
        final Task task = database.get(id);
        return task == null ? Optional.empty() : Optional.of(task.copy());
    }

    @Override
    public List<Task> findAll() {
        final List<Task> tasks = new ArrayList<>();
        database.values().forEach(task -> tasks.add(task.copy()));
        return tasks;
    }

    @Override
    public boolean deleteById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("taskId must not be null");
        }
        return database.remove(id) != null;
    }

    @Override
    public void deleteAll() {
        database.clear();
    }
}
