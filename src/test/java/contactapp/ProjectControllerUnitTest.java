package contactapp;

import contactapp.api.ProjectController;
import contactapp.api.dto.ProjectResponse;
import contactapp.domain.Project;
import contactapp.domain.ProjectStatus;
import contactapp.service.ProjectService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for {@link ProjectController#getAll(boolean, Authentication)} to ensure the
 * controller-level ADMIN guard remains enforced even though {@link contactapp.service.ProjectService}
 * performs a secondary check. PIT left mutants in {@code isAdmin} alive because the service guard
 * masked the behavior; these tests assert the controller still blocks non-admin callers before the
 * service is invoked.
 */
class ProjectControllerUnitTest {

    private final ProjectService projectService = mock(ProjectService.class);
    private final ProjectController controller = new ProjectController(projectService);

    @Test
    void getAll_withAllFlagDelegatesToAdminService() {
        final Project project = new Project("proj-admin", "Admin", "desc", ProjectStatus.ACTIVE);
        when(projectService.getAllProjectsAllUsers()).thenReturn(List.of(project));
        final Authentication adminAuth = mockAuthentication("ROLE_ADMIN");

        final List<ProjectResponse> responses = controller.getAll(true, adminAuth);

        verify(projectService).getAllProjectsAllUsers();
        assertThat(responses).singleElement().satisfies(response ->
                assertThat(response.id()).isEqualTo("proj-admin"));
    }

    @Test
    void getAll_withAllFlagNonAdminThrows() {
        final Authentication userAuth = mockAuthentication("ROLE_USER");

        assertThatThrownBy(() -> controller.getAll(true, userAuth))
                .isInstanceOf(AccessDeniedException.class);

        verify(projectService, never()).getAllProjectsAllUsers();
    }

    private Authentication mockAuthentication(final String role) {
        final Authentication auth = mock(Authentication.class);
        final List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority(role));
        org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();
        return auth;
    }
}
