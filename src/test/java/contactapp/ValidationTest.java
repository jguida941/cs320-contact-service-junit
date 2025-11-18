package contactapp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused tests for {@link Validation} helpers to ensure PIT sees
 * their behavior when blank checks or boundary logic are mutated.
 */
class ValidationTest {

    private static final int MIN_LENGTH = 1;
    private static final int NAME_MAX = 10;
    private static final int ADDRESS_MAX = 30;
    private static final int PHONE_LENGTH = 10;

    // Ensure helper allows lengths exactly at the configured boundaries for names/addresses
    @Test
    void validateLengthAcceptsBoundaryValues() {
        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("A", "firstName", MIN_LENGTH, NAME_MAX));

        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("ABCDEFGHIJ", "firstName", MIN_LENGTH, NAME_MAX));

        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("123456789012345678901234567890", "address", MIN_LENGTH, ADDRESS_MAX));
    }

    // Blank inputs must still fail length validation for firstName
    @Test
    void validateLengthRejectsBlankStrings() {
        assertThatThrownBy(() ->
                Validation.validateLength("   ", "firstName", MIN_LENGTH, NAME_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName must not be null or blank");
    }

    // Null inputs must also fail length validation immediately
    @Test
    void validateLengthRejectsNull() {
        assertThatThrownBy(() ->
                Validation.validateLength(null, "firstName", MIN_LENGTH, NAME_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName must not be null or blank");
    }

    // Phone number validator must detect blank string before digit/length checks
    @Test
    void validateNumeric10RejectsBlankStrings() {
        assertThatThrownBy(() ->
                Validation.validateNumeric10("          ", "phone", PHONE_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must not be null or blank");
    }

    // Null phone input should trigger the same blank check before regex/length logic
    @Test
    void validateNumeric10RejectsNull() {
        assertThatThrownBy(() ->
                Validation.validateNumeric10(null, "phone", PHONE_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must not be null or blank");
    }
}
