package contactapp.persistence.entity;

import contactapp.domain.Validation;
import contactapp.security.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * JPA entity mirroring the {@link contactapp.domain.Contact} structure.
 *
 * <p>Domain objects remain final/value-focused while this mutable entity exists purely
 * for persistence. Column lengths mirror {@link Validation} constants so the database
 * schema always matches domain constraints.
 */
@Entity
@Table(
        name = "contacts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_contacts_contact_id_user_id",
                columnNames = {"contact_id", "user_id"}))
public class ContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contact_id", length = Validation.MAX_ID_LENGTH, nullable = false)
    private String contactId;

    @Column(name = "first_name", length = Validation.MAX_NAME_LENGTH, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = Validation.MAX_NAME_LENGTH, nullable = false)
    private String lastName;

    @Column(name = "phone", length = Validation.PHONE_LENGTH, nullable = false)
    private String phone;

    @Column(name = "address", length = Validation.MAX_ADDRESS_LENGTH, nullable = false)
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Protected no-arg constructor required by JPA. */
    protected ContactEntity() {
        // JPA only
    }

    public ContactEntity(
            final String contactId,
            final String firstName,
            final String lastName,
            final String phone,
            final String address,
            final User user) {
        this.contactId = contactId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getContactId() {
        return contactId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }
}
