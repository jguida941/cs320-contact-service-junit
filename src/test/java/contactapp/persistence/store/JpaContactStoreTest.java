package contactapp.persistence.store;

import contactapp.domain.Contact;
import contactapp.persistence.entity.ContactEntity;
import contactapp.persistence.mapper.ContactMapper;
import contactapp.persistence.repository.ContactRepository;
import contactapp.security.User;
import contactapp.support.TestUserFactory;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies {@link JpaContactStore}'s repository delegations so PIT can't remove them.
 */
@ExtendWith(MockitoExtension.class)
class JpaContactStoreTest {

    @Mock
    private ContactRepository repository;
    @Mock
    private ContactMapper mapper;

    private JpaContactStore store;

    @BeforeEach
    void setUp() {
        store = new JpaContactStore(repository, mapper);
    }

    @Test
    void existsById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.existsById("c-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void existsById_withUserDelegatesToRepository() {
        final User owner = TestUserFactory.createUser("contact-store");
        when(repository.existsByContactIdAndUser("c-2", owner)).thenReturn(true);

        assertThat(store.existsById("c-2", owner)).isTrue();
    }

    /**
     * Covers the false branch so PIT can't flip the result of existsById(User).
     */
    @Test
    void existsById_withUserReturnsFalseWhenRepositoryReturnsFalse() {
        final User owner = TestUserFactory.createUser("contact-store-false");
        when(repository.existsByContactIdAndUser("missing", owner)).thenReturn(false);

        assertThat(store.existsById("missing", owner)).isFalse();
    }

    @Test
    void findById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.findById("c-3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void findById_withUserUsesMapper() {
        final User owner = TestUserFactory.createUser("contact-store-find");
        final ContactEntity entity = mock(ContactEntity.class);
        final Contact domain = new Contact("c-4", "Amy", "Lee", "1234567890", "Addr");
        when(repository.findByContactIdAndUser("c-4", owner)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        assertThat(store.findById("c-4", owner)).contains(domain);
    }

    @Test
    void deleteById_withoutUserThrowsUnsupportedOperation() {
        assertThatThrownBy(() -> store.deleteById("c-5"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void deleteById_withUserUsesRepositoryReturnCode() {
        final User owner = TestUserFactory.createUser("contact-store-delete");
        when(repository.deleteByContactIdAndUser("c-6", owner)).thenReturn(1);

        assertThat(store.deleteById("c-6", owner)).isTrue();
    }

    @Test
    void deleteById_withUserReturnsFalseWhenNoRowsDeleted() {
        final User owner = TestUserFactory.createUser("contact-store-delete-none");
        when(repository.deleteByContactIdAndUser("nonexistent", owner)).thenReturn(0);

        assertThat(store.deleteById("nonexistent", owner)).isFalse();
    }
}
