package contactapp.persistence.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import contactapp.domain.Task;
import contactapp.persistence.entity.TaskEntity;
import contactapp.persistence.mapper.TaskMapper;
import contactapp.persistence.repository.TaskRepository;
import contactapp.security.User;
import contactapp.support.TestUserFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JpaTaskStore} delegations that previously showed up as
 * NO_COVERAGE in PIT. By asserting each branch calls the repository/mapper we
 * make it impossible for PIT to replace the boolean results without detection.
 */
@ExtendWith(MockitoExtension.class)
class JpaTaskStoreTest {

    @Mock
    private TaskRepository repository;
    @Mock
    private TaskMapper mapper;

    private JpaTaskStore store;

    @BeforeEach
    void setUp() {
        store = new JpaTaskStore(repository, mapper);
    }

    @Test
    void existsById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.existsById("task-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.findById("task-2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deleteById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.deleteById("task-3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void existsById_withUserDelegatesToRepository() {
        final User owner = TestUserFactory.createUser("repo-task");
        when(repository.existsByTaskIdAndUser("task-5", owner)).thenReturn(true);

        assertThat(store.existsById("task-5", owner)).isTrue();
    }

    /**
     * Covers the false path for existsById(User) so PIT can't always return true.
     */
    @Test
    void existsById_withUserReturnsFalseWhenRepositoryReturnsFalse() {
        final User owner = TestUserFactory.createUser("repo-task-missing");
        when(repository.existsByTaskIdAndUser("missing", owner)).thenReturn(false);

        assertThat(store.existsById("missing", owner)).isFalse();
    }

    @Test
    void findById_withUserUsesMapper() {
        final User owner = TestUserFactory.createUser("repo-task-find");
        final TaskEntity entity = mock(TaskEntity.class);
        final Task domain = new Task("task-6", "Task", "Desc");
        when(repository.findByTaskIdAndUser("task-6", owner)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(store.findById("task-6", owner)).contains(domain);
    }

    @Test
    void deleteById_withUserReturnsTrueWhenRowsDeleted() {
        final User owner = TestUserFactory.createUser("repo-task-delete");
        when(repository.deleteByTaskIdAndUser("task-7", owner)).thenReturn(1);

        assertThat(store.deleteById("task-7", owner)).isTrue();
    }

    @Test
    void deleteById_withUserReturnsFalseWhenNoRowsDeleted() {
        final User owner = TestUserFactory.createUser("repo-task-delete-none");
        when(repository.deleteByTaskIdAndUser("nonexistent", owner)).thenReturn(0);

        assertThat(store.deleteById("nonexistent", owner)).isFalse();
    }
}
