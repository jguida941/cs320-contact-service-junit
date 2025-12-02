package contactapp.persistence.store;

import contactapp.domain.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Simple in-memory store used when {@link contactapp.service.ProjectService#getInstance()}
 * is accessed before Spring initializes the JPA infrastructure.
 */
public class InMemoryProjectStore implements ProjectStore {

    private final Map<String, Project> database = new ConcurrentHashMap<>();

    @Override
    public boolean existsById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        return database.containsKey(id);
    }

    @Override
    public void save(final Project aggregate) {
        if (aggregate == null) {
            throw new IllegalArgumentException("project aggregate must not be null");
        }
        final String projectId = aggregate.getProjectId();
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        final Project copy = Optional.ofNullable(aggregate.copy())
                .orElseThrow(() -> new IllegalStateException("project copy must not be null"));
        final Project existing = database.putIfAbsent(projectId, copy);
        if (existing != null) {
            throw new DataIntegrityViolationException("Project with id '" + projectId + "' already exists");
        }
    }

    @Override
    public Optional<Project> findById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        final Project project = database.get(id);
        return project == null ? Optional.empty() : Optional.of(project.copy());
    }

    @Override
    public List<Project> findAll() {
        final List<Project> projects = new ArrayList<>();
        database.values().forEach(project -> projects.add(project.copy()));
        return projects;
    }

    @Override
    public boolean deleteById(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }
        return database.remove(id) != null;
    }

    @Override
    public void deleteAll() {
        database.clear();
    }
}
