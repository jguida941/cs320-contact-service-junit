package contactapp.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Annotation to populate the SecurityContext with a real {@link User} instance.
 *
 * <p>Unlike {@code @WithMockUser} which creates a generic UserDetails stub,
 * this annotation creates our concrete {@link User} entity that services
 * expect when calling {@code getCurrentUser()}.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * @Test
 * @WithMockAppUser
 * void myTest() {
 *     // SecurityContext now has a real User with id=1, username="testuser"
 * }
 *
 * @Test
 * @WithMockAppUser(username = "admin", role = Role.ADMIN)
 * void adminTest() {
 *     // SecurityContext has an ADMIN user
 * }
 * }</pre>
 *
 * @see WithMockAppUserSecurityContextFactory
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@WithSecurityContext(factory = WithMockAppUserSecurityContextFactory.class)
public @interface WithMockAppUser {

    /**
     * The user ID for the mock user.
     *
     * @return user ID (default 1L)
     */
    long id() default 1L;

    /**
     * The username for the mock user.
     *
     * @return username (default "testuser")
     */
    String username() default "testuser";

    /**
     * The email for the mock user.
     *
     * @return email (default "testuser@example.com")
     */
    String email() default "testuser@example.com";

    /**
     * The role for the mock user.
     *
     * @return role (default USER)
     */
    Role role() default Role.USER;
}
