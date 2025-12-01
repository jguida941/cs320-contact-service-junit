package contactapp.security;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

    @Test
    void loadUserReturnsUserDetailsWhenFound() {
        User user = new User("tester", "tester@example.com", "$2a$10$dummyBcryptHashForTesting123456", Role.USER);
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));

        UserDetails result = service.loadUserByUsername("tester");

        assertThat(result.getUsername()).isEqualTo("tester");
    }

    @Test
    void loadUserThrowsWhenMissing() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
