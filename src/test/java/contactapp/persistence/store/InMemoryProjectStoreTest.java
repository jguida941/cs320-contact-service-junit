package contactapp.persistence.store;

import contactapp.domain.Project;
import contactapp.domain.ProjectStatus;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the legacy {@link InMemoryProjectStore} so mutation testing can
 * verify every branch even though production code uses JPA stores.
 */
class InMemoryProjectStoreTest {

    private final InMemoryProjectStore store = new InMemoryProjectStore();

    @BeforeEach
    void cleanStore() {
        store.deleteAll();
    }

    @Test
    void saveAndFindReturnDefensiveCopies() {
        Project project = new Project("P-1", "Legacy Project", "Legacy Description", ProjectStatus.ACTIVE);

        store.save(project);

        assertThat(store.existsById("P-1")).isTrue();
        Project loaded = store.findById("P-1").orElseThrow();
        assertThat(loaded).isNotSameAs(project);

        loaded.setName("Changed");
        assertThat(store.findById("P-1").orElseThrow().getName()).isEqualTo("Legacy Project");

        List<Project> snapshot = store.findAll();
        snapshot.get(0).setDescription("Mutated");
        assertThat(store.findById("P-1").orElseThrow().getDescription()).isEqualTo("Legacy Description");
        assertThat(store.existsById("missing")).isFalse();
        // Missing IDs should return Optional.empty so the null branch in findById stays covered.
        assertThat(store.findById("missing")).isEmpty();
    }

    @Test
    void deleteOperationsRemoveEntries() {
        Project project = new Project("P-2", "Delete Me", "Delete Description", ProjectStatus.COMPLETED);
        store.save(project);

        assertThat(store.deleteById("P-2")).isTrue();
        assertThat(store.existsById("P-2")).isFalse();

        store.save(project);
        store.deleteAll();
        assertThat(store.findAll()).isEmpty();
        assertThat(store.deleteById("missing")).isFalse();
    }

    @Test
    void nullGuardsThrowExceptions() {
        assertThatThrownBy(() -> store.existsById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId");

        assertThatThrownBy(() -> store.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("project aggregate");

        Project projectWithoutId = new Project("stub", "Project", "Description", ProjectStatus.ACTIVE);
        setField(projectWithoutId, "projectId", null);
        assertThatThrownBy(() -> store.save(projectWithoutId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projectId");

        assertThatThrownBy(() -> store.findById(null))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> store.deleteById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void setField(final Object target, final String fieldName, final Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final IllegalAccessException | NoSuchFieldException ex) {
            throw new AssertionError(ex);
        }
    }
}
