package contactapp.persistence.repository;

import contactapp.persistence.entity.ContactEntity;
import contactapp.security.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Data repository for persisting contacts with auto-generated numeric primary keys while
 * preserving the natural {@code contactId} string used by legacy callers.
 *
 * <p>Supports per-user data isolation by filtering contacts by user_id.
 */
@Repository
public interface ContactRepository extends JpaRepository<ContactEntity, Long> {

    /**
     * Finds all contacts belonging to a specific user.
     *
     * @param user the user who owns the contacts
     * @return list of contacts for the given user
     */
    List<ContactEntity> findByUser(User user);

    /**
     * Finds a contact by ID and user.
     *
     * @param contactId the contact ID
     * @param user the user who owns the contact
     * @return optional containing the contact if found
     */
    Optional<ContactEntity> findByContactIdAndUser(String contactId, User user);

    /**
     * Finds a contact by ID without user scoping (legacy compatibility).
     *
     * @param contactId the contact ID
     * @return optional containing the contact if found
     * @deprecated Use {@link #findByContactIdAndUser(String, User)} for tenant isolation
     */
    @Deprecated(forRemoval = true)
    Optional<ContactEntity> findByContactId(String contactId);

    /**
     * Checks if a contact exists by ID and user.
     *
     * @param contactId the contact ID
     * @param user the user who owns the contact
     * @return true if the contact exists for the given user
     */
    boolean existsByContactIdAndUser(String contactId, User user);

    /**
     * Checks if a contact exists by ID regardless of user.
     *
     * @param contactId the contact ID
     * @return true if any contact exists with the ID
     * @deprecated Use {@link #existsByContactIdAndUser(String, User)} instead
     */
    @Deprecated(forRemoval = true)
    boolean existsByContactId(String contactId);

    /**
     * Deletes a contact by ID and user.
     *
     * @param contactId the contact ID
     * @param user the user who owns the contact
     * @return number of deleted records (0 or 1)
     */
    @Modifying
    @Transactional
    int deleteByContactIdAndUser(String contactId, User user);
}
