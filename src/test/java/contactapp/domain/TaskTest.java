package contactapp.domain;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.util.stream.Stream;

import contactapp.support.TestDates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Unit tests for {@link Task}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Trimming and successful construction</li>
 *   <li>Update semantics (atomic replacement of fields and unchanged state on validation failure)</li>
 *   <li>Setter behavior for valid values</li>
 *   <li>Validation of taskId, name, and description in constructor, setters, and update(...)</li>
 * </ul>
 *
 * <p>Tests are in the same package as Task to access package-private methods.
 */
public class TaskTest {

    // ==================== Domain-Specific Date Helpers ====================
    // Built on TestDates, express intent clearly for Task-specific tests

    /** Returns a valid future due date for tests that need a non-past date. */
    private static java.time.LocalDate validDueDate() {
        return TestDates.FUTURE_DATE;
    }

    /** Returns a past date for tests that verify past-date rejection (constant, not wall clock). */
    private static java.time.LocalDate pastDueDate() {
        return TestDates.PAST_DATE; // 2000-01-01, always in the past
    }

    /**
     * Ensures constructor trimming works for every field.
     */
    @Test
    void testSuccessfulCreationTrimsValues() {
        Task task = new Task(" 123 ", "  Write tests ", "  Cover edge cases ");

        assertThat(task.getTaskId()).isEqualTo("123");
        assertThat(task.getName()).isEqualTo("Write tests");
        assertThat(task.getDescription()).isEqualTo("Cover edge cases");
    }

    /**
     * Confirms {@link Task#update(String, String)} swaps both mutable fields in one operation.
     */
    @Test
    void testUpdateReplacesValuesAtomically() {
        Task task = new Task("100", "Draft plan", "Outline initial work");

        task.update("Implement feature", "Finish Task entity and service");

        assertThat(task.getName()).isEqualTo("Implement feature");
        assertThat(task.getDescription()).isEqualTo("Finish Task entity and service");
    }

    /**
     * Validates that setters accept good data and persist changes.
     */
    @Test
    void testSettersAcceptValidValues() {
        Task task = new Task("100", "Draft plan", "Outline initial work");

        task.setName("Write docs");
        task.setDescription("Document Task behavior");

        assertThat(task.getName()).isEqualTo("Write docs");
        assertThat(task.getDescription()).isEqualTo("Document Task behavior");
    }

    /**
     * Supplies invalid constructor inputs for {@link #testConstructorValidation}.
     */
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

    /**
     * Ensures invalid constructor inputs raise {@link IllegalArgumentException} with the expected message.
     */
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

    /**
     * Drives invalid name values for {@link #testSetNameValidation}.
     */
    @CsvSource(value = {
            "' ', 'name must not be null or blank'",
            "'', 'name must not be null or blank'",
            "null, 'name must not be null or blank'",
            "This name is definitely too long, 'name length must be between 1 and 20'",
    }, nullValues = "null")

    /**
     * Verifies {@link Task#setName(String)} rejects blank/null/over-length names.
     */
    @ParameterizedTest
    void testSetNameValidation(String invalidName, String expectedMessage) {
        Task task = new Task("100", "Valid", "Valid description");

        assertThatThrownBy(() -> task.setName(invalidName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    /**
     * Supplies invalid descriptions for {@link #testSetDescriptionValidation}.
     */
    @CsvSource(value = {
            "' ', 'description must not be null or blank'",
            "'', 'description must not be null or blank'",
            "null, 'description must not be null or blank'",
            "'This description is intentionally made way too long to exceed the fifty character limit set', 'description length must be between 1 and 50'"
    }, nullValues = "null")

    /**
     * Ensures {@link Task#setDescription(String)} enforces the required constraints.
     */
    @ParameterizedTest
    void testSetDescriptionValidation(String invalidDescription, String expectedMessage) {
        Task task = new Task("100", "Valid", "Valid description");

        assertThatThrownBy(() -> task.setDescription(invalidDescription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    /**
     * Data for update(...) inputs we expect to fail.
     *
     * <p>Each Arguments.of(name, description, message) row feeds one run of
     * testUpdateRejectsInvalidValuesAtomically.
     */
    private static Stream<Arguments> invalidUpdateInputs() {
        return Stream.of(
                Arguments.of(" ", "Valid description", "name must not be null or blank"),
                Arguments.of("", "Valid description", "name must not be null or blank"),
                Arguments.of(null, "Valid description", "name must not be null or blank"),
                Arguments.of("This name is definitely too long", "Valid description", "name length must be between 1 and 20"),
                Arguments.of("Valid", " ", "description must not be null or blank"),
                Arguments.of("Valid", "", "description must not be null or blank"),
                Arguments.of("Valid", null, "description must not be null or blank"),
                Arguments.of("Valid", "This description is intentionally made way too long to exceed the fifty character limit set", "description length must be between 1 and 50")
        );
    }

    /**
     * update(...) must:
     * <ul>
     *   <li>reject invalid name/description with the correct error message</li>
     *   <li>leave the existing Task state unchanged when validation fails (atomic)</li>
     * </ul>
     */
    @ParameterizedTest
    @MethodSource("invalidUpdateInputs")
    void testUpdateRejectsInvalidValuesAtomically(
            String newName,
            String newDescription,
            String expectedMessage) {
        Task task = new Task("100", "Draft plan", "Outline initial work");

        assertThatThrownBy(() -> task.update(newName, newDescription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);

        assertThat(task)
                .hasFieldOrPropertyWithValue("name", "Draft plan")
                .hasFieldOrPropertyWithValue("description", "Outline initial work");
    }

    /**
     * Ensures the copy guard rejects corrupted state (null internal fields).
     *
     * <p>Added to kill PITest mutant: "removed call to validateCopySource" in Task.copy().
     * This test uses reflection to corrupt each internal field and verify copy() throws.
     * Parameterized to achieve full branch coverage of the validateCopySource null checks.
     */
    @ParameterizedTest(name = "copy rejects null {0}")
    @MethodSource("nullFieldProvider")
    void testCopyRejectsNullInternalState(String fieldName) throws Exception {
        Task task = new Task("901", "Valid Name", "Valid Description");

        // Use reflection to corrupt internal state (simulate memory corruption or serialization bugs)
        Field field = Task.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(task, null);

        assertThatThrownBy(task::copy)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task copy source must not be null");
    }

    /**
     * Provides field names for the null internal state test.
     */
    static Stream<String> nullFieldProvider() {
        return Stream.of("taskId", "name", "description");
    }

    // ==================== Additional Boundary and Edge Case Tests ====================

    /**
     * Tests task ID at exact maximum length boundary (10 characters).
     *
     * <p><b>Why this test exists:</b> Ensures that a task ID with exactly 10 characters
     * is accepted. Catches mutants that change {@code >} to {@code >=} in length validation.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary: {@code >} to {@code >=}</li>
     *   <li>Replaced MAX_ID_LENGTH constant</li>
     * </ul>
     */
    @Test
    void constructor_acceptsTaskIdAtMaximumLength() {
        final Task task = new Task("1234567890", "Task name", "Task description");

        assertThat(task.getTaskId()).isEqualTo("1234567890");
        assertThat(task.getTaskId()).hasSize(10);
    }

    /**
     * Tests task ID one character over maximum length (11 characters).
     *
     * <p><b>Why this test exists:</b> Ensures that a task ID with 11 characters
     * is rejected. Tests the boundary from the rejection side.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary</li>
     *   <li>Removed length validation</li>
     * </ul>
     */
    @Test
    void constructor_rejectsTaskIdOneOverMaximumLength() {
        assertThatThrownBy(() ->
                new Task("12345678901", "Task name", "Task description"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("taskId length must be between 1 and 10");
    }

    /**
     * Tests task name at exact maximum length boundary (20 characters).
     *
     * <p><b>Why this test exists:</b> Ensures that a task name with exactly 20 characters
     * is accepted. Catches boundary condition mutants.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary in validateLength</li>
     *   <li>Replaced NAME_MAX_LENGTH constant</li>
     * </ul>
     */
    @Test
    void constructor_acceptsNameAtMaximumLength() {
        final String maxName = "12345678901234567890"; // Exactly 20 chars
        final Task task = new Task("1", maxName, "Description");

        assertThat(task.getName()).isEqualTo(maxName);
        assertThat(task.getName()).hasSize(20);
    }

    /**
     * Tests task name one character over maximum length (21 characters).
     *
     * <p><b>Why this test exists:</b> Ensures that a task name with 21 characters
     * is rejected. Tests the upper boundary validation.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary</li>
     *   <li>Removed validation check</li>
     * </ul>
     */
    @Test
    void constructor_rejectsNameOneOverMaximumLength() {
        final String tooLongName = "123456789012345678901"; // 21 chars

        assertThatThrownBy(() ->
                new Task("1", tooLongName, "Description"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("name length must be between 1 and 20");
    }

    /**
     * Tests task description at exact maximum length boundary (50 characters).
     *
     * <p><b>Why this test exists:</b> Ensures that a task description with exactly
     * 50 characters is accepted. Tests the maximum boundary.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary: {@code >} to {@code >=}</li>
     *   <li>Replaced DESCRIPTION_MAX_LENGTH constant</li>
     * </ul>
     */
    @Test
    void constructor_acceptsDescriptionAtMaximumLength() {
        final String maxDesc = "12345678901234567890123456789012345678901234567890"; // 50 chars
        final Task task = new Task("1", "Task name", maxDesc);

        assertThat(task.getDescription()).isEqualTo(maxDesc);
        assertThat(task.getDescription()).hasSize(50);
    }

    /**
     * Tests task description one character over maximum length (51 characters).
     *
     * <p><b>Why this test exists:</b> Ensures that a description with 51 characters
     * is rejected. Tests the boundary from the other side.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary</li>
     *   <li>Removed validation check</li>
     * </ul>
     */
    @Test
    void constructor_rejectsDescriptionOneOverMaximumLength() {
        final String tooLongDesc = "123456789012345678901234567890123456789012345678901"; // 51 chars

        assertThatThrownBy(() ->
                new Task("1", "Task name", tooLongDesc))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("description length must be between 1 and 50");
    }

    /**
     * Tests setName with a name at exact maximum length.
     *
     * <p><b>Why this test exists:</b> Ensures setName correctly validates and accepts
     * a 20-character name through the setter path.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary in setter</li>
     * </ul>
     */
    @Test
    void setName_acceptsNameAtMaximumLength() {
        final Task task = new Task("1", "Initial", "Description");
        final String maxName = "12345678901234567890"; // 20 chars

        task.setName(maxName);

        assertThat(task.getName()).isEqualTo(maxName);
        assertThat(task.getName()).hasSize(20);
    }

    /**
     * Tests setName with a single character (minimum length).
     *
     * <p><b>Why this test exists:</b> Ensures setName accepts the minimum valid
     * length (1 character). Tests the lower boundary.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary: {@code <} to {@code <=}</li>
     * </ul>
     */
    @Test
    void setName_acceptsMinimumLengthName() {
        final Task task = new Task("1", "Initial", "Description");

        task.setName("A");

        assertThat(task.getName()).isEqualTo("A");
        assertThat(task.getName()).hasSize(1);
    }

    /**
     * Tests setDescription with a description at exact maximum length.
     *
     * <p><b>Why this test exists:</b> Ensures setDescription correctly validates
     * and accepts a 50-character description.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary in setter</li>
     * </ul>
     */
    @Test
    void setDescription_acceptsDescriptionAtMaximumLength() {
        final Task task = new Task("1", "Task", "Initial");
        final String maxDesc = "12345678901234567890123456789012345678901234567890"; // 50 chars

        task.setDescription(maxDesc);

        assertThat(task.getDescription()).isEqualTo(maxDesc);
        assertThat(task.getDescription()).hasSize(50);
    }

    /**
     * Tests setDescription with a single character (minimum length).
     *
     * <p><b>Why this test exists:</b> Ensures setDescription accepts the minimum
     * valid length. Tests the lower boundary of description validation.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary: {@code <} to {@code <=}</li>
     * </ul>
     */
    @Test
    void setDescription_acceptsMinimumLengthDescription() {
        final Task task = new Task("1", "Task", "Initial");

        task.setDescription("X");

        assertThat(task.getDescription()).isEqualTo("X");
        assertThat(task.getDescription()).hasSize(1);
    }

    /**
     * Tests update() with maximum length values for both fields.
     *
     * <p><b>Why this test exists:</b> Ensures atomic update works correctly when
     * both fields are at their maximum length boundaries.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary in update validation</li>
     *   <li>Swapped field assignments</li>
     * </ul>
     */
    @Test
    void update_acceptsMaximumLengthValues() {
        final Task task = new Task("1", "Old name", "Old description");
        final String maxName = "12345678901234567890"; // 20 chars
        final String maxDesc = "12345678901234567890123456789012345678901234567890"; // 50 chars

        task.update(maxName, maxDesc);

        assertThat(task.getName()).isEqualTo(maxName);
        assertThat(task.getDescription()).isEqualTo(maxDesc);
    }

    /**
     * Tests copy() creates an exact duplicate with same values.
     *
     * <p><b>Why this test exists:</b> Ensures copy() correctly copies all fields
     * without swapping or omitting values.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Swapped constructor arguments in copy</li>
     *   <li>Returned 'this' instead of new instance</li>
     * </ul>
     */
    @Test
    void copy_createsExactDuplicateWithSameValues() {
        final Task original = new Task("99", "Implement feature", "Add new functionality");

        final Task copy = original.copy();

        // Verify exact same values
        assertThat(copy.getTaskId()).isEqualTo("99");
        assertThat(copy.getName()).isEqualTo("Implement feature");
        assertThat(copy.getDescription()).isEqualTo("Add new functionality");

        // Verify different instance
        assertThat(copy).isNotSameAs(original);
    }

    /**
     * Tests that modifying a copy doesn't affect the original.
     *
     * <p><b>Why this test exists:</b> Ensures copy() creates a true independent
     * copy without shared mutable state.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Returned 'this' instead of new instance</li>
     * </ul>
     */
    @Test
    void copy_modificationsToopyDontAffectOriginal() {
        final Task original = new Task("99", "Implement feature", "Add new functionality");
        final Task copy = original.copy();

        // Modify the copy
        copy.setName("Different name");
        copy.setDescription("Different description");

        // Verify original is unchanged
        assertThat(original.getName()).isEqualTo("Implement feature");
        assertThat(original.getDescription()).isEqualTo("Add new functionality");
    }

    // ==================== Tests for New Fields (Status, DueDate, Timestamps) ====================

    /**
     * Tests constructor with all parameters sets all fields correctly.
     */
    @Test
    void constructor_withAllParameters_setsAllFields() {
        final LocalDate dueDate = validDueDate();
        final Instant before = Instant.now();

        final Task task = new Task("1", "Task name", "Task description", TaskStatus.IN_PROGRESS, dueDate);

        final Instant after = Instant.now();

        assertThat(task.getTaskId()).isEqualTo("1");
        assertThat(task.getName()).isEqualTo("Task name");
        assertThat(task.getDescription()).isEqualTo("Task description");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getDueDate()).isEqualTo(dueDate);
        assertThat(task.getCreatedAt()).isBetween(before, after);
        assertThat(task.getUpdatedAt()).isBetween(before, after);
        assertThat(task.getCreatedAt()).isEqualTo(task.getUpdatedAt());
    }

    /**
     * Tests constructor with default parameters uses TODO status and null due date.
     */
    @Test
    void constructor_withDefaultParameters_usesTodoStatusAndNullDueDate() {
        final Task task = new Task("1", "Task name", "Task description");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getDueDate()).isNull();
        assertThat(task.getCreatedAt()).isNotNull();
        assertThat(task.getUpdatedAt()).isNotNull();
    }

    /**
     * Tests constructor with null status defaults to TODO.
     */
    @Test
    void constructor_withNullStatus_defaultsToTodo() {
        final Task task = new Task("1", "Task name", "Task description", null, null);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    /**
     * Tests constructor with null due date is accepted.
     */
    @Test
    void constructor_withNullDueDate_isAccepted() {
        final Task task = new Task("1", "Task name", "Task description", TaskStatus.DONE, null);

        assertThat(task.getDueDate()).isNull();
    }

    /**
     * Tests setStatus updates the status.
     */
    @Test
    void setStatus_updatesStatus() {
        final Task task = new Task("1", "Task name", "Task description");

        task.setStatus(TaskStatus.IN_PROGRESS);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    /**
     * Tests setStatus with null defaults to TODO.
     */
    @Test
    void setStatus_withNull_defaultsToTodo() {
        final Task task = new Task("1", "Task name", "Task description", TaskStatus.DONE, null);

        task.setStatus(null);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    /**
     * Tests setDueDate updates the due date.
     */
    @Test
    void setDueDate_updatesDueDate() {
        final Task task = new Task("1", "Task name", "Task description");
        final LocalDate dueDate = validDueDate();

        task.setDueDate(dueDate);

        assertThat(task.getDueDate()).isEqualTo(dueDate);
    }

    /**
     * Tests setDueDate with null clears the due date.
     */
    @Test
    void setDueDate_withNull_clearsDueDate() {
        final LocalDate dueDate = validDueDate();
        final Task task = new Task("1", "Task name", "Task description", TaskStatus.TODO, dueDate);

        task.setDueDate(null);

        assertThat(task.getDueDate()).isNull();
    }

    /**
     * Tests update with all parameters updates all mutable fields and updatedAt timestamp.
     */
    @Test
    void update_withAllParameters_updatesAllFieldsAndTimestamp() throws InterruptedException {
        final Task task = new Task("1", "Old name", "Old description");
        final Instant originalCreatedAt = task.getCreatedAt();
        final Instant originalUpdatedAt = task.getUpdatedAt();

        // Wait a bit to ensure timestamp changes
        Thread.sleep(10);

        final LocalDate newDueDate = TestDates.futureDate(60);
        task.update("New name", "New description", TaskStatus.DONE, newDueDate);

        assertThat(task.getName()).isEqualTo("New name");
        assertThat(task.getDescription()).isEqualTo("New description");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DONE);
        assertThat(task.getDueDate()).isEqualTo(newDueDate);
        assertThat(task.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(task.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    /**
     * Tests update with null status defaults to TODO.
     */
    @Test
    void update_withNullStatus_defaultsToTodo() {
        final Task task = new Task("1", "Name", "Description", TaskStatus.DONE, null);

        task.update("New name", "New description", null, null);

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    /**
     * Tests update with two parameters preserves status and dueDate.
     */
    @Test
    void update_withTwoParameters_preservesStatusAndDueDate() {
        final LocalDate dueDate = validDueDate();
        final Task task = new Task("1", "Old name", "Old description", TaskStatus.IN_PROGRESS, dueDate);

        task.update("New name", "New description");

        assertThat(task.getName()).isEqualTo("New name");
        assertThat(task.getDescription()).isEqualTo("New description");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(task.getDueDate()).isEqualTo(dueDate);
    }

    /**
     * Tests copy preserves all fields including status, dueDate, and timestamps.
     */
    @Test
    void copy_preservesAllFieldsIncludingTimestamps() {
        final LocalDate dueDate = validDueDate();
        final Task original = new Task("1", "Task name", "Task description", TaskStatus.IN_PROGRESS, dueDate);

        final Task copy = original.copy();

        assertThat(copy.getTaskId()).isEqualTo(original.getTaskId());
        assertThat(copy.getName()).isEqualTo(original.getName());
        assertThat(copy.getDescription()).isEqualTo(original.getDescription());
        assertThat(copy.getStatus()).isEqualTo(original.getStatus());
        assertThat(copy.getDueDate()).isEqualTo(original.getDueDate());
        assertThat(copy.getCreatedAt()).isEqualTo(original.getCreatedAt());
        assertThat(copy.getUpdatedAt()).isEqualTo(original.getUpdatedAt());
        assertThat(copy).isNotSameAs(original);
    }

    /**
     * Tests copy with null dueDate preserves null.
     */
    @Test
    void copy_withNullDueDate_preservesNull() {
        final Task original = new Task("1", "Task name", "Task description", TaskStatus.TODO, null);

        final Task copy = original.copy();

        assertThat(copy.getDueDate()).isNull();
    }

    /**
     * Provides field names for the null internal state test (updated for new fields).
     */
    static Stream<String> nullFieldProviderEnhanced() {
        return Stream.of("taskId", "name", "description", "status", "createdAt", "updatedAt");
    }

    /**
     * Tests that copy rejects null status field.
     */
    @ParameterizedTest(name = "copy rejects null {0}")
    @MethodSource("nullFieldProviderEnhanced")
    void testCopyRejectsNullInternalStateEnhanced(String fieldName) throws Exception {
        Task task = new Task("901", "Valid Name", "Valid Description", TaskStatus.TODO, null);

        // Use reflection to corrupt internal state
        Field field = Task.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(task, null);

        assertThatThrownBy(task::copy)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("task copy source must not be null");
    }

    /**
     * Tests that all TaskStatus enum values can be used.
     */
    @Test
    void constructor_acceptsAllTaskStatusValues() {
        final Task todo = new Task("1", "Name", "Desc", TaskStatus.TODO, null);
        final Task inProgress = new Task("2", "Name", "Desc", TaskStatus.IN_PROGRESS, null);
        final Task done = new Task("3", "Name", "Desc", TaskStatus.DONE, null);

        assertThat(todo.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(inProgress.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(done.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    /**
     * Tests that due dates in the past are rejected.
     */
    @Test
    void constructor_rejectsPastDueDate() {
        final LocalDate past = pastDueDate();

        assertThatThrownBy(() -> new Task("1", "Name", "Desc", TaskStatus.TODO, past))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dueDate must not be in the past");
    }

    /**
     * Tests that due dates in the future are accepted.
     */
    @Test
    void constructor_acceptsFutureDueDate() {
        final LocalDate future = validDueDate();
        final Task task = new Task("1", "Name", "Desc", TaskStatus.TODO, future);

        assertThat(task.getDueDate()).isEqualTo(future);
    }

    /**
     * Tests that modifying a copy's status doesn't affect the original.
     */
    @Test
    void copy_modificationsToStatusDontAffectOriginal() {
        final Task original = new Task("1", "Name", "Desc", TaskStatus.TODO, null);
        final Task copy = original.copy();

        copy.setStatus(TaskStatus.DONE);

        assertThat(original.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(copy.getStatus()).isEqualTo(TaskStatus.DONE);
    }

    /**
     * Tests that modifying a copy's dueDate doesn't affect the original.
     */
    @Test
    void copy_modificationsToDueDateDontAffectOriginal() {
        final LocalDate originalDate = TestDates.futureDate(30);
        final LocalDate newDate = TestDates.futureDate(60);
        final Task original = new Task("1", "Name", "Desc", TaskStatus.TODO, originalDate);
        final Task copy = original.copy();

        copy.setDueDate(newDate);

        assertThat(original.getDueDate()).isEqualTo(originalDate);
        assertThat(copy.getDueDate()).isEqualTo(newDate);
    }

    @Test
    void setDueDate_rejectsPastDates() {
        final Task task = new Task("1", "Name", "Desc", TaskStatus.TODO, null);

        assertThatThrownBy(() -> task.setDueDate(LocalDate.now().minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dueDate must not be in the past");
    }

    @Test
    void updateWithPastDueDateThrows() {
        final Task task = new Task("1", "Name", "Desc", TaskStatus.TODO, null);

        assertThatThrownBy(() -> task.update("N", "D", TaskStatus.TODO, LocalDate.now().minusDays(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dueDate must not be in the past");
    }
}
