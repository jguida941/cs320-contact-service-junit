package contactapp.service;

import contactapp.api.exception.DuplicateResourceException;
import contactapp.api.exception.ResourceNotFoundException;
import contactapp.config.ApplicationContextProvider;
import contactapp.domain.Contact;
import contactapp.domain.Project;
import contactapp.domain.ProjectStatus;
import contactapp.domain.Validation;
import contactapp.persistence.entity.ContactEntity;
import contactapp.persistence.entity.ProjectContactEntity;
import contactapp.persistence.entity.ProjectEntity;
import contactapp.persistence.mapper.ContactMapper;
import contactapp.persistence.repository.ContactRepository;
import contactapp.persistence.repository.ProjectContactRepository;
import contactapp.persistence.repository.ProjectRepository;
import contactapp.persistence.store.InMemoryProjectStore;
import contactapp.persistence.store.JpaProjectStore;
import contactapp.persistence.store.ProjectStore;
import contactapp.security.Role;
import contactapp.security.User;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for managing {@link Project} instances.
 *
 * <p>Delegates persistence to a {@link ProjectStore} so the same lifecycle
 * works for both Spring-managed JPA repositories and legacy callers that still
 * rely on {@link #getInstance()} before the application context starts.
 *
 * <h2>Design Decisions</h2>
 * <ul>
 *   <li>Singleton pattern retained for backward compatibility (legacy callers)</li>
 *   <li>{@code @Service} enables Spring DI in controllers (Phase 2+)</li>
 *   <li>{@code @Transactional} methods ensure repository operations run atomically</li>
 *   <li>{@link #clearAllProjects()} remains package-private for test isolation</li>
 * </ul>
 *
 * <h2>Why Not Final?</h2>
 * <p>This class was previously {@code final}. The modifier was removed because
 * Spring's {@code @Transactional} annotation uses CGLIB proxy subclassing,
 * which requires non-final classes for method interception.
 *
 * @see Project
 * @see Validation
 */
@Service
@Transactional
@SuppressFBWarnings(
        value = "CT_CONSTRUCTOR_THROW",
        justification = "Constructor calls registerInstance() for singleton pattern compatibility; "
                + "this is intentional for backward compatibility with legacy non-Spring callers")
public class ProjectService {

    private static ProjectService instance;

    private final ProjectStore store;
    private final boolean legacyStore;

    private ProjectContactRepository projectContactRepository;
    private ContactRepository contactRepository;
    private ProjectRepository projectRepository;
    private ContactMapper contactMapper;

    /**
     * Primary constructor used by Spring to wire the JPA-backed store.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public ProjectService(final ProjectStore store) {
        this(store, false);
    }

    /**
     * Sets repositories and mappers for contact linking functionality.
     * Called by Spring after construction.
     */
    @Autowired(required = false)
    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring injects repos/mappers as singletons; storing refs is safe")
    public void setRepositories(
            final ProjectContactRepository projectContactRepositoryParam,
            final ContactRepository contactRepositoryParam,
            final ProjectRepository projectRepositoryParam,
            final ContactMapper contactMapperParam) {
        this.projectContactRepository = projectContactRepositoryParam;
        this.contactRepository = contactRepositoryParam;
        this.projectRepository = projectRepositoryParam;
        this.contactMapper = contactMapperParam;
    }

    private ProjectService(final ProjectStore store, final boolean legacyStore) {
        this.store = store;
        this.legacyStore = legacyStore;
        registerInstance(this);
    }

    private static synchronized void registerInstance(final ProjectService candidate) {
        if (instance != null && instance.legacyStore && !candidate.legacyStore) {
            instance.getAllProjects().forEach(candidate::addProject);
        }
        instance = candidate;
    }

    /**
     * Returns the global {@code ProjectService} singleton instance.
     *
     * <p>The method is synchronized so that only one instance is created
     * even if multiple threads call it at the same time.
     *
     * <p>Note: When using Spring DI (e.g., in controllers), prefer constructor
     * injection over this method. This exists for backward compatibility.
     * Both access patterns share the same instance and backing store.
     *
     * <p>If called before Spring context initializes, lazily creates a service
     * backed by {@link InMemoryProjectStore}. This preserves backward
     * compatibility for legacy non-Spring callers.
     *
     * @return the singleton {@code ProjectService} instance
     */
    @SuppressFBWarnings(
            value = "MS_EXPOSE_REP",
            justification = "Singleton intentionally exposes shared instance for backward compatibility")
    public static synchronized ProjectService getInstance() {
        if (instance != null) {
            return instance;
        }
        final ApplicationContext context = ApplicationContextProvider.getContext();
        if (context != null) {
            return context.getBean(ProjectService.class);
        }
        return new ProjectService(new InMemoryProjectStore(), true);
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
            return handleMissingAuthentication();
        }
        final Object principal = authentication.getPrincipal();
        if (!(principal instanceof User)) {
            throw new IllegalStateException("Authenticated principal is not a User");
        }
        return (User) principal;
    }

    private User handleMissingAuthentication() {
        if (legacyStore) {
            throw new IllegalStateException(
                    "Legacy in-memory store does not require authenticated users; "
                            + "avoid user-scoped JPA operations until the security context is initialized.");
        }
        throw new IllegalStateException("No authenticated user found");
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
     * Adds a new project to the store for the authenticated user.
     *
     * <p>Database uniqueness constraints act as the single source of truth for detecting duplicates.
     * When a conflicting {@code projectId} exists, {@link DuplicateResourceException} propagates to
     * the controller so the global exception handler can return a 409 Conflict response.
     *
     * @param project the project to add; must not be null
     * @return true if the project was added
     * @throws IllegalArgumentException if project is null
     * @throws DuplicateResourceException if a project with the same ID already exists
     */
    public boolean addProject(final Project project) {
        if (project == null) {
            throw new IllegalArgumentException("project must not be null");
        }
        final String projectId = project.getProjectId();
        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null");
        }

        // Get authenticated user and use user-aware store methods if available
        if (store instanceof JpaProjectStore) {
            final JpaProjectStore jpaStore = (JpaProjectStore) store;
            final User currentUser = getCurrentUser();
            try {
                jpaStore.insert(project, currentUser);
                return true;
            } catch (DataIntegrityViolationException e) {
                throw new DuplicateResourceException(
                        "Project with id '" + projectId + "' already exists", e);
            }
        }

        // Fallback for legacy in-memory store
        if (store.existsById(projectId)) {
            throw new DuplicateResourceException(
                    "Project with id '" + projectId + "' already exists");
        }
        store.save(project);
        return true;
    }

    /**
     * Deletes a project by id for the authenticated user.
     *
     * <p>The provided id is validated and trimmed before removal so callers
     * can pass values like " 123 " and still target the stored entry for "123".
     *
     * @param projectId the id of the project to remove; must not be null or blank
     * @return true if a project was removed, false if no project existed
     * @throws IllegalArgumentException if projectId is null or blank
     */
    public boolean deleteProject(final String projectId) {
        Validation.validateNotBlank(projectId, "projectId");
        final String trimmedId = projectId.trim();

        if (store instanceof JpaProjectStore) {
            final JpaProjectStore jpaStore = (JpaProjectStore) store;
            final User currentUser = getCurrentUser();
            return jpaStore.deleteById(trimmedId, currentUser);
        }

        return store.deleteById(trimmedId);
    }

    /**
     * Updates an existing project's mutable fields by id for the authenticated user.
     *
     * @param projectId the id of the project to update
     * @param name new name
     * @param description new description
     * @param status new status
     * @return true if the project exists and was updated, false if no project with that id exists
     * @throws IllegalArgumentException if any new field value is invalid
     */
    public boolean updateProject(
            final String projectId,
            final String name,
            final String description,
            final ProjectStatus status) {
        Validation.validateNotBlank(projectId, "projectId");
        final String normalizedId = projectId.trim();

        if (store instanceof JpaProjectStore) {
            final JpaProjectStore jpaStore = (JpaProjectStore) store;
            final User currentUser = getCurrentUser();

            final Optional<Project> project = jpaStore.findById(normalizedId, currentUser);
            if (project.isEmpty()) {
                return false;
            }
            final Project existing = project.get();
            existing.update(name, description, status);
            jpaStore.save(existing, currentUser);
            return true;
        }

        // Fallback for legacy in-memory store
        final Optional<Project> project = store.findById(normalizedId);
        if (project.isEmpty()) {
            return false;
        }
        final Project existing = project.get();
        existing.update(name, description, status);
        store.save(existing);
        return true;
    }

    /**
     * Returns a read-only snapshot of the project store.
     *
     * <p>Returns defensive copies of each Project to prevent external mutation
     * of internal state. Modifications to the returned projects do not affect
     * the projects stored in the service.
     *
     * @return unmodifiable map of project defensive copies
     */
    @Transactional(readOnly = true)
    public Map<String, Project> getDatabase() {
        return store.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Project::getProjectId,
                        Project::copy));
    }

    /**
     * Returns all projects as a list of defensive copies.
     *
     * <p>For authenticated users: returns only their projects.
     * For ADMIN users: returns all projects (when called from controllers with ?all=true).
     *
     * <p>Encapsulates the internal storage structure so controllers don't
     * need to access getDatabase() directly.
     *
     * @return list of project defensive copies
     */
    @Transactional(readOnly = true)
    public List<Project> getAllProjects() {
        if (store instanceof JpaProjectStore) {
            final JpaProjectStore jpaStore = (JpaProjectStore) store;
            final User currentUser = getCurrentUser();
            return jpaStore.findAll(currentUser).stream()
                    .map(Project::copy)
                    .toList();
        }

        return store.findAll().stream()
                .map(Project::copy)
                .toList();
    }

    /**
     * Returns all projects across all users (ADMIN only).
     *
     * <p>This method should only be called by controllers when ?all=true
     * is specified and the authenticated user is an ADMIN.
     *
     * @return list of all project defensive copies
     * @throws AccessDeniedException if current user is not an ADMIN
     */
    @Transactional(readOnly = true)
    public List<Project> getAllProjectsAllUsers() {
        final User currentUser = getCurrentUser();
        if (!isAdmin(currentUser)) {
            throw new AccessDeniedException("Only ADMIN users can access all projects");
        }

        return store.findAll().stream()
                .map(Project::copy)
                .toList();
    }

    /**
     * Finds a project by ID for the authenticated user.
     *
     * <p>The ID is validated and trimmed before lookup so callers can pass
     * values like " 123 " and still find the project stored as "123".
     *
     * @param projectId the project ID to search for
     * @return Optional containing a defensive copy of the project, or empty if not found
     * @throws IllegalArgumentException if projectId is null or blank
     */
    @Transactional(readOnly = true)
    public Optional<Project> getProjectById(final String projectId) {
        Validation.validateNotBlank(projectId, "projectId");
        final String trimmedId = projectId.trim();

        if (store instanceof JpaProjectStore) {
            final JpaProjectStore jpaStore = (JpaProjectStore) store;
            final User currentUser = getCurrentUser();
            return jpaStore.findById(trimmedId, currentUser).map(Project::copy);
        }

        return store.findById(trimmedId).map(Project::copy);
    }

    /**
     * Returns projects filtered by status for the authenticated user.
     *
     * @param status the status to filter by; must not be null
     * @return list of project defensive copies matching the status
     * @throws IllegalArgumentException if status is null
     */
    @Transactional(readOnly = true)
    public List<Project> getProjectsByStatus(final ProjectStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }

        if (store instanceof JpaProjectStore) {
            final JpaProjectStore jpaStore = (JpaProjectStore) store;
            final User currentUser = getCurrentUser();
            return jpaStore.findByStatus(currentUser, status).stream()
                    .map(Project::copy)
                    .toList();
        }

        // Fallback for legacy in-memory store
        return store.findAll().stream()
                .filter(p -> p.getStatus() == status)
                .map(Project::copy)
                .toList();
    }

    void clearAllProjects() {
        store.deleteAll();
    }

    // ==================== Contact Linking Methods ====================

    /**
     * Adds a contact to a project with an optional role (e.g., CLIENT, STAKEHOLDER).
     *
     * <p>Enforces per-user data isolation: both the project and contact must belong
     * to the authenticated user. The operation is idempotent - if the relationship
     * already exists, this method does nothing and returns false.
     *
     * @param projectId the project ID (string, not surrogate key)
     * @param contactId the contact ID (string, not surrogate key)
     * @param role optional role description (max 50 chars)
     * @return true if the relationship was created, false if it already existed
     * @throws ResourceNotFoundException if project or contact not found for the current user
     * @throws IllegalArgumentException if projectId or contactId is null/blank
     * @throws IllegalStateException if repositories are not available
     */
    public boolean addContactToProject(final String projectId, final String contactId, final String role) {
        Validation.validateNotBlank(projectId, "projectId");
        Validation.validateNotBlank(contactId, "contactId");
        ensureRepositoriesAvailable();

        final User currentUser = getCurrentUser();

        // Fetch project entity
        final ProjectEntity projectEntity = projectRepository
                .findByProjectIdAndUser(projectId.trim(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectId));

        // Fetch contact entity
        final ContactEntity contactEntity = contactRepository
                .findByContactIdAndUser(contactId.trim(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contact not found: " + contactId));

        // Check if relationship already exists
        if (projectContactRepository.existsByProjectIdAndContactId(
                projectEntity.getId(), contactEntity.getId())) {
            return false;
        }

        // Create the relationship
        final ProjectContactEntity link = new ProjectContactEntity(
                projectEntity, contactEntity, role);
        projectContactRepository.save(link);
        return true;
    }

    /**
     * Removes a contact from a project.
     *
     * <p>Enforces per-user data isolation: both the project and contact must belong
     * to the authenticated user.
     *
     * @param projectId the project ID (string, not surrogate key)
     * @param contactId the contact ID (string, not surrogate key)
     * @return true if the relationship was deleted, false if it didn't exist
     * @throws ResourceNotFoundException if project or contact not found for the current user
     * @throws IllegalArgumentException if projectId or contactId is null/blank
     * @throws IllegalStateException if repositories are not available
     */
    public boolean removeContactFromProject(final String projectId, final String contactId) {
        Validation.validateNotBlank(projectId, "projectId");
        Validation.validateNotBlank(contactId, "contactId");
        ensureRepositoriesAvailable();

        final User currentUser = getCurrentUser();

        // Fetch project entity
        final ProjectEntity projectEntity = projectRepository
                .findByProjectIdAndUser(projectId.trim(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectId));

        // Fetch contact entity
        final ContactEntity contactEntity = contactRepository
                .findByContactIdAndUser(contactId.trim(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contact not found: " + contactId));

        // Find and delete the relationship
        final Optional<ProjectContactEntity> link = projectContactRepository.findById(
                new contactapp.persistence.entity.ProjectContactId(
                        projectEntity.getId(), contactEntity.getId()));

        if (link.isPresent()) {
            projectContactRepository.delete(link.get());
            return true;
        }
        return false;
    }

    /**
     * Returns all contacts associated with a project.
     *
     * <p>Enforces per-user data isolation: only returns contacts if the project
     * belongs to the authenticated user.
     *
     * @param projectId the project ID (string, not surrogate key)
     * @return list of contacts linked to the project (defensive copies)
     * @throws ResourceNotFoundException if project not found for the current user
     * @throws IllegalArgumentException if projectId is null/blank
     * @throws IllegalStateException if repositories are not available
     */
    @Transactional(readOnly = true)
    public List<Contact> getProjectContacts(final String projectId) {
        Validation.validateNotBlank(projectId, "projectId");
        ensureRepositoriesAvailable();

        final User currentUser = getCurrentUser();

        // Verify project exists and belongs to user
        final ProjectEntity projectEntity = projectRepository
                .findByProjectIdAndUser(projectId.trim(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + projectId));

        // Fetch all contact relationships
        return projectContactRepository.findByProjectId(projectEntity.getId()).stream()
                .map(ProjectContactEntity::getContact)
                .map(contactMapper::toDomain)
                .toList();
    }

    /**
     * Returns all projects associated with a contact.
     *
     * <p>Enforces per-user data isolation: only returns projects if the contact
     * belongs to the authenticated user.
     *
     * @param contactId the contact ID (string, not surrogate key)
     * @return list of projects linked to the contact (defensive copies)
     * @throws ResourceNotFoundException if contact not found for the current user
     * @throws IllegalArgumentException if contactId is null/blank
     * @throws IllegalStateException if repositories are not available
     */
    @Transactional(readOnly = true)
    public List<Project> getContactProjects(final String contactId) {
        Validation.validateNotBlank(contactId, "contactId");
        ensureRepositoriesAvailable();

        final User currentUser = getCurrentUser();

        // Verify contact exists and belongs to user
        final ContactEntity contactEntity = contactRepository
                .findByContactIdAndUser(contactId.trim(), currentUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Contact not found: " + contactId));

        // Fetch all project relationships
        return projectContactRepository.findByContactId(contactEntity.getId()).stream()
                .map(ProjectContactEntity::getProject)
                .map(entity -> new Project(
                        entity.getProjectId(),
                        entity.getName(),
                        entity.getDescription(),
                        entity.getStatus()))
                .toList();
    }

    /**
     * Ensures that required repositories and mappers are available for contact linking operations.
     *
     * @throws IllegalStateException if any required dependency is null
     */
    private void ensureRepositoriesAvailable() {
        if (projectContactRepository == null || contactRepository == null
                || projectRepository == null || contactMapper == null) {
            throw new IllegalStateException(
                    "Contact linking operations require JPA repositories and mappers; "
                            + "ensure Spring context is initialized");
        }
    }
}
