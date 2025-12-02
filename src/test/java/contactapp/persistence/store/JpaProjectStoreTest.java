package contactapp.persistence.store;

import contactapp.domain.Project;
import contactapp.domain.ProjectStatus;
import contactapp.persistence.entity.ProjectEntity;
import contactapp.persistence.mapper.ProjectMapper;
import contactapp.persistence.repository.ProjectRepository;
import contactapp.security.User;
import contactapp.support.TestUserFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link JpaProjectStore}'s repository delegations so PIT can't remove them.
 */
@ExtendWith(MockitoExtension.class)
class JpaProjectStoreTest {

    @Mock
    private ProjectRepository repository;
    @Mock
    private ProjectMapper mapper;

    private JpaProjectStore store;

    @BeforeEach
    void setUp() {
        store = new JpaProjectStore(repository, mapper);
    }

    @Test
    @SuppressWarnings("deprecation")
    void existsById_withoutUserThrowsUnsupportedOperation() {
        // Intentionally testing deprecated method to ensure it throws
        assertThatThrownBy(() -> store.existsById("p-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void existsById_withUserDelegatesToRepository() {
        final User owner = TestUserFactory.createUser("project-store");
        when(repository.existsByProjectIdAndUser("p-2", owner)).thenReturn(true);

        assertThat(store.existsById("p-2", owner)).isTrue();
    }

    /**
     * Covers the false branch so PIT can't flip the result of existsById(User).
     */
    @Test
    void existsById_withUserReturnsFalseWhenRepositoryReturnsFalse() {
        final User owner = TestUserFactory.createUser("project-store-false");
        when(repository.existsByProjectIdAndUser("missing", owner)).thenReturn(false);

        assertThat(store.existsById("missing", owner)).isFalse();
    }

    @Test
    @SuppressWarnings("deprecation")
    void findById_withoutUserThrowsUnsupportedOperation() {
        // Intentionally testing deprecated method to ensure it throws
        assertThatThrownBy(() -> store.findById("p-3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findById_withUserUsesMapper() {
        final User owner = TestUserFactory.createUser("project-store-find");
        final ProjectEntity entity = mock(ProjectEntity.class);
        final Project domain = new Project("p-4", "Alpha", "Description", ProjectStatus.ACTIVE);
        when(repository.findByProjectIdAndUser("p-4", owner)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(store.findById("p-4", owner)).contains(domain);
    }

    @Test
    @SuppressWarnings("deprecation")
    void deleteById_withoutUserThrowsUnsupportedOperation() {
        // Intentionally testing deprecated method to ensure it throws
        assertThatThrownBy(() -> store.deleteById("p-5"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deleteById_withUserUsesRepositoryReturnCode() {
        final User owner = TestUserFactory.createUser("project-store-delete");
        when(repository.deleteByProjectIdAndUser("p-6", owner)).thenReturn(1);

        assertThat(store.deleteById("p-6", owner)).isTrue();
    }

    @Test
    void deleteById_withUserReturnsFalseWhenNoRowsDeleted() {
        final User owner = TestUserFactory.createUser("project-store-delete-none");
        when(repository.deleteByProjectIdAndUser("nonexistent", owner)).thenReturn(0);

        assertThat(store.deleteById("nonexistent", owner)).isFalse();
    }

    // --- Null parameter validation tests ---

    @Test
    void existsById_withNullUserThrowsIllegalArgument() {
        assertThatThrownBy(() -> store.existsById("p-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user must not be null");
    }

    @Test
    void findById_withNullUserThrowsIllegalArgument() {
        assertThatThrownBy(() -> store.findById("p-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user must not be null");
    }

    @Test
    void findAll_withNullUserThrowsIllegalArgument() {
        assertThatThrownBy(() -> store.findAll(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user must not be null");
    }

    @Test
    void deleteById_withNullUserThrowsIllegalArgument() {
        assertThatThrownBy(() -> store.deleteById("p-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user must not be null");
    }

    @Test
    void save_withNullAggregateThrowsIllegalArgument() {
        final User owner = TestUserFactory.createUser("project-store-null-agg");
        assertThatThrownBy(() -> store.save(null, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("project aggregate must not be null");
    }

    @Test
    void save_withNullUserThrowsIllegalArgument() {
        final Project project = new Project("p-1", "Alpha", "Description", ProjectStatus.ACTIVE);
        assertThatThrownBy(() -> store.save(project, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user must not be null");
    }

    @Test
    @SuppressWarnings("deprecation")
    void save_withoutUserThrowsUnsupportedOperation() {
        final Project project = new Project("p-1", "Alpha", "Description", ProjectStatus.ACTIVE);
        // Intentionally testing deprecated method to ensure it throws
        assertThatThrownBy(() -> store.save(project))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void insert_withNullAggregateThrowsIllegalArgument() {
        final User owner = TestUserFactory.createUser("project-store-insert-null");
        assertThatThrownBy(() -> store.insert(null, owner))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("project aggregate must not be null");
    }

    @Test
    void insert_withNullUserThrowsIllegalArgument() {
        final Project project = new Project("p-1", "Alpha", "Description", ProjectStatus.ACTIVE);
        assertThatThrownBy(() -> store.insert(project, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user must not be null");
    }

    // --- Save and insert behavior tests ---

    @Test
    void save_updatesExistingProject() {
        final User owner = TestUserFactory.createUser("project-store-update");
        final Project project = new Project("p-upd", "Updated", "New description", ProjectStatus.COMPLETED);
        final ProjectEntity existingEntity = mock(ProjectEntity.class);
        when(repository.findByProjectIdAndUser("p-upd", owner)).thenReturn(Optional.of(existingEntity));

        store.save(project, owner);

        verify(mapper).updateEntity(existingEntity, project);
        verify(repository).save(existingEntity);
    }

    @Test
    void save_insertsNewProject() {
        final User owner = TestUserFactory.createUser("project-store-insert-new");
        final Project project = new Project("p-new", "New Project", "Description", ProjectStatus.ACTIVE);
        final ProjectEntity newEntity = mock(ProjectEntity.class);
        when(repository.findByProjectIdAndUser("p-new", owner)).thenReturn(Optional.empty());
        when(mapper.toEntity(project, owner)).thenReturn(newEntity);

        store.save(project, owner);

        verify(repository).save(newEntity);
    }

    @Test
    void insert_savesNewEntity() {
        final User owner = TestUserFactory.createUser("project-store-insert");
        final Project project = new Project("p-ins", "Insert Test", "Description", ProjectStatus.ACTIVE);
        final ProjectEntity entity = mock(ProjectEntity.class);
        when(mapper.toEntity(project, owner)).thenReturn(entity);

        store.insert(project, owner);

        verify(repository).save(entity);
    }

    // --- findAll tests ---

    @Test
    void findAll_withUserReturnsMappedProjects() {
        final User owner = TestUserFactory.createUser("project-store-findall");
        final ProjectEntity entity1 = mock(ProjectEntity.class);
        final ProjectEntity entity2 = mock(ProjectEntity.class);
        final Project project1 = new Project("p-1", "Alpha", "Desc 1", ProjectStatus.ACTIVE);
        final Project project2 = new Project("p-2", "Beta", "Desc 2", ProjectStatus.ON_HOLD);
        when(repository.findByUser(owner)).thenReturn(List.of(entity1, entity2));
        when(mapper.toDomain(entity1)).thenReturn(project1);
        when(mapper.toDomain(entity2)).thenReturn(project2);

        final List<Project> result = store.findAll(owner);

        assertThat(result).containsExactly(project1, project2);
    }

    @Test
    void findAll_adminReturnsMappedProjects() {
        final ProjectEntity entity = mock(ProjectEntity.class);
        final Project project = new Project("p-admin", "Admin View", "Description", ProjectStatus.COMPLETED);
        when(repository.findAll()).thenReturn(List.of(entity));
        when(mapper.toDomain(entity)).thenReturn(project);

        final List<Project> result = store.findAll();

        assertThat(result).containsExactly(project);
    }

    @Test
    void findById_withUserReturnsEmptyWhenNotFound() {
        final User owner = TestUserFactory.createUser("project-store-find-empty");
        when(repository.findByProjectIdAndUser("missing", owner)).thenReturn(Optional.empty());

        assertThat(store.findById("missing", owner)).isEmpty();
    }

    @Test
    void deleteAll_delegatesToRepository() {
        store.deleteAll();

        verify(repository).deleteAll();
    }
}
