package contactapp;

import contactapp.api.ContactController;
import contactapp.api.dto.ContactResponse;
import contactapp.domain.Contact;
import contactapp.service.ContactService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContactController} covering the {@code ?all=true} branch
 * that PIT previously reported as uncovered.
 */
class ContactControllerUnitTest {

    private final ContactService contactService = mock(ContactService.class);
    private final ContactController controller = new ContactController(contactService);

    @Test
    void getAll_withAllFlagDelegatesToAdminService() {
        final Contact contact = new Contact("admin1", "Amy", "Lee", "1234567890", "100 Admin Way");
        when(contactService.getAllContactsAllUsers()).thenReturn(List.of(contact));
        final Authentication adminAuth = mockAuthentication("ROLE_ADMIN");

        final List<ContactResponse> responses = controller.getAll(true, adminAuth);

        verify(contactService).getAllContactsAllUsers();
        assertThat(responses).singleElement().satisfies(response ->
                assertThat(response.id()).isEqualTo("admin1"));
    }

    @Test
    void getAll_withoutAllFlagUsesUserScopedService() {
        when(contactService.getAllContacts()).thenReturn(List.of());
        final Authentication userAuth = mockAuthentication("ROLE_USER");

        controller.getAll(false, userAuth);

        verify(contactService).getAllContacts();
        verify(contactService, never()).getAllContactsAllUsers();
    }

    private Authentication mockAuthentication(final String role) {
        final Authentication auth = mock(Authentication.class);
        final List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();
        return auth;
    }
}
