package contactapp.domain;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Focused tests for {@link Validation} helpers to ensure PIT sees
 * their behavior when blank checks or boundary logic are mutated.
 *
 * <p>Tests are in the same package as the domain classes to verify
 * the validation utility functions work correctly.
 */
class ValidationTest {

    private static final int MIN_LENGTH = 1;
    private static final int NAME_MAX = 10;
    private static final int ADDRESS_MAX = 30;
    private static final int PHONE_LENGTH = 10;
    private static final String VALID_EMAIL = "user@example.com";

    /**
     * Ensures the helper allows inputs exactly at the configured min/max boundaries.
     */
    @Test
    void validateLengthAcceptsBoundaryValues() {
        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("A", "firstName", MIN_LENGTH, NAME_MAX));

        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("ABCDEFGHIJ", "firstName", MIN_LENGTH, NAME_MAX));

        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("123456789012345678901234567890", "address", MIN_LENGTH, ADDRESS_MAX));
    }

    /**
     * Blank inputs must fail length validation for any label.
     */
    @Test
    void validateLengthRejectsBlankStrings() {
        assertThatThrownBy(() ->
                Validation.validateLength("   ", "firstName", MIN_LENGTH, NAME_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName must not be null or blank");
    }

    /**
     * Null inputs should fail immediately before trim/length checks.
     */
    @Test
    void validateLengthRejectsNull() {
        assertThatThrownBy(() ->
                Validation.validateLength(null, "firstName", MIN_LENGTH, NAME_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName must not be null or blank");
    }

    /**
     * Covers the "too long" branch of {@link Validation#validateLength(String, String, int, int)}.
     */
    @Test
    void validateLengthRejectsTooLong() {
        assertThatThrownBy(() ->
                Validation.validateLength("ABCDEFGHIJK", "firstName", MIN_LENGTH, NAME_MAX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("firstName length must be between 1 and 10");
    }

    /**
     * Covers the "too short" branch of {@link Validation#validateLength(String, String, int, int)}.
     */
    @Test
    void validateLengthRejectsTooShort() {
        assertThatThrownBy(() ->
                Validation.validateLength("A", "middleName", /*minLength*/ 2, /*maxLength*/ 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("middleName length must be between 2 and 5");
    }

    /**
     * Phone numbers must fail fast when blank.
     */
    @Test
    void validateDigitsRejectsBlankStrings() {
        assertThatThrownBy(() ->
                Validation.validateDigits("          ", "phone", PHONE_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must not be null or blank");
    }

    /**
     * Null phones must trigger the same blank check as blanks.
     */
    @Test
    void validateDigitsRejectsNull() {
        assertThatThrownBy(() ->
                Validation.validateDigits(null, "phone", PHONE_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must not be null or blank");
    }

    /**
     * Valid phone numbers (all digits, correct length) must pass validation.
     */
    @Test
    void validateDigitsAcceptsValidPhoneNumber() {
        assertThatNoException().isThrownBy(() ->
                Validation.validateDigits("1234567890", "phone", PHONE_LENGTH));
    }

    /**
     * Phone numbers containing non-digit characters must be rejected.
     */
    @Test
    void validateDigitsRejectsNonDigitCharacters() {
        assertThatThrownBy(() ->
                Validation.validateDigits("123abc7890", "phone", PHONE_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must only contain digits 0-9");
    }

    /**
     * Phone numbers with wrong length must be rejected.
     */
    @Test
    void validateDigitsRejectsWrongLength() {
        assertThatThrownBy(() ->
                Validation.validateDigits("12345", "phone", PHONE_LENGTH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must be exactly 10 digits");
    }

    @Test
    void validateEmailAcceptsStandardAddress() {
        assertThatNoException().isThrownBy(() ->
                Validation.validateEmail(VALID_EMAIL, "email"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainaddress",
            "missing-at.example.com",
            "user@",
            "user@domain",
            "user@domain.",
            "user@domain..com"
    })
    void validateEmailRejectsInvalidFormats(final String email) {
        assertThatThrownBy(() -> Validation.validateEmail(email, "email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email must be a valid email address");
    }

    /**
     * Confirms future dates pass {@link Validation#validateDateNotPast(Date, String)}.
     */
    @Test
    void validateDateNotPastAcceptsFutureDate() {
        final Date future = Date.from(Instant.now().plus(Duration.ofHours(1)));
        assertThatNoException().isThrownBy(() ->
                Validation.validateDateNotPast(future, "appointmentDate"));
    }

    /**
     * Null dates must throw an explicit error.
     */
    @Test
    void validateDateNotPastRejectsNull() {
        assertThatThrownBy(() ->
                Validation.validateDateNotPast((java.util.Date) null, "appointmentDate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointmentDate must not be null");
    }

    /**
     * Past dates must be rejected so appointments cannot be scheduled retroactively.
     */
    @Test
    void validateDateNotPastRejectsPastDate() {
        final Date past = Date.from(Instant.now().minus(Duration.ofHours(1)));
        assertThatThrownBy(() ->
                Validation.validateDateNotPast(past, "appointmentDate"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointmentDate must not be in the past");
    }

    /**
     * A date exactly equal to "now" must be accepted (not rejected as "in the past").
     *
     * <p>Added to kill PITest mutant: boundary condition {@code <} vs {@code <=}
     * in validateDateNotPast(). Uses Clock.fixed() to deterministically test the
     * exact boundary where date.getTime() == clock.millis().
     */
    @Test
    void validateDateNotPastAcceptsDateExactlyEqualToNow() {
        // Fix the clock at a specific instant
        final Instant fixedInstant = Instant.parse("2025-01-15T10:00:00Z");
        final Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        // Create a date with the exact same timestamp as the fixed clock
        final Date exactlyNow = new Date(fixedInstant.toEpochMilli());

        // With < operator: exactlyNow < fixedClock.millis() is false, so no exception
        // With <= operator (mutant): exactlyNow <= fixedClock.millis() is true, so exception
        assertThatNoException().isThrownBy(() ->
                Validation.validateDateNotPast(exactlyNow, "appointmentDate", fixedClock));
    }

    /**
     * Verifies the private constructor exists and is not accessible.
     *
     * <p>Added for line coverage: private constructor in utility class Validation.
     * Documents that the class is intentionally designed as a utility class (non-instantiable).
     */
    @Test
    void privateConstructorIsNotAccessible() throws Exception {
        Constructor<Validation> constructor = Validation.class.getDeclaredConstructor();
        assertThat(constructor.canAccess(null)).isFalse();

        // Verify the constructor can be invoked via reflection (for coverage)
        // but produces a useless instance (utility class pattern)
        constructor.setAccessible(true);
        Validation instance = constructor.newInstance();
        assertThat(instance).isNotNull();
    }

    // ==================== Additional Boundary Condition Tests ====================

    /**
     * Tests the exact minimum boundary condition for length validation.
     *
     * <p><b>Why this test exists:</b> Mutation testing can change the comparison operator
     * from {@code length < minLength} to {@code length <= minLength}, which would incorrectly
     * reject a string at the minimum length. This test ensures a string with trimmed length
     * exactly equal to minLength is accepted.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary: {@code <} to {@code <=}</li>
     *   <li>Negated conditional: {@code <} to {@code >=}</li>
     * </ul>
     */
    @Test
    void validateLength_acceptsExactMinimumLength() {
        // Test with minLength = 1, string of length 1 should be accepted
        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("A", "field", 1, 10));

        // Test with minLength = 3, string of length 3 should be accepted
        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("ABC", "field", 3, 10));
    }

    /**
     * Tests one character below the minimum boundary.
     *
     * <p><b>Why this test exists:</b> This ensures that {@code minLength - 1} is correctly
     * rejected. Without this test, a mutant that changes {@code <} to {@code <=} might
     * pass if we only test values at or above the minimum.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary</li>
     *   <li>Replaced integer subtraction with addition</li>
     * </ul>
     */
    @Test
    void validateLength_rejectsOneBelowMinimum() {
        // minLength = 3, string of length 2 should be rejected
        assertThatThrownBy(() ->
                Validation.validateLength("AB", "field", 3, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field length must be between 3 and 10");
    }

    /**
     * Tests the exact maximum boundary condition for length validation.
     *
     * <p><b>Why this test exists:</b> Mutation testing can change the comparison operator
     * from {@code length > maxLength} to {@code length >= maxLength}, which would incorrectly
     * reject a string at the maximum length. This test ensures a string with trimmed length
     * exactly equal to maxLength is accepted.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary: {@code >} to {@code >=}</li>
     *   <li>Negated conditional: {@code >} to {@code <=}</li>
     * </ul>
     */
    @Test
    void validateLength_acceptsExactMaximumLength() {
        // Test with maxLength = 10, string of length 10 should be accepted
        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("ABCDEFGHIJ", "field", 1, 10));

        // Test with maxLength = 5, string of length 5 should be accepted
        assertThatNoException().isThrownBy(() ->
                Validation.validateLength("ABCDE", "field", 1, 5));
    }

    /**
     * Tests one character above the maximum boundary.
     *
     * <p><b>Why this test exists:</b> This ensures that {@code maxLength + 1} is correctly
     * rejected. Without this test, a mutant that changes {@code >} to {@code >=} might
     * pass if we only test values at or below the maximum.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary</li>
     *   <li>Replaced integer addition with subtraction</li>
     * </ul>
     */
    @Test
    void validateLength_rejectsOneAboveMaximum() {
        // maxLength = 10, string of length 11 should be rejected
        assertThatThrownBy(() ->
                Validation.validateLength("ABCDEFGHIJK", "field", 1, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("field length must be between 1 and 10");
    }

    /**
     * Tests validateDigits with exact required length.
     *
     * <p><b>Why this test exists:</b> Ensures the equality check {@code length != requiredLength}
     * cannot be mutated to {@code length == requiredLength} without detection.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Negated conditional: {@code !=} to {@code ==}</li>
     *   <li>Changed conditional boundary</li>
     * </ul>
     */
    @Test
    void validateDigits_acceptsExactRequiredLength() {
        // Should accept exactly 10 digits
        assertThatNoException().isThrownBy(() ->
                Validation.validateDigits("1234567890", "phone", 10));

        // Should accept exactly 5 digits
        assertThatNoException().isThrownBy(() ->
                Validation.validateDigits("12345", "code", 5));
    }

    /**
     * Tests validateDigits rejects one digit short.
     *
     * <p><b>Why this test exists:</b> Tests the {@code length != requiredLength} check
     * when length is one less than required. Catches mutants that remove or invert
     * the length check.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Removed conditional - removed call to length check</li>
     *   <li>Replaced integer subtraction with addition</li>
     * </ul>
     */
    @Test
    void validateDigits_rejectsOneDigitShort() {
        assertThatThrownBy(() ->
                Validation.validateDigits("123456789", "phone", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must be exactly 10 digits");
    }

    /**
     * Tests validateDigits rejects one digit too long.
     *
     * <p><b>Why this test exists:</b> Tests the {@code length != requiredLength} check
     * when length is one more than required. Ensures the boundary is checked in both directions.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Removed conditional - removed call to length check</li>
     *   <li>Replaced integer addition with subtraction</li>
     * </ul>
     */
    @Test
    void validateDigits_rejectsOneDigitTooLong() {
        assertThatThrownBy(() ->
                Validation.validateDigits("12345678901", "phone", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phone must be exactly 10 digits");
    }

    /**
     * Tests that validateDateNotPast rejects a date exactly 1 millisecond in the past.
     *
     * <p><b>Why this test exists:</b> Tests the boundary condition of the {@code <} operator
     * in {@code date.getTime() < clock.millis()}. A date 1ms in the past should be rejected.
     * This catches mutants that change {@code <} to {@code <=}.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary: {@code <} to {@code <=}</li>
     *   <li>Negated conditional: {@code <} to {@code >=}</li>
     * </ul>
     */
    @Test
    void validateDateNotPast_rejectsDateOneMillisecondInPast() {
        final Instant fixedInstant = Instant.parse("2025-01-15T10:00:00Z");
        final Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        // Create a date exactly 1ms before the clock time
        final Date oneMsInPast = new Date(fixedInstant.toEpochMilli() - 1);

        // Should be rejected as it's in the past (date < now)
        assertThatThrownBy(() ->
                Validation.validateDateNotPast(oneMsInPast, "appointmentDate", fixedClock))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("appointmentDate must not be in the past");
    }

    /**
     * Tests that validateDateNotPast accepts a date exactly 1 millisecond in the future.
     *
     * <p><b>Why this test exists:</b> Tests the boundary condition from the other direction.
     * A date 1ms in the future should be accepted. This ensures the comparison is strictly
     * {@code <} and not {@code <=}.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary</li>
     *   <li>Negated conditional</li>
     * </ul>
     */
    @Test
    void validateDateNotPast_acceptsDateOneMillisecondInFuture() {
        final Instant fixedInstant = Instant.parse("2025-01-15T10:00:00Z");
        final Clock fixedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        // Create a date exactly 1ms after the clock time
        final Date oneMsInFuture = new Date(fixedInstant.toEpochMilli() + 1);

        // Should be accepted as it's in the future (date >= now)
        assertThatNoException().isThrownBy(() ->
                Validation.validateDateNotPast(oneMsInFuture, "appointmentDate", fixedClock));
    }

    /**
     * Tests validateEmail with an email at the exact maximum length boundary.
     *
     * <p><b>Why this test exists:</b> Ensures that an email with length exactly equal to
     * MAX_EMAIL_LENGTH (100) is accepted. Catches mutants that change the length comparison.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary in validateLength</li>
     *   <li>Replaced MAX_EMAIL_LENGTH constant</li>
     * </ul>
     */
    @Test
    void validateEmail_acceptsEmailAtMaximumLength() {
        // Create a valid email with exactly 100 characters
        // Format: username@domain.com (need to make it exactly 100 chars)
        // "a" * 88 + "@example.com" = 88 + 12 = 100 characters
        final String longEmail = "a".repeat(88) + "@example.com";
        assertThat(longEmail).hasSize(100); // Verify it's exactly 100 chars

        assertThatNoException().isThrownBy(() ->
                Validation.validateEmail(longEmail, "email"));
    }

    /**
     * Tests validateEmail rejects an email one character over the maximum length.
     *
     * <p><b>Why this test exists:</b> Ensures that an email with length MAX_EMAIL_LENGTH + 1
     * is rejected. Tests the boundary from the other side.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Changed conditional boundary</li>
     *   <li>Replaced integer addition with subtraction</li>
     * </ul>
     */
    @Test
    void validateEmail_rejectsEmailOneOverMaximumLength() {
        // Create a valid email format with 101 characters
        // "a" * 89 + "@example.com" = 89 + 12 = 101 characters
        final String tooLongEmail = "a".repeat(89) + "@example.com";
        assertThat(tooLongEmail).hasSize(101); // Verify it's 101 chars

        assertThatThrownBy(() ->
                Validation.validateEmail(tooLongEmail, "email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email length must be between 1 and 100");
    }

    /**
     * Tests validateNotBlank with a string containing only spaces (isBlank returns true).
     *
     * <p><b>Why this test exists:</b> Ensures the {@code isBlank()} check cannot be removed
     * or replaced with {@code isEmpty()}. A string of spaces is blank but not empty.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Removed call to isBlank</li>
     *   <li>Replaced isBlank with isEmpty (would pass with empty but fail with spaces)</li>
     * </ul>
     */
    @Test
    void validateNotBlank_rejectsOnlySpaces() {
        assertThatThrownBy(() ->
                Validation.validateNotBlank("     ", "field"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("field must not be null or blank");
    }

    /**
     * Tests validateNotBlank with a single space character.
     *
     * <p><b>Why this test exists:</b> Edge case for blank check - a single space should
     * be rejected. Tests the isBlank() implementation for minimal whitespace.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Negated conditional in isBlank check</li>
     * </ul>
     */
    @Test
    void validateNotBlank_rejectsSingleSpace() {
        assertThatThrownBy(() ->
                Validation.validateNotBlank(" ", "field"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("field must not be null or blank");
    }

    /**
     * Tests validateNotBlank accepts a single non-whitespace character.
     *
     * <p><b>Why this test exists:</b> Ensures the isBlank check correctly accepts
     * minimal valid input (single character). Catches mutants that incorrectly
     * implement the blank check.
     *
     * <p><b>Mutants killed:</b>
     * <ul>
     *   <li>Inverted isBlank return value</li>
     * </ul>
     */
    @Test
    void validateNotBlank_acceptsSingleCharacter() {
        assertThatNoException().isThrownBy(() ->
                Validation.validateNotBlank("A", "field"));
    }
}
