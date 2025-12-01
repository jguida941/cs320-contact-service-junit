package contactapp.support;

import contactapp.security.Role;
import contactapp.security.User;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for creating {@link User} instances in tests.
 *
 * <p>Provides deterministic but unique usernames/emails plus a valid BCrypt hash so
 * persistence tests can satisfy foreign key constraints without duplicating setup code.
 */
public final class TestUserFactory {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);
    private static final String PASSWORD_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOBiZFn0eTD1yQbFq6Z9erjRzCQXDpG7W"; // "password"

    private TestUserFactory() {
    }

    /**
     * Creates a unique USER-role {@link User}.
     *
     * @return a new user with random suffix
     */
    public static User createUser() {
        return createUser("testuser" + COUNTER.getAndIncrement());
    }

    /**
     * Creates a USER-role {@link User} with the given username.
     *
     * @param username the username to use
     * @return the user
     */
    public static User createUser(final String username) {
        return createUser(username, username + "@example.com");
    }

    /**
     * Creates a USER-role {@link User} with the given username and email.
     *
     * @param username the username
     * @param email the email
     * @return the user
     */
    public static User createUser(final String username, final String email) {
        return createUser(username, email, Role.USER);
    }

    /**
     * Creates a {@link User} with the provided attributes.
     *
     * @param username the username
     * @param email the email
     * @param role the desired role
     * @return the configured user
     */
    public static User createUser(final String username, final String email, final Role role) {
        return new User(username, email, PASSWORD_HASH, role);
    }

    /**
     * Creates an ADMIN-role {@link User} with a generated username.
     *
     * @return the user
     */
    public static User createAdmin() {
        return createUser("admin" + COUNTER.getAndIncrement(), "admin@example.com", Role.ADMIN);
    }
}
