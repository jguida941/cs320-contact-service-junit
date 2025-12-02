package contactapp.persistence.repository;

import contactapp.domain.ProjectStatus;
import contactapp.persistence.entity.ProjectEntity;
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
 * Repository slice tests for {@link ProjectRepository} (H2 + Flyway schema).
 */

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProjectRepositoryTest {

    @org.springframework.beans.factory.annotation.Autowired
    private ProjectRepository repository;
    @org.springframework.beans.factory.annotation.Autowired
    private UserRepository userRepository;

    @Test
    void savePersistsProject() {
        User owner = userRepository.save(TestUserFactory.createUser("repo-project"));
        ProjectEntity entity = new ProjectEntity("repo-1", "Alpha Project", "Initial project", ProjectStatus.ACTIVE, owner);

        repository.saveAndFlush(entity);

        assertThat(repository.findByProjectIdAndUser("repo-1", owner))
                .isPresent()
                .get()
                .extracting(ProjectEntity::getName)
                .isEqualTo("Alpha Project");
    }

    @Test
    void invalidStatusFailsCheckConstraint() {
        User owner = userRepository.save(TestUserFactory.createUser("repo-project-invalid"));
        ProjectEntity entity = new ProjectEntity("repo-2", "Beta Project", "Description", ProjectStatus.ACTIVE, owner);
        
        // Manually set invalid status to test database constraint
        entity.setStatus(null);

        assertThatThrownBy(() -> repository.saveAndFlush(entity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameProjectIdForDifferentUsers() {
        User ownerOne = userRepository.save(TestUserFactory.createUser("project-a"));
        User ownerTwo = userRepository.save(TestUserFactory.createUser("project-b"));

        ProjectEntity first = new ProjectEntity("shared", "Project A", "Description A", ProjectStatus.ACTIVE, ownerOne);
        ProjectEntity second = new ProjectEntity("shared", "Project B", "Description B", ProjectStatus.ON_HOLD, ownerTwo);

        repository.saveAndFlush(first);
        repository.saveAndFlush(second);

        assertThat(repository.findByProjectIdAndUser("shared", ownerOne)).isPresent();
        assertThat(repository.findByProjectIdAndUser("shared", ownerTwo)).isPresent();
    }

    @Test
    void duplicateProjectIdForSameUserFails() {
        User owner = userRepository.save(TestUserFactory.createUser("project-dup"));
        ProjectEntity first = new ProjectEntity("dup", "First Project", "Desc 1", ProjectStatus.ACTIVE, owner);
        ProjectEntity second = new ProjectEntity("dup", "Second Project", "Desc 2", ProjectStatus.COMPLETED, owner);

        repository.saveAndFlush(first);

        assertThatThrownBy(() -> repository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
