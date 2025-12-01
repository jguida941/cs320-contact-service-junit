package contactapp;

import contactapp.api.AppointmentController;
import contactapp.domain.Appointment;
import contactapp.service.AppointmentService;
import java.util.Date;
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
 * Unit tests for {@link AppointmentController#getAll(boolean)} hitting both the
 * admin and non-admin branches for mutation coverage.
 */
class AppointmentControllerUnitTest {

    private final AppointmentService appointmentService = mock(AppointmentService.class);
    private final AppointmentController controller = new AppointmentController(appointmentService);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAll_withAllFlagRejectsNonAdmins() {
        authenticate(false);

        assertThatThrownBy(() -> controller.getAll(true))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only administrators");
    }

    @Test
    void getAll_withAllFlagReturnsAdminData() {
        authenticate(true);
        final Appointment appointment = new Appointment(
                "admin-appt",
                new Date(System.currentTimeMillis() + 86_400_000L),
                "Admin review");
        when(appointmentService.getAllAppointmentsAllUsers()).thenReturn(List.of(appointment));

        assertThat(controller.getAll(true)).singleElement().satisfies(response ->
                assertThat(response.id()).isEqualTo("admin-appt"));
        verify(appointmentService).getAllAppointmentsAllUsers();
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
