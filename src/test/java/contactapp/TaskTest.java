package contactapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link Task}.
 *
 * Covers:
 *  - trimming and successful construction
 *  - update semantics (atomic replacement of fields)
 *  - setter behavior for valid values
 *  - validation of taskId, name, and description in both constructor and setters
 */
public class TaskTest {

    @Test
    void testSuccessfulCreationTrimsValues() {
        Task task = new Task(" 123 ", "  Write tests ", "  Cover edge cases ");

        assertThat(task.getTaskId()).isEqualTo("123");
        assertThat(task.getName()).isEqualTo("Write tests");
        assertThat(task.getDescription()).isEqualTo("Cover edge cases");
    }

    @Test
    void testUpdateReplacesValuesAtomically() {
        Task task = new Task("100", "Draft plan", "Outline initial work");

        task.update("Implement feature", "Finish Task entity and service");

        assertThat(task.getName()).isEqualTo("Implement feature");
        assertThat(task.getDescription()).isEqualTo("Finish Task entity and service");
    }

    @Test
    void testSettersAcceptValidValues() {
        Task task = new Task("100", "Draft plan", "Outline initial work");

        task.setName("Write docs");
        task.setDescription("Document Task behavior");

        assertThat(task.getName()).isEqualTo("Write docs");
        assertThat(task.getDescription()).isEqualTo("Document Task behavior");
    }

    @CsvSource(value = {
            // taskId validation
            "' ', name, description, 'taskId must not be null or blank'",
            "'', name, description, 'taskId must not be null or blank'",
            "null, name, description, 'taskId must not be null or blank'",
            "12345678901, name, description, 'taskId length must be between 1 and 10'",

            // name validation
            "1, ' ', description, 'name must not be null or blank'",
            "1, '', description, 'name must not be null or blank'",
            "1, null, description, 'name must not be null or blank'",
            "1, This name is definitely too long, description, 'name length must be between 1 and 20'",

            // description validation
            "1, name, ' ', 'description must not be null or blank'",
            "1, name, '', 'description must not be null or blank'",
            "1, name, null, 'description must not be null or blank'",
            "1, name, 'This description is intentionally made way too long to exceed the fifty character limit set', 'description length must be between 1 and 50'"
    }, nullValues = "null")

    @ParameterizedTest
    void testConstructorValidation(
            String taskId,
            String name,
            String description,
            String expectedMessage) {
        assertThatThrownBy(() -> new Task(taskId, name, description))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @CsvSource(value = {
            "' ', 'name must not be null or blank'",
            "'', 'name must not be null or blank'",
            "null, 'name must not be null or blank'",
            "This name is definitely too long, 'name length must be between 1 and 20'",
    }, nullValues = "null")

    @ParameterizedTest
    void testSetNameValidation(String invalidName, String expectedMessage) {
        Task task = new Task("100", "Valid", "Valid description");

        assertThatThrownBy(() -> task.setName(invalidName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @CsvSource(value = {
            "' ', 'description must not be null or blank'",
            "'', 'description must not be null or blank'",
            "null, 'description must not be null or blank'",
            "'This description is intentionally made way too long to exceed the fifty character limit set', 'description length must be between 1 and 50'"
    }, nullValues = "null")

    @ParameterizedTest
    void testSetDescriptionValidation(String invalidDescription, String expectedMessage) {
        Task task = new Task("100", "Valid", "Valid description");

        assertThatThrownBy(() -> task.setDescription(invalidDescription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
