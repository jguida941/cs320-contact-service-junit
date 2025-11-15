package contactapp;

// JUnit 5 core test annotations
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

// AssertJ for object field checks
import static org.assertj.core.api.Assertions.assertThat;

// JUnit 5 assertions for exceptions, etc.
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests the Contact class.
 * Verifies:
 * - successful creation with valid data,
 * - updates via setters with valid data,
 * - and that invalid inputs cause the constructor to throw an exception.
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

    // CsvSource provides multiple sets of invalid input values for the test below
    // Each line is one test case (contactId, firstName, lastName, phone, address)
    @CsvSource(
            value = {
                    "' ', firstName, lastName, 1234567890, '7622 Main Street'"  // empty id with space to check for trim
                    , "'', firstName, lastName, 1234567890, '7622 Main Street'" // empty id
                    , "null, firstName, lastName, 1234567890, '7622 Main Street'" // null id
                    , "12345678901, firstName, lastName, 1234567890, '7622 Main Street'" // id too long

                    , "1, ' ', lastName, 1234567890, '7622 Main Street'" // empty first name with space
                    , "1, '', lastName, 1234567890, '7622 Main Street'" // empty first name
                    , "1, null, lastName, 1234567890, '7622 Main Street'" // null first name
                    , "1, 'TooLongFirstName', lastName, 1234567890, '7622 Main Street'" // first name too long

                    , "1, firstName, ' ' , 1234567890, '7622 Main Street'" // empty last name with space
                    , "1, firstName, '', 1234567890, '7622 Main Street'" // empty last name
                    , "1, firstName, null, 1234567890, '7622 Main Street'" // null last name
                    , "1, firstName, 'TooLongLastName', 1234567890, '7622 Main Street'" // last name too long

                    , "1, firstName, lastName, ' ' , '7622 Main Street'," // empty phone with space
                    , "1, firstName, lastName, '', '7622 Main Street'" // empty phone
                    , "1, firstName, lastName, null, '7622 Main Street'" // null phone
                    , "1, firstName, lastName, 123456789, '7622 Main Street'" // phone too short
                    , "1, firstName, lastName, 12345678901, '7622 Main Street'" // phone too long
                    , "1, firstName, lastName, 12345abcde, '7622 Main Street'" // phone with letters
                    , "1, firstName, lastName, 123-456-7890, '7622 Main Street'" // phone with special chars
                    , "1, firstName, lastName, 123 555 7855, '7622 Main Street'" // phone with spaces

                    , "1, firstName, lastName, 1234567890, ' '" // empty address with space
                    , "1, firstName, lastName, 1234567890, ''" // empty address
                    , "1, firstName, lastName, 1234567890, null" // null address
                    , "1, firstName, lastName, 1234567890, 'This address is way too long to be valid'" // address too long
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
            String address
    ) {
        // Check that creating a Contact with invalid inputs throws an exception
        assertThrows(IllegalArgumentException.class, () -> {
            new Contact(contactId, firstName, lastName, phone, address);
        });
    }

    @CsvSource(
            value = {
                    "' '",             // space-only first name
                    "''",              // empty first name
                    "null",            // null first name
                    "TooLongFirstName" // first name too long
            },
            nullValues = "null"
    )

    @ParameterizedTest
    void testFailedSetFirstName(String invalidFirstName) {
        Contact contact = new Contact("1", "firstName", "lastName", "1234567890", "7622 Main Street");
        assertThrows(IllegalArgumentException.class, () -> {
            contact.setFirstName(invalidFirstName);
        });
    }
}

