package contactapp.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardHost;
import org.junit.jupiter.api.Test;
import org.springframework.boot.tomcat.TomcatContextCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies {@link TomcatConfig} wires the custom JSON error valve into embedded Tomcat.
 */
class TomcatConfigTest {

    @Test
    void customizerRegistersJsonErrorValve() {
        TomcatConfig config = new TomcatConfig();
        WebServerFactoryCustomizer<TomcatServletWebServerFactory> customizer = config.tomcatCustomizer();
        assertThat(customizer).as("Bean must not be null so PIT cannot replace it with null").isNotNull();

        CapturingFactory factory = new CapturingFactory();
        customizer.customize(factory);

        assertThat(factory.customizers)
                .as("Factory should receive at least one context customizer")
                .isNotEmpty();

        StandardHost host = new StandardHost();
        StandardContext context = new StandardContext();
        context.setParent(host);
        host.setErrorReportValveClass("org.apache.catalina.valves.ErrorReportValve");

        factory.customizers.forEach(c -> c.customize(context));

        assertThat(host.getErrorReportValveClass())
                .isEqualTo(JsonErrorReportValve.class.getName());
    }

    @Test
    void customizerDoesNothingWhenParentIsNotHost() {
        TomcatConfig config = new TomcatConfig();
        CapturingFactory factory = new CapturingFactory();
        config.tomcatCustomizer().customize(factory);

        StandardContext context = new StandardContext();
        context.setParent(new StandardContext()); // not a StandardHost, so the guard should skip

        factory.customizers.forEach(c -> c.customize(context));

        // Nothing to assert other than no exceptions and the customizer list existing.
        assertThat(factory.customizers).isNotEmpty();
    }

    private static final class CapturingFactory extends TomcatServletWebServerFactory {
        private final List<TomcatContextCustomizer> customizers = new ArrayList<>();

        @Override
        public void addContextCustomizers(final TomcatContextCustomizer... customizers) {
            this.customizers.addAll(Arrays.asList(customizers));
        }
    }
}
