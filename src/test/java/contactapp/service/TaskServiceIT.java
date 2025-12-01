package contactapp.service;

import contactapp.domain.Task;
import contactapp.persistence.entity.TaskEntity;
import contactapp.persistence.repository.TaskRepository;
import contactapp.security.User;
import contactapp.security.UserRepository;
import contactapp.support.TestUserFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Full-stack TaskService tests against Postgres (Testcontainers).
 */
@SpringBootTest
@ActiveProfiles("integration")
@Testcontainers
class TaskServiceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUpContext() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Transactional
    void addTaskPersistsRecord() {
        User owner = authenticateTestUser();
        Task task = new Task("it-task", "Write docs", "Integration test path");

        boolean added = taskService.addTask(task);

        assertThat(added).isTrue();
        assertThat(taskRepository.findByTaskIdAndUser("it-task", owner)).isPresent();
    }

    @Test
    void databaseRejectsNullDescription() {
        User owner = userRepository.save(TestUserFactory.createUser("task-it-invalid"));
        TaskEntity entity = new TaskEntity("it-task-null", "Name", null, owner);

        assertThatThrownBy(() -> taskRepository.saveAndFlush(entity))
                .as("description column is NOT NULL")
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User authenticateTestUser() {
        User user = userRepository.save(TestUserFactory.createUser());
        var authentication = new UsernamePasswordAuthenticationToken(
                user,
                user.getPassword(),
                user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return user;
    }
}
