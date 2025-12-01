package contactapp.persistence.store;

import contactapp.domain.Contact;
import contactapp.security.User;

/**
 * Marker interface tying {@link Contact} aggregates to the generic {@link DomainDataStore}.
 */
public interface ContactStore extends DomainDataStore<Contact> {

    /**
     * Persists the aggregate on behalf of a specific user.
     *
     * <p>Default implementation throws so legacy in-memory stores are unaffected,
     * while JPA-backed stores can override with user-aware persistence logic.
     *
     * @param aggregate validated contact aggregate
     * @param user owner of the aggregate
     */
    default void save(final Contact aggregate, final User user) {
        throw new UnsupportedOperationException("User-scoped save is not supported");
    }
}
