package contactapp.service;

import contactapp.domain.Task;
import contactapp.persistence.store.TaskStore;
import java.lang.reflect.Constructor;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests targeting the legacy fallback path inside {@link TaskService#addTask(Task)}.
 *
 * <p>PIT previously reported that the {@code return attemptLegacySave(task)} statement at the end of
 * {@link TaskService#addTask(Task)} could be replaced with {@code return true} without failing any
 * tests. That mutation removed the {@code TaskStore#save(Task)} invocation entirely, so this test
 * constructs a legacy-style {@link TaskService} and asserts the store interaction happens.
 */
class TaskServiceFallbackTest {

    @Test
    void addTaskInvokesLegacyStoreSave() throws Exception {
        final TaskStore store = mock(TaskStore.class);
        when(store.existsById("LEG-99")).thenReturn(false);
        final TaskService legacyService = createLegacyService(store);

        final Task task = new Task("LEG-99", "Legacy", "Fallback coverage");
        assertThat(legacyService.addTask(task)).isTrue();

        final ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(store).save(captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo("LEG-99");
    }

    private static TaskService createLegacyService(final TaskStore store) throws Exception {
        final Constructor<TaskService> constructor =
                TaskService.class.getDeclaredConstructor(TaskStore.class, boolean.class);
        constructor.setAccessible(true);
        return constructor.newInstance(store, true);
    }
}
