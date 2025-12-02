package contactapp.persistence.repository;

import contactapp.persistence.entity.ContactEntity;
import contactapp.security.User;
import contactapp.security.UserRepository;
import contactapp.support.TestUserFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository slice tests for {@link ContactRepository} (H2 + Flyway schema).
 */

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ContactRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private ContactRepository repository;
    @org.springframework.beans.factory.annotation.Autowired
    private UserRepository userRepository;

    @Test
    void savePersistsContact() {
        User owner = userRepository.save(TestUserFactory.createUser("repo-contact"));
        ContactEntity entity = new ContactEntity("repo-1", "Jane", "Doe", "5555555555", "Repo Street", owner);

        repository.saveAndFlush(entity);

        assertThat(repository.findByContactIdAndUser("repo-1", owner))
                .isPresent()
                .get()
                .extracting(ContactEntity::getFirstName)
                .isEqualTo("Jane");
    }

    @Test
    void invalidPhoneFailsCheckConstraint() {
        User owner = userRepository.save(TestUserFactory.createUser("repo-contact-invalid"));
        ContactEntity entity = new ContactEntity("repo-2", "Jane", "Doe", "notdigits", "Repo Street", owner);

        assertThatThrownBy(() -> repository.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameContactIdForDifferentUsers() {
        User ownerOne = userRepository.save(TestUserFactory.createUser("contact-a"));
        User ownerTwo = userRepository.save(TestUserFactory.createUser("contact-b"));

        ContactEntity first = new ContactEntity("shared", "A", "One", "1234567890", "Addr 1", ownerOne);
        ContactEntity second = new ContactEntity("shared", "B", "Two", "0987654321", "Addr 2", ownerTwo);

        repository.saveAndFlush(first);
        repository.saveAndFlush(second);

        assertThat(repository.findByContactIdAndUser("shared", ownerOne)).isPresent();
        assertThat(repository.findByContactIdAndUser("shared", ownerTwo)).isPresent();
    }

    @Test
    void duplicateContactIdForSameUserFails() {
        User owner = userRepository.save(TestUserFactory.createUser("contact-dup"));
        ContactEntity first = new ContactEntity("dup", "First", "User", "1112223333", "Addr", owner);
        ContactEntity second = new ContactEntity("dup", "Second", "User", "4445556666", "Addr", owner);

        repository.saveAndFlush(first);

        assertThatThrownBy(() -> repository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
