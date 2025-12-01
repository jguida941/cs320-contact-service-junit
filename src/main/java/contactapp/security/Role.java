package contactapp.security;

/**
 * User roles for authorization.
 * Used with @PreAuthorize annotations for role-based access control.
 */
public enum Role {
    /**
     * Standard user with read/write access to their own resources.
     */
    USER,

    /**
     * Administrator with full access to all resources.
     */
    ADMIN
}
