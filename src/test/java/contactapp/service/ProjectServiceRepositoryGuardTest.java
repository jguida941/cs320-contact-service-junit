package contactapp.service;

import contactapp.persistence.store.InMemoryProjectStore;
import contactapp.persistence.store.ProjectStore;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lightweight unit tests that ensure {@link ProjectService#ensureRepositoriesAvailable()}
 * guards the contact-linking APIs. These scenarios are impossible once Spring injects the
 * repositories, but PIT reported surviving mutants where the guard was removed.
 */
class ProjectServiceRepositoryGuardTest {

    private ProjectService service;

    @BeforeEach
    void createService() throws Exception {
        service = createLegacyService(new InMemoryProjectStore());
    }

    @AfterEach
    void clearSingleton() throws Exception {
        final Field instanceField = ProjectService.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void addContactToProjectFailsWhenRepositoriesNotInjected() {
        assertThatThrownBy(() -> service.addContactToProject("proj", "contact", "CLIENT"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Contact linking operations require JPA repositories");
    }

    @Test
    void removeContactFromProjectFailsWhenRepositoriesNotInjected() {
        assertThatThrownBy(() -> service.removeContactFromProject("proj", "contact"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getProjectContactsFailsWhenRepositoriesNotInjected() {
        assertThatThrownBy(() -> service.getProjectContacts("proj"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getContactProjectsFailsWhenRepositoriesNotInjected() {
        assertThatThrownBy(() -> service.getContactProjects("contact"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static ProjectService createLegacyService(final ProjectStore store) throws Exception {
        final Constructor<ProjectService> ctor =
                ProjectService.class.getDeclaredConstructor(ProjectStore.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(store, true);
    }
}
