package contactapp.service;

import contactapp.api.exception.DuplicateResourceException;
import contactapp.domain.Appointment;
import contactapp.persistence.store.AppointmentStore;
import contactapp.security.Role;
import contactapp.security.User;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentServiceMutationTest {

    private static final String PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOBiZFn0eTD1yQbFq6Z9erjRzCQXDpG7W";

    @AfterEach
    void resetSingleton() {
        ReflectionTestUtils.setField(AppointmentService.class, "instance", null);
        SecurityContextHolder.clearContext();
    }

    @Test
    void addAppointmentThrowsDuplicateWhenStoreReportsExistingId() {
        AppointmentStore store = mock(AppointmentStore.class);
        AppointmentService service = new AppointmentService(store);
        Appointment appointment = new Appointment("appt-dup", new Date(System.currentTimeMillis() + 1_000), "Persist Failure");

        when(store.existsById("appt-dup")).thenReturn(true);

        assertThatThrownBy(() -> service.addAppointment(appointment))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getAllAppointmentsAllUsersThrowsForNonAdmins() {
        final AppointmentService service = new AppointmentService(mock(AppointmentStore.class));
        authenticate(Role.USER);

        assertThatThrownBy(service::getAllAppointmentsAllUsers)
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void getAllAppointmentsAllUsersReturnsCopiesForAdmins() {
        final AppointmentStore store = mock(AppointmentStore.class);
        final AppointmentService service = new AppointmentService(store);
        authenticate(Role.ADMIN);
        final Appointment stored = new Appointment(
                "adm-1",
                new Date(System.currentTimeMillis() + 1_000),
                "Admin");
        when(store.findAll()).thenReturn(List.of(stored));

        assertThat(service.getAllAppointmentsAllUsers()).singleElement().isNotSameAs(stored);
    }

    @Test
    void deleteAppointmentReturnsStoreResult() {
        final AppointmentStore store = mock(AppointmentStore.class);
        final AppointmentService service = new AppointmentService(store);
        when(store.deleteById("adm-1")).thenReturn(true);

        assertThat(service.deleteAppointment("adm-1")).isTrue();
        assertThat(service.deleteAppointment("missing")).isFalse();
    }

    @Test
    void updateAppointmentReturnsFalseWhenMissing() {
        final AppointmentStore store = mock(AppointmentStore.class);
        final AppointmentService service = new AppointmentService(store);
        when(store.findById("missing")).thenReturn(Optional.empty());

        assertThat(service.updateAppointment("missing",
                new Date(System.currentTimeMillis() + 1_000), "Desc")).isFalse();
    }

    @Test
    void updateAppointmentMutatesExistingRecord() {
        final AppointmentStore store = mock(AppointmentStore.class);
        final AppointmentService service = new AppointmentService(store);
        final Appointment existing = new Appointment(
                "adm-5",
                new Date(System.currentTimeMillis() + 1_000),
                "Original");
        when(store.findById("adm-5")).thenReturn(Optional.of(existing));

        assertThat(service.updateAppointment("adm-5",
                new Date(System.currentTimeMillis() + 2_000),
                "Updated")).isTrue();

        final ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(store).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Updated");
    }

    @Test
    void getAppointmentByIdReturnsCopyFromStore() {
        final AppointmentStore store = mock(AppointmentStore.class);
        final AppointmentService service = new AppointmentService(store);
        final Appointment stored = new Appointment(
                "adm-7",
                new Date(System.currentTimeMillis() + 1_000),
                "Original");
        when(store.findById("adm-7")).thenReturn(Optional.of(stored));

        assertThat(service.getAppointmentById("adm-7")).isPresent().get().isNotSameAs(stored);
    }

    private void authenticate(final Role role) {
        final User user = new User(role.name().toLowerCase(), role.name().toLowerCase() + "@example.com", PASSWORD_HASH, role);
        final var token = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
