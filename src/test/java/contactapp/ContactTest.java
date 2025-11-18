package contactapp;

// JUnit 5 core test annotations
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// AssertJ for object field checks
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests the Contact class.
 * Verifies:
 * - successful creation with valid data,
 * - updates via setters with valid data,
 * - and that invalid inputs cause the constructor/setters to throw IllegalArgumentException
 * with the specific validation messages emitted by the Validation helper.
 */
public class ContactTest {

    // Simple test to check for contactId, firstName, lastName,
    // phone, and address field values
    // @test tests for good path creation of a Contact object
    @Test
    void testSuccessfulCreation() {
        Contact contact = new Contact(
                "1",
                "firstName",
                "lastName",
                "1234567890",
                "7622 Main Street"
        );

        // Check that each field has the expected value for the contact object
        assertThat(contact)
                .hasFieldOrPropertyWithValue("contactId", "1")
                .hasFieldOrPropertyWithValue("firstName", "firstName")
                .hasFieldOrPropertyWithValue("lastName", "lastName")
                .hasFieldOrPropertyWithValue("phone", "1234567890")
                .hasFieldOrPropertyWithValue("address", "7622 Main Street");
    }

    // Test that updating values with the setters works with valid data
    @Test
    void testValidSetters() {
        Contact contact = new Contact("1", "firstName", "lastName", "1234567890", "7622 Main Street");
        contact.setFirstName("Justin");
        contact.setLastName("Guida");
        contact.setPhone("0987654321");
        contact.setAddress("1234 Taro Street");

        // Check that each field has the updated value for the contact object
        assertThat(contact)
                .hasFieldOrPropertyWithValue("firstName", "Justin")
                .hasFieldOrPropertyWithValue("lastName", "Guida")
                .hasFieldOrPropertyWithValue("phone", "0987654321")
                .hasFieldOrPropertyWithValue("address", "1234 Taro Street");
    }

    // Constructor should trim leading/trailing whitespace for all stored fields
    @Test
    void testConstructorTrimsStoredValues() {
        Contact contact = new Contact(
                " 100 ",
                "  Alice ",
                "  Smith  ",
                "1234567890",
                " 742 Evergreen Terrace  "
        );

        assertThat(contact.getContactId()).isEqualTo("100");
        assertThat(contact.getFirstName()).isEqualTo("Alice");
        assertThat(contact.getLastName()).isEqualTo("Smith");
        assertThat(contact.getAddress()).isEqualTo("742 Evergreen Terrace");
    }

    // CsvSource provides multiple sets of invalid input values for the test below
    // Each line is one test case (contactId, firstName, lastName, phone, address)
    @CsvSource(
            value = {
                    // contactId validation
                    "' ', firstName, lastName, 1234567890, '7622 Main Street', 'contactId must not be null or blank'"
                    , "'', firstName, lastName, 1234567890, '7622 Main Street', 'contactId must not be null or blank'"
                    , "null, firstName, lastName, 1234567890, '7622 Main Street', 'contactId must not be null or blank'"
                    , "12345678901, firstName, lastName, 1234567890, '7622 Main Street', 'contactId length must be between 1 and 10'"

                    // firstName validation
                    , "1, ' ', lastName, 1234567890, '7622 Main Street', 'firstName must not be null or blank'"
                    , "1, '', lastName, 1234567890, '7622 Main Street', 'firstName must not be null or blank'"
                    , "1, null, lastName, 1234567890, '7622 Main Street', 'firstName must not be null or blank'"
                    , "1, 'TooLongFirstName', lastName, 1234567890, '7622 Main Street', 'firstName length must be between 1 and 10'"

                    // lastName validation
                    , "1, firstName, ' ' , 1234567890, '7622 Main Street', 'lastName must not be null or blank'"
                    , "1, firstName, '', 1234567890, '7622 Main Street', 'lastName must not be null or blank'"
                    , "1, firstName, null, 1234567890, '7622 Main Street', 'lastName must not be null or blank'"
                    , "1, firstName, 'TooLongLastName', 1234567890, '7622 Main Street', 'lastName length must be between 1 and 10'"
                    , "1, ' firstNameWithSpaces ', lastName, 1234567890, '7622 Main Street', 'firstName length must be between 1 and 10'"

                    // phone validation
                    , "1, firstName, lastName, ' ' , '7622 Main Street', 'phone must not be null or blank'"
                    , "1, firstName, lastName, '', '7622 Main Street', 'phone must not be null or blank'"
                    , "1, firstName, lastName, null, '7622 Main Street', 'phone must not be null or blank'"
                    , "1, firstName, lastName, 123456789, '7622 Main Street', 'phone must be exactly 10 digits'"
                    , "1, firstName, lastName, 12345678901, '7622 Main Street', 'phone must be exactly 10 digits'"
                    , "1, firstName, lastName, 12345abcde, '7622 Main Street', 'phone must only contain digits 0-9'"
                    , "1, firstName, lastName, 123-456-7890, '7622 Main Street', 'phone must only contain digits 0-9'"
                    , "1, firstName, lastName, 123 555 7855, '7622 Main Street', 'phone must only contain digits 0-9'"

                    // address validation
                    , "1, firstName, lastName, 1234567890, ' ', 'address must not be null or blank'"
                    , "1, firstName, lastName, 1234567890, '', 'address must not be null or blank'"
                    , "1, firstName, lastName, 1234567890, null, 'address must not be null or blank'"
                    , "1, firstName, lastName, 1234567890, 'This address is way too long to be valid', 'address length must be between 1 and 30'"
            },

            // Specify that the string "null" should be treated as a null value
            nullValues = "null"
    )

    // Parameterized test: this method will run once for each set of input values we define
    // it tests for bad path creation of a Contact object
    @ParameterizedTest
    void testFailedCreation(
            String contactId,
            String firstName,
            String lastName,
            String phone,
            String address,
            String expectedMessage // expected exception message
    ) {

        // Check that creating a Contact with invalid inputs throws an exception with the expected message
        assertThatThrownBy(() -> new Contact(contactId, firstName, lastName, phone, address))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }

    @CsvSource(
            value = {
                    "' ', 'firstName must not be null or blank'"
                    , "'', 'firstName must not be null or blank'"
                    , "null, 'firstName must not be null or blank'"
                    , "TooLongFirstName, 'firstName length must be between 1 and 10'"
            },
            nullValues = "null"
    )

    @ParameterizedTest
    void testFailedSetFirstName(String invalidFirstName, String expectedMessage) {
        Contact contact = new Contact("1", "firstName", "lastName", "1234567890", "7622 Main Street");

        // Check that invalid firstName updates throw the proper exception message
        assertThatThrownBy(() -> contact.setFirstName(invalidFirstName))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(expectedMessage);
    }
}
