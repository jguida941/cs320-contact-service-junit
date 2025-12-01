package contactapp.persistence.store;

import contactapp.domain.Appointment;
import contactapp.persistence.entity.AppointmentEntity;
import contactapp.persistence.mapper.AppointmentMapper;
import contactapp.persistence.repository.AppointmentRepository;
import contactapp.security.User;
import contactapp.support.TestUserFactory;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JpaAppointmentStore} delegations.
 */
@ExtendWith(MockitoExtension.class)
class JpaAppointmentStoreTest {

    @Mock
    private AppointmentRepository repository;
    @Mock
    private AppointmentMapper mapper;

    private JpaAppointmentStore store;

    @BeforeEach
    void setUp() {
        store = new JpaAppointmentStore(repository, mapper);
    }

    @Test
    void existsById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.existsById("a-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void existsById_withUserDelegatesToRepository() {
        final User owner = TestUserFactory.createUser("appt-store");
        when(repository.existsByAppointmentIdAndUser("a-2", owner)).thenReturn(true);

        assertThat(store.existsById("a-2", owner)).isTrue();
    }

    /**
     * Covers the false path for existsById(User) to keep PIT from mutating the boolean result.
     */
    @Test
    void existsById_withUserReturnsFalseWhenRepositoryReturnsFalse() {
        final User owner = TestUserFactory.createUser("appt-store-false");
        when(repository.existsByAppointmentIdAndUser("missing", owner)).thenReturn(false);

        assertThat(store.existsById("missing", owner)).isFalse();
    }

    @Test
    void findById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.findById("a-3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findById_withUserUsesMapper() {
        final User owner = TestUserFactory.createUser("appt-store-find");
        final AppointmentEntity entity = mock(AppointmentEntity.class);
        final Appointment domain = new Appointment("a-4",
                new Date(System.currentTimeMillis() + 1_000),
                "Review");
        when(repository.findByAppointmentIdAndUser("a-4", owner)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(store.findById("a-4", owner)).contains(domain);
    }

    @Test
    void deleteById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.deleteById("a-5"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deleteById_withUserUsesRepositoryReturnCode() {
        final User owner = TestUserFactory.createUser("appt-store-delete");
        when(repository.deleteByAppointmentIdAndUser("a-6", owner)).thenReturn(1);

        assertThat(store.deleteById("a-6", owner)).isTrue();
    }

    @Test
    void deleteById_withUserReturnsFalseWhenNoRowsDeleted() {
        final User owner = TestUserFactory.createUser("appt-store-delete-none");
        when(repository.deleteByAppointmentIdAndUser("nonexistent", owner)).thenReturn(0);

        assertThat(store.deleteById("nonexistent", owner)).isFalse();
    }
}
