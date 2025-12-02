package contactapp.persistence.repository;

import contactapp.persistence.entity.AppointmentEntity;
import contactapp.security.UserRepository;
import contactapp.support.TestUserFactory;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository slice tests for {@link AppointmentRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppointmentRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private AppointmentRepository repository;
    @org.springframework.beans.factory.annotation.Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindAppointment() {
        Instant instant = Instant.now().plusSeconds(86_400);
        var owner = userRepository.save(TestUserFactory.createUser("appointment-repo"));
        AppointmentEntity entity = new AppointmentEntity("appt-101", instant, "Repo Appointment", owner);

        repository.saveAndFlush(entity);

        assertThat(repository.findByAppointmentIdAndUser("appt-101", owner))
                .isPresent()
                .get()
                .extracting(AppointmentEntity::getAppointmentDate)
                .isEqualTo(instant);
    }

    @Test
    void allowsSameAppointmentIdForDifferentUsers() {
        var ownerOne = userRepository.save(TestUserFactory.createUser("appt-owner-1"));
        var ownerTwo = userRepository.save(TestUserFactory.createUser("appt-owner-2"));
        Instant when = Instant.now().plusSeconds(3600);

        repository.saveAndFlush(new AppointmentEntity("shrd-appt", when, "One", ownerOne));
        repository.saveAndFlush(new AppointmentEntity("shrd-appt", when.plusSeconds(60), "Two", ownerTwo));

        assertThat(repository.findByAppointmentIdAndUser("shrd-appt", ownerOne)).isPresent();
        assertThat(repository.findByAppointmentIdAndUser("shrd-appt", ownerTwo)).isPresent();
    }

    @Test
    void duplicateAppointmentIdForSameUserFails() {
        var owner = userRepository.save(TestUserFactory.createUser("appt-owner-dup"));
        Instant when = Instant.now().plusSeconds(7200);

        repository.saveAndFlush(new AppointmentEntity("appt-dup", when, "One", owner));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new AppointmentEntity("appt-dup", when.plusSeconds(60), "Two", owner)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
