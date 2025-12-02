package contactapp.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the TaskStatus enum.
 *
 * <p>Verifies:
 * <ul>
 *   <li>All enum values are defined correctly</li>
 *   <li>valueOf() works for all valid values</li>
 *   <li>valueOf() throws for invalid values</li>
 *   <li>values() returns all enum constants</li>
 * </ul>
 */
public class TaskStatusTest {

    /**
     * Verifies that all expected enum values exist.
     */
    @Test
    void testAllValuesExist() {
        TaskStatus[] values = TaskStatus.values();

        assertThat(values)
                .hasSize(3)
                .containsExactlyInAnyOrder(
                        TaskStatus.TODO,
                        TaskStatus.IN_PROGRESS,
                        TaskStatus.DONE
                );
    }

    /**
     * Verifies that valueOf() works for all valid enum values.
     */
    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void testValueOfWithValidNames(TaskStatus status) {
        String name = status.name();
        TaskStatus result = TaskStatus.valueOf(name);

        assertThat(result).isEqualTo(status);
    }

    /**
     * Verifies that valueOf() throws IllegalArgumentException for invalid values.
     */
    @Test
    void testValueOfWithInvalidName() {
        assertThatThrownBy(() -> TaskStatus.valueOf("INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Verifies that valueOf() throws NullPointerException for null.
     */
    @Test
    void testValueOfWithNull() {
        assertThatThrownBy(() -> TaskStatus.valueOf(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Verifies that each enum constant has the expected name.
     */
    @Test
    void testEnumNames() {
        assertThat(TaskStatus.TODO.name()).isEqualTo("TODO");
        assertThat(TaskStatus.IN_PROGRESS.name()).isEqualTo("IN_PROGRESS");
        assertThat(TaskStatus.DONE.name()).isEqualTo("DONE");
    }

    /**
     * Verifies that each enum constant has the expected ordinal value.
     */
    @Test
    void testEnumOrdinals() {
        assertThat(TaskStatus.TODO.ordinal()).isEqualTo(0);
        assertThat(TaskStatus.IN_PROGRESS.ordinal()).isEqualTo(1);
        assertThat(TaskStatus.DONE.ordinal()).isEqualTo(2);
    }

    /**
     * Verifies that toString() returns the enum name by default.
     */
    @Test
    void testToString() {
        assertThat(TaskStatus.TODO.toString()).isEqualTo("TODO");
        assertThat(TaskStatus.IN_PROGRESS.toString()).isEqualTo("IN_PROGRESS");
        assertThat(TaskStatus.DONE.toString()).isEqualTo("DONE");
    }

    /**
     * Verifies that enum instances can be compared using == and equals().
     */
    @Test
    void testEnumEquality() {
        TaskStatus todo1 = TaskStatus.TODO;
        TaskStatus todo2 = TaskStatus.valueOf("TODO");

        assertThat(todo1).isSameAs(todo2);
        assertThat(todo1).isEqualTo(todo2);
        assertThat(todo1 == todo2).isTrue();
    }

    /**
     * Verifies that different enum instances are not equal.
     */
    @Test
    void testEnumInequality() {
        assertThat(TaskStatus.TODO).isNotEqualTo(TaskStatus.DONE);
        assertThat(TaskStatus.IN_PROGRESS).isNotEqualTo(TaskStatus.DONE);
    }
}
