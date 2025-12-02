package contactapp.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Test utility for date-related test fixtures.
 *
 * <p>Provides future dates that will never be "in the past" for validation tests.
 * Using constants/helpers avoids hardcoded dates that become stale over time.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // For LocalDate fields (Task.dueDate)
 * Task task = new Task("1", "Name", "Desc", TaskStatus.TODO, TestDates.FUTURE_DATE);
 *
 * // For java.util.Date fields (Appointment.appointmentDate)
 * Appointment apt = new Appointment("1", TestDates.FUTURE_LEGACY_DATE, "Meeting");
 *
 * // For Clock-based validation testing
 * Validation.validateDateNotPast(date, "field", TestDates.FIXED_CLOCK);
 * }</pre>
 */
public final class TestDates {

    private TestDates() {
        // Utility class
    }

    // ==================== Future Date Constants ====================

    /**
     * A LocalDate far in the future (year 2099) - will never be "in the past".
     * Use for Task.dueDate and other LocalDate fields in tests.
     */
    public static final LocalDate FUTURE_DATE = LocalDate.of(2099, 12, 31);

    /**
     * A LocalDate clearly in the past (year 2000) - will always be "in the past".
     * Use for tests that verify past-date rejection.
     */
    public static final LocalDate PAST_DATE = LocalDate.of(2000, 1, 1);

    /**
     * A java.util.Date far in the future - will never be "in the past".
     * Use for Appointment.appointmentDate in tests.
     */
    public static final Date FUTURE_LEGACY_DATE = Date.from(
            FUTURE_DATE.atStartOfDay(ZoneOffset.UTC).toInstant());

    // ==================== Fixed Clock for Deterministic Testing ====================

    /**
     * Fixed instant for deterministic date validation tests.
     * Set far enough in the future that futureDate(N) returns dates
     * still in the future relative to any realistic test run.
     */
    public static final Instant FIXED_INSTANT = Instant.parse("2098-06-15T12:00:00Z");

    /**
     * Fixed clock at FIXED_INSTANT for injecting into validation methods.
     * Use with {@code Validation.validateDateNotPast(date, label, TestDates.FIXED_CLOCK)}.
     */
    public static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    /**
     * LocalDate representing "today" in the fixed clock's timezone.
     */
    public static final LocalDate FIXED_TODAY = LocalDate.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);

    // ==================== Helper Methods (Deterministic, based on FIXED_TODAY) ====================

    /**
     * Returns a date N days after FIXED_TODAY.
     * Deterministic - independent of wall-clock time.
     *
     * @param daysFromFixedToday number of days after FIXED_TODAY
     * @return LocalDate that many days after the fixed reference point
     */
    public static LocalDate futureDate(final int daysFromFixedToday) {
        return FIXED_TODAY.plusDays(daysFromFixedToday);
    }

    /**
     * Returns a date 30 days after FIXED_TODAY.
     * Deterministic convenience method.
     *
     * @return LocalDate 30 days after FIXED_TODAY
     */
    public static LocalDate futureDate() {
        return futureDate(30);
    }

    /**
     * Returns a java.util.Date N days after FIXED_TODAY.
     * Deterministic - independent of wall-clock time.
     *
     * @param daysFromFixedToday number of days after FIXED_TODAY
     * @return Date that many days after the fixed reference point
     */
    public static Date futureLegacyDate(final int daysFromFixedToday) {
        return Date.from(futureDate(daysFromFixedToday).atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}
