package contactapp.domain;

/**
 * Enumeration of task statuses.
 *
 * <p>Defines the lifecycle states a task can be in:
 * <ul>
 *   <li>{@link #TODO} - Task is pending and not yet started</li>
 *   <li>{@link #IN_PROGRESS} - Task is actively being worked on</li>
 *   <li>{@link #DONE} - Task is completed</li>
 * </ul>
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    DONE
}
