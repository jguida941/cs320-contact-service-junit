package contactapp;
/**
 * Contact domain object.
 * Enforces all field constraints from the requirements:
 * - contactId: non-null, <= 10 chars, not updatable
 * - firstName/lastName: non-null, <= 10 chars
 * - phone: non-null, exactly 10 digits
 * - address: non-null, <= 30 chars
 */

public class Contact {
    private final String contactId;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;

    public Contact(
            String contactId,
            String firstName,
            String lastName,
            String phone,
            String address) {

        Validation.validateNotBlank(contactId, "contactId");
        Validation.validateLength(contactId, "contactId", 1, 10);
        this.contactId = contactId;

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
    public void setFirstName(String firstName) {
        Validation.validateNotBlank(firstName, "firstName");
        Validation.validateLength(firstName, "firstName", 1, 10);
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        Validation.validateNotBlank(lastName, "lastName");
        Validation.validateLength(lastName, "lastName", 1, 10);
        this.lastName = lastName;
    }
    public void setPhone(String phone) {
        Validation.validateNotBlank(phone, "phone");
        Validation.validateNumeric10(phone, "phone");
        this.phone = phone;
    }

    public void setAddress(String address) {
        Validation.validateNotBlank(address, "address");
        Validation.validateLength(address, "address", 1, 30);
        this.address = address;
    }
}
