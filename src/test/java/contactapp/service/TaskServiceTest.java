package contactapp.service;

import contactapp.api.exception.DuplicateResourceException;
import contactapp.domain.Task;
import contactapp.security.Role;
import contactapp.security.TestUserSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests {@link TaskService} behavior against the Spring context (H2 + Flyway).
 *
 * <p>Remaining in the same package allows direct access to {@link TaskService#clearAllTasks()}.
 */
@SpringBootTest
@ActiveProfiles("test")
public class TaskServiceTest {

    @Autowired
    private TaskService service;

    @Autowired
    private TestUserSetup testUserSetup;

    /**
     * Sets up test user and clears data before each test.
     */
    @BeforeEach
    void reset() {
        testUserSetup.setupTestUser();
        service.clearAllTasks();
    }

    @Test
    void testSingletonSharesStateWithSpringBean() {
        TaskService singleton = TaskService.getInstance();
        singleton.clearAllTasks();

        Task task = new Task("legacy10", "Legacy add", "added through getInstance");
        boolean addedViaSingleton = singleton.addTask(task);

        assertThat(addedViaSingleton).isTrue();
        assertThat(service.getTaskById("legacy10")).isPresent();
    }

    /**
     * Confirms {@link TaskService#addTask(Task)} inserts new tasks.
     */
    @Test
    void testAddTask() {
        TaskService service = this.service;
        Task task = new Task("100", "Write docs", "Document Task service");

        boolean added = service.addTask(task);

        assertThat(added).isTrue();
        assertThat(service.getDatabase()).containsKey("100");
        Task stored = service.getDatabase().get("100");
        assertThat(stored.getName()).isEqualTo("Write docs");
        assertThat(stored.getDescription()).isEqualTo("Document Task service");
    }

    /**
     * Verifies duplicate IDs are rejected and the original entry remains.
     */
    @Test
    void testAddDuplicateTaskIdFails() {
        TaskService service = this.service;
        Task original = new Task("100", "Write docs", "Document Task service");
        Task duplicate = new Task("100", "Other", "Another desc");

        assertThat(service.addTask(original)).isTrue();
        assertThatThrownBy(() -> service.addTask(duplicate))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("100");
        // Verify original data is still stored
        Task stored = service.getDatabase().get("100");
        assertThat(stored.getName()).isEqualTo("Write docs");
        assertThat(stored.getDescription()).isEqualTo("Document Task service");
    }

    /**
     * Ensures null inputs trigger the explicit IllegalArgumentException guard.
     */
    @Test
    void testAddTaskNullThrows() {
        TaskService service = this.service;

        assertThatThrownBy(() -> service.addTask(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task must not be null");
    }

    /**
     * Confirms delete removes stored tasks and returns true.
     */
    @Test
    void testDeleteTask() {
        TaskService service = this.service;
        Task task = new Task("100", "Write docs", "Document Task service");
        service.addTask(task);

        boolean deleted = service.deleteTask("100");

        assertThat(deleted).isTrue();
        assertThat(service.getDatabase()).doesNotContainKey("100");
    }

    /**
     * Ensures delete validates the incoming ID.
     */
    @Test
    void testDeleteTaskBlankIdThrows() {
        TaskService service = this.service;

        assertThatThrownBy(() -> service.deleteTask(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId must not be null or blank");
    }

    /**
     * Verifies delete returns {@code false} when the ID does not exist.
     */
    @Test
    void testDeleteMissingTaskReturnsFalse() {
        TaskService service = this.service;

        assertThat(service.deleteTask("missing")).isFalse();
    }

    /**
     * Proves {@link TaskService#clearAllTasks()} empties the backing store (PIT guard).
     */
    @Test
    void testClearAllTasksRemovesEntries() {
        TaskService service = this.service;
        service.addTask(new Task("100", "Write docs", "Document Task service"));
        service.addTask(new Task("101", "Review plan", "Double-check requirements"));

        service.clearAllTasks();

        assertThat(service.getDatabase()).isEmpty();
    }

    /**
     * Ensures {@link TaskService#updateTask(String, String, String)} updates stored tasks.
     */
    @Test
    void testUpdateTask() {
        TaskService service = this.service;
        Task task = new Task("100", "Write docs", "Document Task service");
        service.addTask(task);

        boolean updated = service.updateTask("100", "Implement feature", "Finish TaskService");

        assertThat(updated).isTrue();
        assertThat(service.getDatabase().get("100"))
                .hasFieldOrPropertyWithValue("name", "Implement feature")
                .hasFieldOrPropertyWithValue("description", "Finish TaskService");
    }

    /**
     * Confirms update trims IDs before lookups so whitespace-wrapped IDs work.
     */
    @Test
    void testUpdateTaskTrimsId() {
        TaskService service = this.service;
        Task task = new Task("200", "Write docs", "Document Task service");
        service.addTask(task);

        boolean updated = service.updateTask(" 200 ", "Polish docs", "Tighten descriptions");

        assertThat(updated).isTrue();
        assertThat(service.getDatabase().get("200"))
                .hasFieldOrPropertyWithValue("name", "Polish docs")
                .hasFieldOrPropertyWithValue("description", "Tighten descriptions");
    }

    /**
     * Ensures update validates the ID input before accessing the map.
     */
    @Test
    void testUpdateTaskBlankIdThrows() {
        TaskService service = this.service;

        assertThatThrownBy(() -> service.updateTask(" ", "Name", "Desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId must not be null or blank");
    }

    /**
     * Verifies {@link TaskService#updateTask} returns {@code false} when the ID is missing.
     */
    @Test
    void testUpdateMissingTaskReturnsFalse() {
        TaskService service = this.service;

        assertThat(service.updateTask("missing", "Name", "Desc")).isFalse();
    }

    /**
     * Confirms invalid update inputs throw and leave the stored Task unchanged.
     */
    @Test
    void testUpdateTaskInvalidValuesLeaveStateUnchanged() {
        TaskService service = this.service;
        Task task = new Task("300", "Write docs", "Original description");
        service.addTask(task);

        assertThatThrownBy(() -> service.updateTask("300", " ", "New desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name must not be null or blank");

        Task stored = service.getDatabase().get("300");
        assertThat(stored.getName()).isEqualTo("Write docs");
        assertThat(stored.getDescription()).isEqualTo("Original description");
    }

    /**
     * Ensures getDatabase returns defensive copies so external mutation cannot alter service state.
     */
    @Test
    void testGetDatabaseReturnsDefensiveCopies() {
        TaskService service = this.service;
        Task task = new Task("400", "Original Name", "Original Description");
        service.addTask(task);

        // Get a snapshot and mutate it
        Task snapshot = service.getDatabase().get("400");
        snapshot.setName("Mutated Name");
        snapshot.setDescription("Mutated Description");

        // Fetch a fresh snapshot and verify the service state is unchanged
        Task freshSnapshot = service.getDatabase().get("400");
        assertThat(freshSnapshot.getName()).isEqualTo("Original Name");
        assertThat(freshSnapshot.getDescription()).isEqualTo("Original Description");
    }

    // ==================== getTaskById Tests ====================

    /**
     * Verifies getTaskById returns the task when it exists.
     */
    @Test
    void testGetTaskByIdReturnsTask() {
        TaskService service = this.service;
        Task task = new Task("500", "Test Task", "Test Description");
        service.addTask(task);

        var result = service.getTaskById("500");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Test Task");
    }

    /**
     * Verifies getTaskById returns empty when task doesn't exist.
     */
    @Test
    void testGetTaskByIdReturnsEmptyWhenNotFound() {
        TaskService service = this.service;

        var result = service.getTaskById("nonexistent");

        assertThat(result).isEmpty();
    }

    /**
     * Verifies getTaskById throws when ID is blank.
     */
    @Test
    void testGetTaskByIdBlankIdThrows() {
        TaskService service = this.service;

        assertThatThrownBy(() -> service.getTaskById(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId must not be null or blank");
    }

    /**
     * Verifies getTaskById trims the ID before lookup.
     */
    @Test
    void testGetTaskByIdTrimsId() {
        TaskService service = this.service;
        Task task = new Task("600", "Trimmed Task", "Test Description");
        service.addTask(task);

        var result = service.getTaskById(" 600 ");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Trimmed Task");
    }

    // ==================== getAllTasks Tests ====================

    /**
     * Verifies getAllTasks returns empty list when no tasks exist.
     */
    @Test
    void testGetAllTasksReturnsEmptyList() {
        TaskService service = this.service;

        var result = service.getAllTasks();

        assertThat(result).isEmpty();
    }

    /**
     * Verifies getAllTasks returns all tasks.
     */
    @Test
    void testGetAllTasksReturnsAllTasks() {
        TaskService service = this.service;
        service.addTask(new Task("701", "First Task", "First Description"));
        service.addTask(new Task("702", "Second Task", "Second Description"));

        var result = service.getAllTasks();

        assertThat(result).hasSize(2);
    }

    @Test
    void getAllTasksAllUsers_requiresAdminRole() {
        runAs("limited", Role.USER, () ->
                service.addTask(new Task("task-1", "Task", "User owned")));

        assertThatThrownBy(service::getAllTasksAllUsers)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only ADMIN users can access all tasks");
    }

    @Test
    void getAllTasksAllUsers_returnsDataForAdmins() {
        runAs("user-one", Role.USER, () ->
                service.addTask(new Task("tu-1", "User One", "Task One")));
        runAs("user-two", Role.USER, () ->
                service.addTask(new Task("tu-2", "User Two", "Task Two")));

        runAs("admin", Role.ADMIN, () -> assertThat(service.getAllTasksAllUsers())
                .extracting(Task::getTaskId)
                .contains("tu-1", "tu-2"));
    }

    @Test
    void getTaskById_onlyReturnsCurrentUsersTasks() {
        runAs("owner", Role.USER, () ->
                service.addTask(new Task("shared01", "Owner Task", "Owned by user")));

        runAs("other", Role.USER, () ->
                assertThat(service.getTaskById("shared01")).isEmpty());
    }

    @Test
    void deleteTask_doesNotAllowOtherUsersToDelete() {
        runAs("owner", Role.USER, () ->
                service.addTask(new Task("locked01", "Locked", "Should stay")));

        runAs("other", Role.USER, () ->
                assertThat(service.deleteTask("locked01")).isFalse());
    }

    @Test
    void updateTask_doesNotAllowCrossUserModification() {
        runAs("owner", Role.USER, () ->
                service.addTask(new Task("owned01", "Owned", "Original")));

        runAs("other", Role.USER, () ->
                assertThat(service.updateTask("owned01", "Edited", "New description"))
                        .isFalse());
    }

    private void runAs(final String username, final Role role, final Runnable action) {
        testUserSetup.setupTestUser(username, username + "@example.com", role);
        action.run();
    }
}
