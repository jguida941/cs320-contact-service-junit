package contactapp.service;

import contactapp.api.exception.DuplicateResourceException;
import contactapp.domain.Contact;
import contactapp.persistence.store.ContactStore;
import contactapp.security.Role;
import contactapp.security.User;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ContactService mutation-sensitive logic.
 */
class ContactServiceMutationTest {

    private static final String PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOBiZFn0eTD1yQbFq6Z9erjRzCQXDpG7W";

    @AfterEach
    void resetSingleton() {
        ReflectionTestUtils.setField(ContactService.class, "instance", null);
        SecurityContextHolder.clearContext();
    }

    @Test
    void addContactThrowsDuplicateWhenStoreReportsExistingId() {
        ContactStore store = mock(ContactStore.class);
        ContactService service = new ContactService(store);
        Contact contact = new Contact("dup-1", "Persist", "Failure", "5555555555", "123 Retry Lane");

        when(store.existsById("dup-1")).thenReturn(true);

        assertThatThrownBy(() -> service.addContact(contact))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getAllContactsAllUsersThrowsForNonAdmins() {
        final ContactService service = new ContactService(mock(ContactStore.class));
        authenticate(Role.USER);

        assertThatThrownBy(service::getAllContactsAllUsers)
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void getAllContactsAllUsersReturnsCopiesForAdmins() {
        final ContactStore store = mock(ContactStore.class);
        final ContactService service = new ContactService(store);
        authenticate(Role.ADMIN);
        final Contact stored = new Contact("id1", "Amy", "Lee", "1112223333", "10 Main");
        when(store.findAll()).thenReturn(List.of(stored));

        assertThat(service.getAllContactsAllUsers()).singleElement().isNotSameAs(stored);
    }

    @Test
    void deleteContactReturnsStoreResult() {
        final ContactStore store = mock(ContactStore.class);
        final ContactService service = new ContactService(store);
        when(store.deleteById("known")).thenReturn(true);

        assertThat(service.deleteContact("known")).isTrue();
        assertThat(service.deleteContact("missing")).isFalse();
    }

    @Test
    void updateContactReturnsFalseWhenMissing() {
        final ContactStore store = mock(ContactStore.class);
        final ContactService service = new ContactService(store);
        when(store.findById("missing")).thenReturn(Optional.empty());

        assertThat(service.updateContact("missing", "Amy", "Lee", "1112223333", "Addr")).isFalse();
    }

    @Test
    void updateContactMutatesExistingRecord() {
        final ContactStore store = mock(ContactStore.class);
        final ContactService service = new ContactService(store);
        final Contact existing = new Contact("id5", "Amy", "Lee", "1112223333", "Addr");
        when(store.findById("id5")).thenReturn(Optional.of(existing));

        assertThat(service.updateContact("id5", "Beth", "Ray", "9998887777", "New Addr")).isTrue();
        final ArgumentCaptor<Contact> captor = ArgumentCaptor.forClass(Contact.class);
        verify(store).save(captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Beth");
        assertThat(captor.getValue().getPhone()).isEqualTo("9998887777");
    }

    @Test
    void getContactByIdReturnsCopyFromStore() {
        final ContactStore store = mock(ContactStore.class);
        final ContactService service = new ContactService(store);
        final Contact stored = new Contact("id7", "Amy", "Lee", "1112223333", "Addr");
        when(store.findById("id7")).thenReturn(Optional.of(stored));

        assertThat(service.getContactById("id7")).isPresent().get().isNotSameAs(stored);
    }

    private void authenticate(final Role role) {
        final User user = new User(role.name().toLowerCase(), role.name().toLowerCase() + "@example.com", PASSWORD_HASH, role);
        final var token = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
