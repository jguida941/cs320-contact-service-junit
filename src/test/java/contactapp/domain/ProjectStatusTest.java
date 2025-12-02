package contactapp.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the ProjectStatus enum.
 *
 * <p>Verifies:
 * <ul>
 *   <li>All enum values are defined correctly</li>
 *   <li>valueOf() works for all valid values</li>
 *   <li>valueOf() throws for invalid values</li>
 *   <li>values() returns all enum constants</li>
 * </ul>
 */
public class ProjectStatusTest {

    /**
     * Verifies that all expected enum values exist.
     */
    @Test
    void testAllValuesExist() {
        ProjectStatus[] values = ProjectStatus.values();

        assertThat(values)
                .hasSize(4)
                .containsExactlyInAnyOrder(
                        ProjectStatus.ACTIVE,
                        ProjectStatus.ON_HOLD,
                        ProjectStatus.COMPLETED,
                        ProjectStatus.ARCHIVED
                );
    }

    /**
     * Verifies that valueOf() works for all valid enum values.
     */
    @ParameterizedTest
    @EnumSource(ProjectStatus.class)
    void testValueOfWithValidNames(ProjectStatus status) {
        String name = status.name();
        ProjectStatus result = ProjectStatus.valueOf(name);

        assertThat(result).isEqualTo(status);
    }

    /**
     * Verifies that valueOf() throws IllegalArgumentException for invalid values.
     */
    @Test
    void testValueOfWithInvalidName() {
        assertThatThrownBy(() -> ProjectStatus.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Verifies that valueOf() throws NullPointerException for null.
     */
    @Test
    void testValueOfWithNull() {
        assertThatThrownBy(() -> ProjectStatus.valueOf(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Verifies that each enum constant has the expected name.
     */
    @Test
    void testEnumNames() {
        assertThat(ProjectStatus.ACTIVE.name()).isEqualTo("ACTIVE");
        assertThat(ProjectStatus.ON_HOLD.name()).isEqualTo("ON_HOLD");
        assertThat(ProjectStatus.COMPLETED.name()).isEqualTo("COMPLETED");
        assertThat(ProjectStatus.ARCHIVED.name()).isEqualTo("ARCHIVED");
    }

    /**
     * Verifies that each enum constant has the expected ordinal value.
     */
    @Test
    void testEnumOrdinals() {
        assertThat(ProjectStatus.ACTIVE.ordinal()).isEqualTo(0);
        assertThat(ProjectStatus.ON_HOLD.ordinal()).isEqualTo(1);
        assertThat(ProjectStatus.COMPLETED.ordinal()).isEqualTo(2);
        assertThat(ProjectStatus.ARCHIVED.ordinal()).isEqualTo(3);
    }

    /**
     * Verifies that toString() returns the enum name by default.
     */
    @Test
    void testToString() {
        assertThat(ProjectStatus.ACTIVE.toString()).isEqualTo("ACTIVE");
        assertThat(ProjectStatus.ON_HOLD.toString()).isEqualTo("ON_HOLD");
        assertThat(ProjectStatus.COMPLETED.toString()).isEqualTo("COMPLETED");
        assertThat(ProjectStatus.ARCHIVED.toString()).isEqualTo("ARCHIVED");
    }

    /**
     * Verifies that enum instances can be compared using == and equals().
     */
    @Test
    void testEnumEquality() {
        ProjectStatus active1 = ProjectStatus.ACTIVE;
        ProjectStatus active2 = ProjectStatus.valueOf("ACTIVE");

        assertThat(active1).isSameAs(active2);
        assertThat(active1).isEqualTo(active2);
        assertThat(active1 == active2).isTrue();
    }

    /**
     * Verifies that different enum instances are not equal.
     */
    @Test
    void testEnumInequality() {
        assertThat(ProjectStatus.ACTIVE).isNotEqualTo(ProjectStatus.COMPLETED);
        assertThat(ProjectStatus.ON_HOLD).isNotEqualTo(ProjectStatus.ARCHIVED);
    }
}
