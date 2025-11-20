package contactapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests TaskService behavior mirrors the requirements.
 */
public class TaskServiceTest {

    @BeforeEach
    void reset() {
        TaskService.getInstance().clearAllTasks();
    }

    @Test
    void testSingletonInstance() {
        TaskService first = TaskService.getInstance();
        TaskService second = TaskService.getInstance();

        assertThat(first).isSameAs(second);
    }

    @Test
    void testAddTask() {
        TaskService service = TaskService.getInstance();
        Task task = new Task("100", "Write docs", "Document Task service");

        boolean added = service.addTask(task);

        assertThat(added).isTrue();
        assertThat(service.getDatabase()).containsEntry("100", task);
    }

    @Test
    void testAddDuplicateTaskIdFails() {
        TaskService service = TaskService.getInstance();
        Task original = new Task("100", "Write docs", "Document Task service");
        Task duplicate = new Task("100", "Other", "Another desc");

        assertThat(service.addTask(original)).isTrue();
        assertThat(service.addTask(duplicate)).isFalse();
        assertThat(service.getDatabase()).containsEntry("100", original);
    }

    @Test
    void testAddTaskNullThrows() {
        TaskService service = TaskService.getInstance();

        assertThatThrownBy(() -> service.addTask(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task must not be null");
    }

    @Test
    void testDeleteTask() {
        TaskService service = TaskService.getInstance();
        Task task = new Task("100", "Write docs", "Document Task service");
        service.addTask(task);

        boolean deleted = service.deleteTask("100");

        assertThat(deleted).isTrue();
        assertThat(service.getDatabase()).doesNotContainKey("100");
    }

    @Test
    void testDeleteTaskBlankIdThrows() {
        TaskService service = TaskService.getInstance();

        assertThatThrownBy(() -> service.deleteTask(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId must not be null or blank");
    }

    @Test
    void testDeleteMissingTaskReturnsFalse() {
        TaskService service = TaskService.getInstance();

        assertThat(service.deleteTask("missing")).isFalse();
    }

    @Test
    // Proves clearAllTasks() actually empties the map so mutation tests can detect a missing Map.clear().
    void testClearAllTasksRemovesEntries() {
        TaskService service = TaskService.getInstance();
        service.addTask(new Task("100", "Write docs", "Document Task service"));
        service.addTask(new Task("101", "Review plan", "Double-check requirements"));

        service.clearAllTasks();

        assertThat(service.getDatabase()).isEmpty();
    }

    @Test
    void testUpdateTask() {
        TaskService service = TaskService.getInstance();
        Task task = new Task("100", "Write docs", "Document Task service");
        service.addTask(task);

        boolean updated = service.updateTask("100", "Implement feature", "Finish TaskService");

        assertThat(updated).isTrue();
        assertThat(service.getDatabase().get("100"))
                .hasFieldOrPropertyWithValue("name", "Implement feature")
                .hasFieldOrPropertyWithValue("description", "Finish TaskService");
    }

    @Test
    void testUpdateTaskTrimsId() {
        TaskService service = TaskService.getInstance();
        Task task = new Task("200", "Write docs", "Document Task service");
        service.addTask(task);

        boolean updated = service.updateTask(" 200 ", "Polish docs", "Tighten descriptions");

        assertThat(updated).isTrue();
        assertThat(service.getDatabase().get("200"))
                .hasFieldOrPropertyWithValue("name", "Polish docs")
                .hasFieldOrPropertyWithValue("description", "Tighten descriptions");
    }

    @Test
    void testUpdateTaskBlankIdThrows() {
        TaskService service = TaskService.getInstance();

        assertThatThrownBy(() -> service.updateTask(" ", "Name", "Desc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId must not be null or blank");
    }

    @Test
    void testUpdateMissingTaskReturnsFalse() {
        TaskService service = TaskService.getInstance();

        assertThat(service.updateTask("missing", "Name", "Desc")).isFalse();
    }
}
