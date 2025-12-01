package contactapp.persistence.repository;

import contactapp.persistence.entity.TaskEntity;
import contactapp.security.UserRepository;
import contactapp.support.TestUserFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Repository slice tests for {@link TaskRepository}.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class TaskRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private TaskRepository repository;
    @org.springframework.beans.factory.annotation.Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindTask() {
        var owner = userRepository.save(TestUserFactory.createUser("task-repo"));
        TaskEntity entity = new TaskEntity("task-101", "Repo Task", "Persist via repository", owner);

        repository.saveAndFlush(entity);

        assertThat(repository.findByTaskIdAndUser("task-101", owner))
                .isPresent()
                .get()
                .extracting(TaskEntity::getDescription)
                .isEqualTo("Persist via repository");
    }

    @Test
    void allowsSameTaskIdForDifferentUsers() {
        var ownerOne = userRepository.save(TestUserFactory.createUser("task-owner-1"));
        var ownerTwo = userRepository.save(TestUserFactory.createUser("task-owner-2"));

        repository.saveAndFlush(new TaskEntity("shrd-task", "First", "Desc", ownerOne));
        repository.saveAndFlush(new TaskEntity("shrd-task", "Second", "Desc", ownerTwo));

        assertThat(repository.findByTaskIdAndUser("shrd-task", ownerOne)).isPresent();
        assertThat(repository.findByTaskIdAndUser("shrd-task", ownerTwo)).isPresent();
    }

    @Test
    void duplicateTaskIdForSameUserFails() {
        var owner = userRepository.save(TestUserFactory.createUser("task-owner-dup"));
        repository.saveAndFlush(new TaskEntity("uniq-task", "One", "Desc", owner));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new TaskEntity("uniq-task", "Two", "Desc", owner)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
