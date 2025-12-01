package contactapp.persistence.mapper;

import contactapp.domain.Contact;
import contactapp.persistence.entity.ContactEntity;
import contactapp.security.User;
import org.springframework.stereotype.Component;

/**
 * Converts between {@link Contact} domain objects and {@link ContactEntity} rows.
 *
 * <p>{@link #toDomain(ContactEntity)} reuses the domain constructor so corrupted
 * database rows still go through {@link contactapp.domain.Validation}.
 */
@Component
public class ContactMapper {

    /**
     * Converts a domain Contact to an entity, associating it with the given user.
     *
     * @param domain the domain contact
     * @param user the user who owns this contact
     * @return the entity representation
     */
    public ContactEntity toEntity(final Contact domain, final User user) {
        if (domain == null) {
            return null;
        }
        return new ContactEntity(
                domain.getContactId(),
                domain.getFirstName(),
                domain.getLastName(),
                domain.getPhone(),
                domain.getAddress(),
                user);
    }

    /**
     * Converts a domain Contact to an entity using the existing user from the entity.
     *
     * <p>This method should only be used when updating an existing entity that
     * already has a user association. For new entities, use {@link #toEntity(Contact, User)}.
     *
     * @param domain the domain contact
     * @throws UnsupportedOperationException always; kept for binary compatibility only
     * @deprecated Use {@link #toEntity(Contact, User)} instead. This stub intentionally throws to
     * discourage callers from forgetting to include a {@link User} association.
     */
    @Deprecated
    public ContactEntity toEntity(final Contact domain) {
        if (domain == null) {
            return null;
        }
        // This is used for updates where the entity already exists with a user
        // The JpaStore will handle fetching the existing entity and preserving the user
        throw new UnsupportedOperationException(
                "Use toEntity(Contact, User) instead. This method requires a User parameter.");
    }

    public Contact toDomain(final ContactEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Contact(
                entity.getContactId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getPhone(),
                entity.getAddress());
    }

    /**
     * Updates an existing entity with the latest mutable fields from the domain aggregate.
     *
     * @param target the entity to mutate; must not be null
     * @param source the domain contact providing the latest data; must not be null
     */
    public void updateEntity(final ContactEntity target, final Contact source) {
        if (target == null) {
            throw new IllegalArgumentException("target entity must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("source contact must not be null");
        }
        target.setFirstName(source.getFirstName());
        target.setLastName(source.getLastName());
        target.setPhone(source.getPhone());
        target.setAddress(source.getAddress());
    }
}
