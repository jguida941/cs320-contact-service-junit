package contactapp.config;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logback converter that masks PII (Personally Identifiable Information) in log messages.
 *
 * <p>This converter automatically detects and masks sensitive data:
 * <ul>
 *   <li><b>Phone numbers:</b> Shows only last 4 digits (e.g., "***-***-1234")</li>
 *   <li><b>Addresses:</b> Shows only city/state, masks street/zip (e.g., "*** Cambridge, MA ***")</li>
 * </ul>
 *
 * <p>The masking patterns are designed to match the Contact entity fields:
 * <ul>
 *   <li>Phone: 10-digit numbers (raw or formatted as XXX-XXX-XXXX)</li>
 *   <li>Address: Common formats with street, city, state, zip</li>
 * </ul>
 *
 * <h2>Usage in logback-spring.xml</h2>
 * <pre>
 * &lt;conversionRule conversionWord="pii"
 *                 converterClass="contactapp.config.PiiMaskingConverter" /&gt;
 *
 * &lt;pattern&gt;%d{ISO8601} [%thread] %-5level %logger - %pii%n&lt;/pattern&gt;
 * </pre>
 *
 * <h2>Examples</h2>
 * <pre>
 * Input:  "Contact created with phone 6175551234"
 * Output: "Contact created with phone ***-***-1234"
 *
 * Input:  "Address: 123 Main St, Cambridge, MA 02139"
 * Output: "Address: *** Cambridge, MA ***"
 * </pre>
 */
public class PiiMaskingConverter extends MessageConverter {

    private static final int EXPECTED_PHONE_DIGIT_COUNT = 10;
    private static final int LAST_FOUR_DIGIT_START = EXPECTED_PHONE_DIGIT_COUNT - 4;

    /**
     * Pattern to match 10-digit phone numbers (with or without formatting).
     * Matches: 6175551234, 617-555-1234, (617) 555-1234
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "\\b(?:\\(?\\d{3}\\)?[-.\\s]?)?\\d{3}[-.\\s]?\\d{4}\\b"
    );

    /**
     * Pattern to match street addresses.
     * Captures: street number/name, city, state, zip
     * Example: "123 Main St, Cambridge, MA 02139"
     */
    private static final Pattern ADDRESS_PATTERN = Pattern.compile(
            "\\b\\d+\\s+[A-Za-z\\s]+(?:St|Street|Ave|Avenue|Rd|Road|Blvd|Boulevard|Dr|Drive|Ln|Lane|Way|Ct|Court),?\\s+"
            + "([A-Za-z\\s]+),\\s+([A-Z]{2})(?:\\s+\\d{5}(?:-\\d{4})?)?\\b"
    );

    /**
     * Converts the log message by masking PII.
     *
     * @param event the logging event
     * @return the masked log message
     */
    @Override
    public String convert(final ILoggingEvent event) {
        String message = event.getFormattedMessage();

        if (message == null) {
            return null;
        }

        // Mask phone numbers
        message = maskPhoneNumbers(message);

        // Mask addresses
        message = maskAddresses(message);

        return message;
    }

    /**
     * Masks phone numbers in the message, showing only last 4 digits.
     *
     * @param message the original message
     * @return the message with masked phone numbers
     */
    private String maskPhoneNumbers(final String message) {
        final Matcher matcher = PHONE_PATTERN.matcher(message);
        final StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            final String phone = matcher.group();
            // Extract just the digits
            final String digits = phone.replaceAll("[^0-9]", "");

            if (digits.length() == EXPECTED_PHONE_DIGIT_COUNT) {
                // Show last 4 digits: ***-***-1234
                final String lastFour = digits.substring(LAST_FOUR_DIGIT_START);
                matcher.appendReplacement(sb, "***-***-" + lastFour);
            } else {
                // If not 10 digits, mask entirely
                matcher.appendReplacement(sb, "***-***-****");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Masks addresses in the message, showing only city and state.
     *
     * @param message the original message
     * @return the message with masked addresses
     */
    private String maskAddresses(final String message) {
        final Matcher matcher = ADDRESS_PATTERN.matcher(message);
        final StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            final String city = matcher.group(1);
            final String state = matcher.group(2);
            // Preserve city and state, mask street and zip
            matcher.appendReplacement(sb, "*** " + city + ", " + state + " ***");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
