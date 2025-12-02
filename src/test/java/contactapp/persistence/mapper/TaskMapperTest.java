package contactapp.persistence.mapper;

import contactapp.domain.Task;
import contactapp.domain.TaskStatus;
import contactapp.persistence.entity.TaskEntity;
import contactapp.support.TestUserFactory;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ensures {@link TaskMapper} copies fields between domain and entity objects.
 */
class TaskMapperTest {

    private final TaskMapper mapper = new TaskMapper();

    /** Fixed timestamp for deterministic testing. */
    private static final Instant TEST_CREATED_AT = Instant.parse("2025-01-15T10:00:00Z");
    private static final Instant TEST_UPDATED_AT = Instant.parse("2025-01-15T12:30:00Z");

    /** Sets timestamps on entity to simulate @PrePersist behavior for tests. */
    private static void setEntityTimestamps(final TaskEntity entity) {
        entity.setCreatedAt(TEST_CREATED_AT);
        entity.setUpdatedAt(TEST_UPDATED_AT);
    }

    @Test
    void toEntityCopiesAllFields() {
        LocalDate dueDate = LocalDate.of(2025, 12, 31);
        Task task = new Task("task-1", "Write docs", "Document persistence layer",
                TaskStatus.IN_PROGRESS, dueDate);

        TaskEntity entity = mapper.toEntity(task, TestUserFactory.createUser("task-mapper"));

        assertThat(entity.getTaskId()).isEqualTo("task-1");
        assertThat(entity.getName()).isEqualTo("Write docs");
        assertThat(entity.getDescription()).isEqualTo("Document persistence layer");
        assertThat(entity.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(entity.getDueDate()).isEqualTo(dueDate);
        assertThat(entity.getUser()).isNotNull();
    }

    /**
     * Null inputs should short-circuit to null so callers only get defensive copies
     * when a real domain object exists.
     */
    @Test
    void toEntityReturnsNullWhenDomainIsNull() {
        assertThat(mapper.toEntity(null, TestUserFactory.createUser())).isNull();
    }

    @Test
    void toDomainRoundTripsEntity() {
        LocalDate dueDate = LocalDate.of(2025, 12, 31);
        TaskEntity entity = new TaskEntity(
                "task-1",
                "Write docs",
                "Document persistence layer",
                TaskStatus.DONE,
                dueDate,
                TestUserFactory.createUser("task-mapper-domain"));
        setEntityTimestamps(entity);

        Task task = mapper.toDomain(entity);

        assertThat(task.getTaskId()).isEqualTo("task-1");
        assertThat(task.getName()).isEqualTo("Write docs");
        assertThat(task.getDescription()).isEqualTo("Document persistence layer");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getDueDate()).isEqualTo(dueDate);
        assertThat(task.getCreatedAt()).isEqualTo(TEST_CREATED_AT);
        assertThat(task.getUpdatedAt()).isEqualTo(TEST_UPDATED_AT);
    }

    /**
     * JPA repositories can surface null rows during delete-or-insert flows, so make sure
     * the mapper handles those gracefully instead of throwing.
     */
    @Test
    void toDomainReturnsNullWhenEntityIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedToEntityReturnsNullWhenDomainNull() {
        // Intentionally testing deprecated method - verifies null handling
        assertThat(mapper.toEntity((Task) null)).isNull();
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedToEntityThrowsWhenDomainProvided() {
        Task task = new Task("legacy-1", "Name", "Desc");
        // Intentionally testing deprecated method to ensure it throws
        assertThatThrownBy(() -> mapper.toEntity(task))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updateEntityCopiesMutableFields() {
        LocalDate oldDate = LocalDate.of(2025, 11, 30);
        LocalDate newDate = LocalDate.of(2025, 12, 31);
        TaskEntity entity = new TaskEntity(
                "task-123",
                "Old",
                "Old desc",
                TaskStatus.TODO,
                oldDate,
                TestUserFactory.createUser("task-mapper-update"));
        Task updated = new Task("task-123", "New Name", "New Desc",
                TaskStatus.IN_PROGRESS, newDate);

        mapper.updateEntity(entity, updated);

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getDescription()).isEqualTo("New Desc");
        assertThat(entity.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(entity.getDueDate()).isEqualTo(newDate);
    }

    @Test
    void updateEntityThrowsWhenTargetIsNull() {
        Task source = new Task("task-1", "Name", "Desc");
        assertThatThrownBy(() -> mapper.updateEntity(null, source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target entity must not be null");
    }

    @Test
    void updateEntityThrowsWhenSourceIsNull() {
        TaskEntity entity = new TaskEntity(
                "task-1",
                "Old",
                "Old desc",
                TaskStatus.TODO,
                null,
                TestUserFactory.createUser("task-mapper-null-source"));
        assertThatThrownBy(() -> mapper.updateEntity(entity, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source task must not be null");
    }

    // ==================== Phase 2 Field Tests ====================

    @Test
    void toEntityMapsStatusField() {
        Task task = new Task("tsk-stat", "Status Test", "Testing status field",
                TaskStatus.DONE, null);

        TaskEntity entity = mapper.toEntity(task, TestUserFactory.createUser("status-test"));

        assertThat(entity.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void toEntityDefaultsToTodoWhenStatusIsNull() {
        Task task = new Task("tsk-dflt", "Default Status", "Testing default status",
                null, null);

        TaskEntity entity = mapper.toEntity(task, TestUserFactory.createUser("default-test"));

        assertThat(entity.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void toEntityMapsDueDateField() {
        LocalDate dueDate = LocalDate.of(2026, 1, 15);
        Task task = new Task("task-due", "Due Date Test", "Testing due date",
                TaskStatus.TODO, dueDate);

        TaskEntity entity = mapper.toEntity(task, TestUserFactory.createUser("due-test"));

        assertThat(entity.getDueDate()).isEqualTo(dueDate);
    }

    @Test
    void toEntityHandlesNullDueDate() {
        Task task = new Task("tsk-nodue", "No Due Date", "Testing null due date",
                TaskStatus.TODO, null);

        TaskEntity entity = mapper.toEntity(task, TestUserFactory.createUser("no-due-test"));

        assertThat(entity.getDueDate()).isNull();
    }

    @Test
    void toDomainMapsAllStatusValues() {
        int idx = 0;
        for (TaskStatus status : TaskStatus.values()) {
            TaskEntity entity = new TaskEntity(
                    "tsk-s" + idx++,
                    "Test",
                    "Testing " + status,
                    status,
                    null,
                    TestUserFactory.createUser("status-" + status));
            setEntityTimestamps(entity);

            Task task = mapper.toDomain(entity);

            assertThat(task.getStatus()).isEqualTo(status);
        }
    }

    @Test
    void updateEntityUpdatesStatus() {
        TaskEntity entity = new TaskEntity(
                "tsk-upstat",
                "Name",
                "Desc",
                TaskStatus.TODO,
                null,
                TestUserFactory.createUser("update-status"));
        Task updated = new Task("tsk-upstat", "Name", "Desc",
                TaskStatus.DONE, null);

        mapper.updateEntity(entity, updated);

        assertThat(entity.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void updateEntityUpdatesDueDate() {
        LocalDate newDate = LocalDate.of(2099, 6, 1);
        TaskEntity entity = new TaskEntity(
                "tsk-updue",
                "Name",
                "Desc",
                TaskStatus.TODO,
                LocalDate.of(2099, 12, 31),
                TestUserFactory.createUser("update-due"));
        Task updated = new Task("tsk-updue", "Name", "Desc",
                TaskStatus.TODO, newDate);

        mapper.updateEntity(entity, updated);

        assertThat(entity.getDueDate()).isEqualTo(newDate);
    }

    @Test
    void updateEntityClearsDueDateWhenNull() {
        TaskEntity entity = new TaskEntity(
                "tsk-clrdue",
                "Name",
                "Desc",
                TaskStatus.TODO,
                LocalDate.of(2099, 12, 31),
                TestUserFactory.createUser("clear-due"));
        Task updated = new Task("tsk-clrdue", "Name", "Desc",
                TaskStatus.TODO, null);

        mapper.updateEntity(entity, updated);

        assertThat(entity.getDueDate()).isNull();
    }
}
