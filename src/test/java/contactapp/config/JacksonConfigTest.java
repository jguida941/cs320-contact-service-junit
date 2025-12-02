package contactapp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson2.autoconfigure.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ensures {@link JacksonConfig} disables string coercion so schema violations surface as 400s.
 */
class JacksonConfigTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        JacksonConfig config = new JacksonConfig();
        Jackson2ObjectMapperBuilderCustomizer customizer = config.strictCoercionCustomizer();
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        customizer.customize(builder);
        mapper = builder.build();
    }

    @Test
    void objectMapperRejectsBooleanCoercion() {
        assertThat(mapper).isNotNull();

        String payload = """
                {"value": false}
                """;

        assertThatThrownBy(() -> mapper.readValue(payload, SamplePayload.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void objectMapperRejectsNumericCoercion() {
        String payload = """
                {"value": 42}
                """;

        assertThatThrownBy(() -> mapper.readValue(payload, SamplePayload.class))
                .isInstanceOf(MismatchedInputException.class);
    }

    private static final class SamplePayload {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
