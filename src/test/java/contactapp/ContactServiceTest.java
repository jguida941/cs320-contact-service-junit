package contactapp;
// JUnit 5 core test annotations
import org.junit.jupiter.api.Test;
// AssertJ for object field checks
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class ContactServiceTest {

    // Check that ContactService singleton instance is not null
    @Test
    void testContactService() {
        assertThat(ContactService.getInstance()).isNotNull();
    }
}
