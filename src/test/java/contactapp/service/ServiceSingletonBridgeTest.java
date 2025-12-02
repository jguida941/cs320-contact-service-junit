package contactapp.service;

import contactapp.config.ApplicationContextProvider;
import java.lang.reflect.Field;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@code getInstance()} delegates to the Spring {@link ApplicationContext}
 * once it is available, ensuring legacy callers receive the DI-managed proxy instead of
 * spinning up isolated in-memory stores.
 */
@Isolated
class ServiceSingletonBridgeTest {

    private ApplicationContext originalContext;
    private Object originalContactInstance;
    private Object originalTaskInstance;
    private Object originalAppointmentInstance;

    @BeforeEach
    void snapshotStatics() throws Exception {
        originalContext = readContext();
        originalContactInstance = readServiceInstance(ContactService.class);
        originalTaskInstance = readServiceInstance(TaskService.class);
        originalAppointmentInstance = readServiceInstance(AppointmentService.class);
    }

    @AfterEach
    void restoreStatics() throws Exception {
        installContext(originalContext);
        setServiceInstance(ContactService.class, originalContactInstance);
        setServiceInstance(TaskService.class, originalTaskInstance);
        setServiceInstance(AppointmentService.class, originalAppointmentInstance);
    }

    @Test
    void contactServiceUsesSpringContextWhenAvailable() throws Exception {
        ContactService mockService = Mockito.mock(ContactService.class);
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.getBean(ContactService.class)).thenReturn(mockService);

        installContext(context);
        setServiceInstance(ContactService.class, null);

        assertThat(ContactService.getInstance()).isSameAs(mockService);
    }

    @Test
    void taskServiceUsesSpringContextWhenAvailable() throws Exception {
        TaskService mockService = Mockito.mock(TaskService.class);
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.getBean(TaskService.class)).thenReturn(mockService);

        installContext(context);
        setServiceInstance(TaskService.class, null);

        assertThat(TaskService.getInstance()).isSameAs(mockService);
    }

    @Test
    void appointmentServiceUsesSpringContextWhenAvailable() throws Exception {
        AppointmentService mockService = Mockito.mock(AppointmentService.class);
        ApplicationContext context = Mockito.mock(ApplicationContext.class);
        Mockito.when(context.getBean(AppointmentService.class)).thenReturn(mockService);

        installContext(context);
        setServiceInstance(AppointmentService.class, null);

        assertThat(AppointmentService.getInstance()).isSameAs(mockService);
    }

    /**
     * Legacy callers should still receive a usable service even if Spring has not
     * started yet; this guards against the null-return mutant in getInstance().
     */
    @Test
    void contactServiceProvidesFallbackWhenContextMissing() throws Exception {
        installContext(null);
        setServiceInstance(ContactService.class, null);
        assertThat(ContactService.getInstance()).isNotNull();
    }

    @Test
    void taskServiceProvidesFallbackWhenContextMissing() throws Exception {
        installContext(null);
        setServiceInstance(TaskService.class, null);
        assertThat(TaskService.getInstance()).isNotNull();
    }

    @Test
    void appointmentServiceProvidesFallbackWhenContextMissing() throws Exception {
        installContext(null);
        setServiceInstance(AppointmentService.class, null);
        assertThat(AppointmentService.getInstance()).isNotNull();
    }

    private static Object readServiceInstance(final Class<?> serviceClass) throws Exception {
        Field instanceField = serviceClass.getDeclaredField("instance");
        instanceField.setAccessible(true);
        return instanceField.get(null);
    }

    private static void setServiceInstance(
            final Class<?> serviceClass,
            final Object value) throws Exception {
        Field instanceField = serviceClass.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, value);
    }

    private static void installContext(final ApplicationContext context) throws Exception {
        Field contextField = ApplicationContextProvider.class.getDeclaredField("context");
        contextField.setAccessible(true);
        contextField.set(null, context);
    }

    private static ApplicationContext readContext() throws Exception {
        Field contextField = ApplicationContextProvider.class.getDeclaredField("context");
        contextField.setAccessible(true);
        return (ApplicationContext) contextField.get(null);
    }
}
