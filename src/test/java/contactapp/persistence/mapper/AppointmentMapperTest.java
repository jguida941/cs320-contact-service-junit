package contactapp.persistence.mapper;

import contactapp.domain.Appointment;
import contactapp.persistence.entity.AppointmentEntity;
import contactapp.support.TestUserFactory;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link AppointmentMapper} converts between {@link java.util.Date} and {@link Instant}.
 */
class AppointmentMapperTest {

    private final AppointmentMapper mapper = new AppointmentMapper();

    @Test
    void toEntityConvertsDateToInstant() {
        Date futureDate = Date.from(Instant.now().plusSeconds(3_600));
        Appointment appointment = new Appointment("appt-1", futureDate, "Check-in");
        appointment.setProjectId("proj-entity");
        appointment.setTaskId("task-entity");

        AppointmentEntity entity = mapper.toEntity(appointment, TestUserFactory.createUser("appointment-mapper"));

        assertThat(entity.getAppointmentId()).isEqualTo("appt-1");
        assertThat(entity.getAppointmentDate()).isEqualTo(futureDate.toInstant());
        assertThat(entity.getDescription()).isEqualTo("Check-in");
        assertThat(entity.getProjectId()).isEqualTo("proj-entity");
        assertThat(entity.getTaskId()).isEqualTo("task-entity");
    }

    @Test
    void toDomainConvertsInstantToDate() {
        Instant instant = Instant.now().plusSeconds(7_200);
        AppointmentEntity entity = new AppointmentEntity(
                "appt-1",
                instant,
                "Check-in",
                TestUserFactory.createUser("appointment-mapper-domain"));
        entity.setProjectId("proj-domain");
        entity.setTaskId("task-domain");

        Appointment appointment = mapper.toDomain(entity);

        assertThat(appointment.getAppointmentId()).isEqualTo("appt-1");
        assertThat(appointment.getAppointmentDate().toInstant())
                .isEqualTo(instant.truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(appointment.getDescription()).isEqualTo("Check-in");
        assertThat(appointment.getProjectId()).isEqualTo("proj-domain");
        assertThat(appointment.getTaskId()).isEqualTo("task-domain");
    }

    /**
     * Null appointments (legacy fallback paths) should stay null instead of creating entities
     * with default-initialized fields.
     */
    @Test
    void toEntityReturnsNullWhenDomainIsNull() {
        assertThat(mapper.toEntity(null, TestUserFactory.createUser())).isNull();
    }

    /**
     * Null JPA rows should not blow up when mapping back to the domain layer.
     */
    @Test
    void toDomainReturnsNullWhenEntityIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    /**
     * Guard against null timestamps from JPA (should never happen, but the mapper
     * defends to keep downstream services predictable).
     */
    @Test
    void toDomainThrowsWhenEntityDateMissing() throws Exception {
        AppointmentEntity entity = new AppointmentEntity(
                "appt-3",
                Instant.now(),
                "Missing instant",
                TestUserFactory.createUser("appointment-mapper-null"));
        setField(entity, "appointmentDate", null);

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("appointmentDate");
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedToEntityReturnsNullWhenDomainNull() {
        // Intentionally testing deprecated method - verifies null handling
        assertThat(mapper.toEntity((Appointment) null)).isNull();
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedToEntityThrowsWhenDomainProvided() {
        Appointment appointment = new Appointment(
                "legacy",
                new Date(System.currentTimeMillis() + 1_000),
                "Desc");
        // Intentionally testing deprecated method to ensure it throws
        assertThatThrownBy(() -> mapper.toEntity(appointment))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updateEntityCopiesMutableFields() {
        AppointmentEntity entity = new AppointmentEntity(
                "appt-77",
                Instant.now().plusSeconds(600),
                "Old Desc",
                TestUserFactory.createUser("appointment-mapper-update"));
        Appointment updated = new Appointment(
                "appt-77",
                new Date(System.currentTimeMillis() + 3_600_000),
                "New Desc");

        mapper.updateEntity(entity, updated);

        assertThat(entity.getAppointmentDate()).isEqualTo(updated.getAppointmentDate().toInstant());
        assertThat(entity.getDescription()).isEqualTo("New Desc");
    }

    @Test
    void updateEntityThrowsWhenTargetIsNull() {
        Appointment source = new Appointment(
                "appt-1",
                new Date(System.currentTimeMillis() + 1_000),
                "Desc");
        assertThatThrownBy(() -> mapper.updateEntity(null, source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target entity must not be null");
    }

    @Test
    void updateEntityThrowsWhenSourceIsNull() {
        AppointmentEntity entity = new AppointmentEntity(
                "appt-1",
                Instant.now().plusSeconds(600),
                "Old Desc",
                TestUserFactory.createUser("appointment-mapper-null-source"));
        assertThatThrownBy(() -> mapper.updateEntity(entity, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source appointment must not be null");
    }

    private static void setField(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final IllegalAccessException | NoSuchFieldException ex) {
            throw new AssertionError(ex);
        }
    }
}
