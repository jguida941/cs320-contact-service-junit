package contactapp;

import contactapp.api.TaskController;
import contactapp.api.dto.TaskResponse;
import contactapp.domain.Task;
import contactapp.service.TaskService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests that exercise {@link TaskController#getAll} so
 * PIT observes both the admin and non-admin branches.
 */
class TaskControllerUnitTest {

    private final TaskService taskService = mock(TaskService.class);
    private final TaskController controller = new TaskController(taskService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAll_withAllFlagThrowsForNonAdmins() {
        authenticate(false);

        assertThatThrownBy(() -> controller.getAll(true, null, null, false, null, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only administrators");
    }

    @Test
    void getAll_withAllFlagReturnsResultsForAdmins() {
        authenticate(true);
        when(taskService.getAllTasksAllUsers()).thenReturn(List.of(
                new Task("t-1", "Admin Task", "Visible to admins")));

        final List<TaskResponse> responses = controller.getAll(true, null, null, false, null, null);

        verify(taskService).getAllTasksAllUsers();
        assertThat(responses).singleElement().satisfies(response ->
                assertThat(response.id()).isEqualTo("t-1"));
    }

    private void authenticate(final boolean admin) {
        final var authorities = admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        final var authentication = new UsernamePasswordAuthenticationToken(
                "user", null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
