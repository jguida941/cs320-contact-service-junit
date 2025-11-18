package contactapp;
/**
 * Contact domain object.
 *
 * Enforces all field constraints from the requirements:
 * - contactId: non-null, length 1–10, not updatable
 * - firstName/lastName: non-null, length 1–10
 * - phone: non-null, exactly 10 numeric digits
 * - address: non-null, length 1–30
 *
 * All violations result in {@link IllegalArgumentException} being thrown
 * by the underlying {@link Validation} helper.
 */

public class Contact {
    private static final int MIN_LENGTH = 1;
    private static final int ID_MAX_LENGTH = 10;
    private static final int NAME_MAX_LENGTH = 10;
    private static final int ADDRESS_MAX_LENGTH = 30;
    private static final int PHONE_LENGTH = 10;

    private final String contactId;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;

    /**
     * Creates a new Contact with the given values.
     *
     * @throws IllegalArgumentException if any field violates the Contact constraints
     */
    public Contact(
            final String contactId,
            final String firstName,
            final String lastName,
            final String phone,
            final String address) {

        // Use Validation utility for constructor field checks
        Validation.validateLength(contactId, "contactId", MIN_LENGTH, ID_MAX_LENGTH);
        this.contactId = contactId.trim(); // normalize ID by trimming whitespace

        // Reuse setter validation for the mutable fields
        setFirstName(firstName);
        setLastName(lastName);
        setPhone(phone);
        setAddress(address);
    }

    // Getters
    public String getContactId() {
        return contactId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    // Setters
    public void setFirstName(final String firstName) {
        Validation.validateLength(firstName, "firstName", MIN_LENGTH, NAME_MAX_LENGTH);
        this.firstName = firstName.trim(); // trim once after validation
    }

    public void setLastName(final String lastName) {
        Validation.validateLength(lastName, "lastName", MIN_LENGTH, NAME_MAX_LENGTH);
        this.lastName = lastName.trim(); // trim once after validation
    }

    public void setPhone(final String phone) {
        Validation.validateNumeric10(phone, "phone", PHONE_LENGTH);
        this.phone = phone;
    }

    public void setAddress(final String address) {
        Validation.validateLength(address, "address", MIN_LENGTH, ADDRESS_MAX_LENGTH);
        this.address = address.trim(); // trim once after validation
    }
}
