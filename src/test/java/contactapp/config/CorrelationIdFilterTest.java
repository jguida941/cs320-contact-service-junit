package contactapp.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Exercises {@link CorrelationIdFilter} end-to-end so PIT cannot remove the
 * sanitization logic or MDC cleanup. The previous mutation report flagged
 * {@code sanitizeCorrelationId} as uncovered, so these tests intentionally
 * cover valid, blank, invalid-character, and over-length inputs.
 */
class CorrelationIdFilterTest {

    private CorrelationIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        MDC.clear();
    }

    @Test
    void doFilterInternal_preservesValidHeader() throws ServletException, IOException {
        final String headerValue = "abc-123_DEF";
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, headerValue);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        final List<String> valuesSeenInChain = new ArrayList<>();
        final FilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(final ServletRequest req, final ServletResponse res)
                    throws IOException, ServletException {
                valuesSeenInChain.add(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY));
                super.doFilter(req, res);
            }
        };

        filter.doFilterInternal(request, response, chain);

        assertThat(valuesSeenInChain).singleElement().isEqualTo(headerValue);
        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(headerValue);
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void doFilterInternal_generatesIdWhenHeaderInvalid() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "  invalid header\t");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        final String assignedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(assignedId).isNotBlank();
        assertThat(assignedId).matches("[a-zA-Z0-9\\-_]+");
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void doFilterInternal_rejectsOverlyLongIds() throws ServletException, IOException {
        final String longId = UUID.randomUUID().toString().repeat(3);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, longId);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        final String assignedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(assignedId).isNotBlank().isNotEqualTo(longId);
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }

    /**
     * Tests the exact boundary condition for correlation ID length. A 64-character
     * ID is valid, but a 65-character ID must be rejected. This ensures PIT cannot
     * mutate the boundary check from {@code >} to {@code >=} without detection.
     */
    @Test
    void doFilterInternal_acceptsIdAtMaxLength() throws ServletException, IOException {
        final String maxLengthId = "a".repeat(64);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, maxLengthId);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(maxLengthId);
    }

    @Test
    void doFilterInternal_rejectsIdJustOverMaxLength() throws ServletException, IOException {
        final String overMaxId = "a".repeat(65);
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, overMaxId);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        final String assignedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(assignedId).isNotEqualTo(overMaxId);
    }

    @Test
    void doFilterInternal_generatesIdWhenHeaderMissing() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        final String assignedId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(assignedId).isNotBlank();
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }

    @Test
    void doFilterInternal_trimsValidHeaderBeforePropagating() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "  abc-123  ");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertThat(response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo("abc-123");
        assertThat(MDC.get(CorrelationIdFilter.CORRELATION_ID_KEY)).isNull();
    }
}
