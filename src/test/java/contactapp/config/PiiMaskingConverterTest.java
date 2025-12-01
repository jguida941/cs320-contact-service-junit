package contactapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link PiiMaskingConverter} to guarantee the masking logic stays intact.
 *
 * <p>These assertions guard the log-scrubbing rules that PIT previously marked as
 * uncovered (NO_COVERAGE). By exercising both 10-digit phones and addresses we
 * ensure future changes cannot silently drop the masking behavior.
 */
class PiiMaskingConverterTest {

    private final PiiMaskingConverter converter = new PiiMaskingConverter();

    @Test
    void convert_masksPhonesAndAddresses() {
        final ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(
                "Contact created with phone 6175551234 at 123 Main St, Cambridge, MA 02139");

        final String masked = converter.convert(event);

        assertThat(masked)
                .contains("***-***-1234")
                .contains("*** Cambridge, MA ***");
    }

    @Test
    void convert_masksSevenDigitPhoneFallback() {
        final ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn("Short phone 555-1234 in text");

        final String masked = converter.convert(event);

        assertThat(masked).contains("***-***-****");
    }

    @Test
    void convert_handlesNullMessagesGracefully() {
        final ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getFormattedMessage()).thenReturn(null);

        assertThat(converter.convert(event)).isNull();
    }
}
