package contactapp.domain;

/**
 * Enumeration of project statuses.
 *
 * <p>Defines the lifecycle states a project can be in:
 * <ul>
 *   <li>{@link #ACTIVE} - Project is currently being worked on</li>
 *   <li>{@link #ON_HOLD} - Project is paused but not cancelled</li>
 *   <li>{@link #COMPLETED} - Project is finished</li>
 *   <li>{@link #ARCHIVED} - Project is archived for historical reference</li>
 * </ul>
 */
public enum ProjectStatus {
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    ARCHIVED
}
