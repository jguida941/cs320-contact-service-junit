package contactapp.persistence.mapper;

import contactapp.domain.Project;
import contactapp.domain.ProjectStatus;
import contactapp.persistence.entity.ProjectEntity;
import contactapp.security.User;
import contactapp.support.TestUserFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies {@link ProjectMapper} round-trips data and reuses domain validation.
 */
class ProjectMapperTest {

    private final ProjectMapper mapper = new ProjectMapper();

    @Test
    void toEntityCopiesAllFields() {
        Project project = new Project("1234567890", "Alpha Project", "Detailed description", ProjectStatus.ACTIVE);
        User owner = TestUserFactory.createUser("project-mapper");

        ProjectEntity entity = mapper.toEntity(project, owner);

        assertThat(entity.getProjectId()).isEqualTo("1234567890");
        assertThat(entity.getName()).isEqualTo("Alpha Project");
        assertThat(entity.getDescription()).isEqualTo("Detailed description");
        assertThat(entity.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(entity.getUser()).isEqualTo(owner);
    }

    @Test
    void toDomainReusesValidation() {
        ProjectEntity entity = new ProjectEntity(
                "abc",
                "Beta Project",
                "Project description",
                ProjectStatus.ON_HOLD,
                TestUserFactory.createUser("project-mapper-domain"));

        Project project = mapper.toDomain(entity);

        assertThat(project.getProjectId()).isEqualTo("abc");
        assertThat(project.getName()).isEqualTo("Beta Project");
        assertThat(project.getDescription()).isEqualTo("Project description");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ON_HOLD);
    }

    /**
     * Null values can surface from legacy persistence fallbacks, so returning null keeps
     * callers from receiving phantom entities.
     */
    @Test
    void toEntityReturnsNullWhenDomainIsNull() {
        assertThat(mapper.toEntity(null, TestUserFactory.createUser())).isNull();
    }

    @Test
    void toDomainRejectsInvalidDatabaseData() {
        ProjectEntity entity = new ProjectEntity(
                "abc",
                "Beta Project",
                "Project description",
                null,  // Invalid: null status
                TestUserFactory.createUser("project-mapper-invalid"));

        assertThatThrownBy(() -> mapper.toDomain(entity))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }

    /**
     * Mapper should simply return null when a repository hands us a null row.
     */
    @Test
    void toDomainReturnsNullWhenEntityIsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedToEntityReturnsNullWhenDomainNull() {
        // Intentionally testing deprecated method - verifies null handling
        assertThat(mapper.toEntity((Project) null)).isNull();
    }

    @Test
    @SuppressWarnings("deprecation")
    void deprecatedToEntityThrowsWhenDomainProvided() {
        Project project = new Project("legacy", "Legacy Project", "Description", ProjectStatus.ARCHIVED);
        // Intentionally testing deprecated method to ensure it throws
        assertThatThrownBy(() -> mapper.toEntity(project))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void updateEntityCopiesMutableFields() {
        ProjectEntity entity = new ProjectEntity(
                "project-1",
                "Old Name",
                "Old description",
                ProjectStatus.ACTIVE,
                TestUserFactory.createUser("project-mapper-update"));
        Project updated = new Project("project-1", "New Name", "New description", ProjectStatus.COMPLETED);

        mapper.updateEntity(entity, updated);

        assertThat(entity.getName()).isEqualTo("New Name");
        assertThat(entity.getDescription()).isEqualTo("New description");
        assertThat(entity.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    }

    @Test
    void updateEntityThrowsWhenTargetIsNull() {
        Project source = new Project("project-1", "Name", "Description", ProjectStatus.ACTIVE);
        assertThatThrownBy(() -> mapper.updateEntity(null, source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target entity must not be null");
    }

    @Test
    void updateEntityThrowsWhenSourceIsNull() {
        ProjectEntity entity = new ProjectEntity(
                "project-1",
                "Old Name",
                "Old description",
                ProjectStatus.ACTIVE,
                TestUserFactory.createUser("project-mapper-null-source"));
        assertThatThrownBy(() -> mapper.updateEntity(entity, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source project must not be null");
    }
}
