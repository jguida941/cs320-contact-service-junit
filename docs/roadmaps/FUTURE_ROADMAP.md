# Jira-Like Evolution Roadmap

> Evolving ContactApp into a full-featured project management platform

**Status:** Planning | **Created:** 2025-12-03 | **Owner:** Justin Guida

---

## Current State Summary

### What We Have (Phases 0-7 Complete)

| Domain          | Features                                                                   |
|-----------------|----------------------------------------------------------------------------|
| **User**        | JWT auth, roles (USER/ADMIN), BCrypt passwords, session management         |
| **Project**     | CRUD, status (ACTIVE/ON_HOLD/COMPLETED/ARCHIVED), contact linking          |
| **Task**        | CRUD, status (TODO/IN_PROGRESS/DONE), due dates, project linking, assignee |
| **Appointment** | CRUD, task/project linking, calendar context                               |
| **Contact**     | CRUD, project stakeholder linking                                          |

### Technical Foundation

| Category     | Implementation                                              |
|--------------|-------------------------------------------------------------|
| **Backend**  | Spring Boot 3.4, Spring Security 7, JPA/Hibernate           |
| **Frontend** | React 19, Vite, Tailwind v4, shadcn/ui                      |
| **Database** | PostgreSQL + Flyway (13 migrations)                         |
| **Testing**  | 1,026 tests, 90%+ mutation coverage, E2E with Playwright    |
| **CI/CD**    | GitHub Actions, CodeQL, ZAP, API fuzzing, Docker            |
| **Security** | JWT HttpOnly cookies, CSRF, rate limiting, tenant isolation |
| **Docs**     | 49 ADRs, threat model, design notes                         |

---

## Evolution Roadmap

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           JIRA-LIKE EVOLUTION                                │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  CURRENT STATE ────────────────────────────────────────────────────────────► │
│  ✅ Projects, Tasks, Appointments, Contacts, Users                           │
│  ✅ Basic task status workflow (TODO → IN_PROGRESS → DONE)                   │
│  ✅ Assignment, due dates, project linking                                   │
│                                                                              │
│  PHASE 8: Task Enhancements ────────────────────────────────────────────────►│
│  • Task types (Bug, Story, Epic, Subtask)                                    │
│  • Priority levels (Critical, High, Medium, Low)                             │
│  • Story points / effort estimation                                          │
│  • Labels/tags for categorization                                            │
│                                                                              │
│  PHASE 9: Sprint Management ───────────────────────────────────────────────► │
│  • Sprint entity with start/end dates                                        │
│  • Backlog vs sprint views                                                   │
│  • Sprint planning and capacity                                              │
│  • Velocity tracking                                                         │
│                                                                              │
│  PHASE 10: Activity & Comments ────────────────────────────────────────────► │
│  • Task comments with rich text                                              │
│  • Activity/audit log per task                                               │
│  • @mentions in comments                                                     │
│  • File attachments                                                          │
│                                                                              │
│  PHASE 11: Kanban Board UI ────────────────────────────────────────────────► │
│  • Drag-and-drop columns                                                     │
│  • Swimlanes (by assignee, priority)                                         │
│  • WIP limits per column                                                     │
│  • Quick filters and search                                                  │
│                                                                              │
│  PHASE 12: Reporting & Analytics ──────────────────────────────────────────► │
│  • Burndown/burnup charts                                                    │
│  • Velocity charts                                                           │
│  • Cumulative flow diagrams                                                  │
│  • Custom dashboards                                                         │
│                                                                              │
│  PHASE 13: Notifications & Real-time ──────────────────────────────────────► │
│  • WebSocket for live updates                                                │
│  • Email notifications                                                       │
│  • In-app notification center                                                │
│  • Watchers/subscribers on tasks                                             │
│                                                                              │
│  PHASE 14: Epic & Roadmap ─────────────────────────────────────────────────► │
│  • Epic entity (story grouping)                                              │
│  • Task dependencies (blocked by)                                            │
│  • Timeline/Gantt view                                                       │
│  • Release planning                                                          │
│                                                                              │
│  PHASE 15: Advanced Workflows ─────────────────────────────────────────────► │
│  • Custom status definitions per project                                     │
│  • Workflow transitions with rules                                           │
│  • Automation (auto-assign, triggers)                                        │
│  • SLA tracking                                                              │
│                                                                              │
│  PHASE 16: Integrations ───────────────────────────────────────────────────► │
│  • GitHub/GitLab commit linking                                              │
│  • Slack notifications                                                       │
│  • Webhook system                                                            │
│  • Public API with OAuth                                                     │
│                                                                              │
│  PHASE 17: Teams & Permissions ────────────────────────────────────────────► │
│  • Team entity with members                                                  │
│  • Project-level permissions                                                 │
│  • Team workload views                                                       │
│  • Organization hierarchy                                                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Phase 8: Task Enhancements

**Goal:** Transform simple tasks into rich work items like Jira issues

### New Domain Models

```java
// TaskType.java
public enum TaskType {
    TASK,       // Generic work item (default)
    BUG,        // Defect/issue to fix
    STORY,      // User story (feature)
    EPIC,       // Large body of work (contains stories)
    SUBTASK     // Child of another task
}

// Priority.java
public enum Priority {
    CRITICAL,   // P0 - Drop everything
    HIGH,       // P1 - Do next
    MEDIUM,     // P2 - Normal priority (default)
    LOW         // P3 - Nice to have
}

// Label.java (new entity)
public class Label {
    private String labelId;      // Unique per project
    private String name;         // Display name (e.g., "frontend")
    private String color;        // Hex color code
    private String projectId;    // Scoped to project
}
```

### Task Domain Updates

```java
// Task.java additions
private TaskType type;           // BUG, STORY, EPIC, SUBTASK
private Priority priority;       // CRITICAL, HIGH, MEDIUM, LOW
private Integer storyPoints;     // Effort estimation (1, 2, 3, 5, 8, 13, 21)
private String parentTaskId;     // For subtasks - FK to parent task
private Set<String> labelIds;    // Many-to-many with labels
```

### Database Migrations

```sql
-- V14__add_task_type_and_priority.sql
ALTER TABLE tasks ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'TASK';
ALTER TABLE tasks ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE tasks ADD COLUMN story_points INTEGER;
ALTER TABLE tasks ADD COLUMN parent_task_id BIGINT;

ALTER TABLE tasks ADD CONSTRAINT chk_tasks_type
    CHECK (type IN ('TASK', 'BUG', 'STORY', 'EPIC', 'SUBTASK'));
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_priority
    CHECK (priority IN ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW'));
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_parent
    FOREIGN KEY (parent_task_id) REFERENCES tasks(id) ON DELETE CASCADE;

CREATE INDEX idx_tasks_type ON tasks(type);
CREATE INDEX idx_tasks_priority ON tasks(priority);
CREATE INDEX idx_tasks_parent ON tasks(parent_task_id);

-- V15__create_labels_table.sql
CREATE TABLE labels (
    id BIGSERIAL PRIMARY KEY,
    label_id VARCHAR(20) NOT NULL,
    name VARCHAR(50) NOT NULL,
    color VARCHAR(7) NOT NULL DEFAULT '#6B7280',
    project_id BIGINT NOT NULL,
    CONSTRAINT fk_labels_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_labels_project_label_id UNIQUE (project_id, label_id)
);

CREATE TABLE task_labels (
    task_id BIGINT NOT NULL,
    label_id BIGINT NOT NULL,
    PRIMARY KEY (task_id, label_id),
    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (label_id) REFERENCES labels(id) ON DELETE CASCADE
);
```

### API Endpoints

```
POST   /api/v1/projects/{projectId}/labels     - Create label
GET    /api/v1/projects/{projectId}/labels     - List project labels
DELETE /api/v1/projects/{projectId}/labels/{id} - Delete label

GET    /api/v1/tasks?type=BUG                  - Filter by type
GET    /api/v1/tasks?priority=HIGH             - Filter by priority
GET    /api/v1/tasks/{id}/subtasks             - Get subtasks
POST   /api/v1/tasks/{id}/labels               - Add label to task
DELETE /api/v1/tasks/{id}/labels/{labelId}     - Remove label
```

### Test Coverage

- `TaskTypeTest.java` - Enum coverage
- `PriorityTest.java` - Enum coverage
- `LabelTest.java` - Domain validation
- `LabelServiceTest.java` - CRUD operations
- `TaskLabelingTest.java` - Many-to-many operations
- `SubtaskTest.java` - Parent-child relationships

---

## Phase 9: Sprint Management

**Goal:** Enable Scrum-style iteration planning

### New Domain Models

```java
// Sprint.java
public class Sprint {
    private String sprintId;        // Unique identifier
    private String name;            // "Sprint 1", "Q1 Week 3"
    private String goal;            // Sprint objective
    private String projectId;       // FK to project
    private LocalDate startDate;    // Sprint start
    private LocalDate endDate;      // Sprint end
    private SprintStatus status;    // PLANNING, ACTIVE, COMPLETED
    private Integer capacity;       // Team capacity in story points
}

// SprintStatus.java
public enum SprintStatus {
    PLANNING,   // Not yet started
    ACTIVE,     // Currently in progress (only 1 per project)
    COMPLETED   // Sprint finished
}
```

### Task Domain Updates

```java
// Task.java additions
private String sprintId;         // FK to current sprint (nullable = backlog)
```

### Database Migrations

```sql
-- V16__create_sprints_table.sql
CREATE TABLE sprints (
    id BIGSERIAL PRIMARY KEY,
    sprint_id VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    goal VARCHAR(500),
    project_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
    capacity INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0 NOT NULL,
    CONSTRAINT fk_sprints_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT uq_sprints_project_sprint_id UNIQUE (project_id, sprint_id),
    CONSTRAINT chk_sprints_status CHECK (status IN ('PLANNING', 'ACTIVE', 'COMPLETED')),
    CONSTRAINT chk_sprints_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_sprints_project_id ON sprints(project_id);
CREATE INDEX idx_sprints_status ON sprints(status);

-- V17__add_sprint_to_tasks.sql
ALTER TABLE tasks ADD COLUMN sprint_id BIGINT;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_sprint
    FOREIGN KEY (sprint_id) REFERENCES sprints(id) ON DELETE SET NULL;
CREATE INDEX idx_tasks_sprint_id ON tasks(sprint_id);
```

### API Endpoints

```
POST   /api/v1/projects/{projectId}/sprints           - Create sprint
GET    /api/v1/projects/{projectId}/sprints           - List sprints
GET    /api/v1/projects/{projectId}/sprints/active    - Get active sprint
GET    /api/v1/projects/{projectId}/sprints/{id}      - Get sprint details
PUT    /api/v1/projects/{projectId}/sprints/{id}      - Update sprint
POST   /api/v1/projects/{projectId}/sprints/{id}/start   - Start sprint
POST   /api/v1/projects/{projectId}/sprints/{id}/complete - Complete sprint

GET    /api/v1/projects/{projectId}/backlog           - Get backlog (unassigned)
POST   /api/v1/tasks/{taskId}/sprint                  - Move task to sprint
DELETE /api/v1/tasks/{taskId}/sprint                  - Move task to backlog

GET    /api/v1/projects/{projectId}/velocity          - Sprint velocity history
```

### Sprint Metrics

```java
// SprintMetrics.java
public record SprintMetrics(
    int totalPoints,          // Sum of story points in sprint
    int completedPoints,      // Points for DONE tasks
    int remainingPoints,      // Points for non-DONE tasks
    int taskCount,            // Total tasks
    int completedCount,       // DONE tasks
    double completionRate,    // completedCount / taskCount
    List<DailyProgress> burndown  // For burndown chart
) {}

public record DailyProgress(
    LocalDate date,
    int remainingPoints,
    int idealRemaining
) {}
```

---

## Phase 10: Activity & Comments

**Goal:** Enable collaboration through discussions and audit trails

### New Domain Models

```java
// Comment.java
public class Comment {
    private String commentId;
    private String taskId;           // FK to task
    private Long authorId;           // FK to user
    private String content;          // Markdown-supported text
    private Instant createdAt;
    private Instant updatedAt;
    private boolean edited;          // Was this comment modified?
}

// ActivityEntry.java (audit log)
public class ActivityEntry {
    private Long id;
    private String taskId;
    private Long userId;
    private ActivityType type;       // STATUS_CHANGED, ASSIGNED, etc.
    private String oldValue;
    private String newValue;
    private Instant timestamp;
}

// ActivityType.java
public enum ActivityType {
    CREATED,
    STATUS_CHANGED,
    ASSIGNED,
    UNASSIGNED,
    PRIORITY_CHANGED,
    DUE_DATE_CHANGED,
    SPRINT_CHANGED,
    COMMENT_ADDED,
    LABEL_ADDED,
    LABEL_REMOVED,
    ATTACHMENT_ADDED
}
```

### Database Migrations

```sql
-- V18__create_comments_table.sql
CREATE TABLE comments (
    id BIGSERIAL PRIMARY KEY,
    comment_id VARCHAR(36) NOT NULL,
    task_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    edited BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_comments_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_comments_task_id ON comments(task_id);
CREATE INDEX idx_comments_author_id ON comments(author_id);
CREATE INDEX idx_comments_created_at ON comments(created_at);

-- V19__create_activity_log.sql
CREATE TABLE activity_log (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL,
    user_id BIGINT,
    activity_type VARCHAR(30) NOT NULL,
    old_value VARCHAR(500),
    new_value VARCHAR(500),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_task FOREIGN KEY (task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_activity_task_id ON activity_log(task_id);
CREATE INDEX idx_activity_timestamp ON activity_log(timestamp);
```

### API Endpoints

```
POST   /api/v1/tasks/{taskId}/comments              - Add comment
GET    /api/v1/tasks/{taskId}/comments              - List comments
PUT    /api/v1/tasks/{taskId}/comments/{id}         - Edit comment
DELETE /api/v1/tasks/{taskId}/comments/{id}         - Delete comment

GET    /api/v1/tasks/{taskId}/activity              - Get activity log
GET    /api/v1/projects/{projectId}/activity        - Project-wide activity
GET    /api/v1/activity/me                          - My recent activity
```

### Mention System

```java
// MentionService.java
public class MentionService {
    // Parse @username from comment content
    public Set<String> extractMentions(String content);

    // Create notifications for mentioned users
    public void notifyMentionedUsers(Comment comment, Set<String> usernames);
}
```

---

## Phase 11: Kanban Board UI

**Goal:** Visual drag-and-drop task management

### Frontend Components

```
ui/contact-app/src/
├── components/
│   └── board/
│       ├── KanbanBoard.tsx       # Main board container
│       ├── KanbanColumn.tsx      # Status column (TODO, IN_PROGRESS, DONE)
│       ├── KanbanCard.tsx        # Task card with preview
│       ├── KanbanSwimlane.tsx    # Horizontal grouping (by assignee)
│       └── QuickCreateCard.tsx   # Inline task creation
├── hooks/
│   ├── useDragAndDrop.ts         # DnD logic with optimistic updates
│   └── useBoardFilters.ts        # Filter state management
└── pages/
    └── BoardPage.tsx             # Board view route
```

### Key Features

1. **Drag-and-Drop**
   - React DnD or dnd-kit library
   - Optimistic UI updates
   - Auto-save on drop
   - Undo capability

2. **Columns**
   - One column per task status
   - WIP limits with visual warnings
   - Collapsed column support
   - Column task count

3. **Cards**
   - Task type icon (bug, story, etc.)
   - Priority indicator (color bar)
   - Assignee avatar
   - Due date warning (overdue = red)
   - Story points badge
   - Label chips

4. **Swimlanes**
   - Group by: None, Assignee, Priority, Epic
   - Collapsible rows
   - Row task counts

5. **Quick Filters**
   - My tasks only
   - By label
   - By assignee
   - By priority
   - Search by title

### API Updates

```
PATCH /api/v1/tasks/{id}/status    - Quick status update (for drag-drop)
PATCH /api/v1/tasks/reorder        - Batch reorder within column

Request: { taskIds: ["T1", "T2", "T3"], status: "IN_PROGRESS" }
```

---

## Phase 12: Reporting & Analytics

**Goal:** Data-driven insights for teams

### Charts & Visualizations

1. **Burndown Chart**
   - X: Days in sprint
   - Y: Remaining story points
   - Ideal line + actual line

2. **Burnup Chart**
   - X: Days in sprint
   - Y: Completed story points
   - Shows scope changes

3. **Velocity Chart**
   - X: Sprint number
   - Y: Completed story points
   - Running average line

4. **Cumulative Flow Diagram**
   - X: Time
   - Y: Task count by status
   - Stacked area chart

5. **Task Distribution**
   - Pie charts: By type, priority, assignee
   - Bar charts: Created vs completed over time

### Dashboard Components

```
ui/contact-app/src/
├── components/
│   └── charts/
│       ├── BurndownChart.tsx
│       ├── VelocityChart.tsx
│       ├── CumulativeFlowChart.tsx
│       └── DistributionChart.tsx
└── pages/
    └── ReportsPage.tsx
```

### API Endpoints

```
GET /api/v1/projects/{id}/reports/burndown?sprintId={sprintId}
GET /api/v1/projects/{id}/reports/velocity?sprints=5
GET /api/v1/projects/{id}/reports/cumulative-flow?days=30
GET /api/v1/projects/{id}/reports/distribution
GET /api/v1/projects/{id}/reports/export?format=csv
```

---

## Phase 13: Notifications & Real-time

**Goal:** Keep team members informed and synchronized

### Notification Types

| Event | Recipients |
|-------|------------|
| Task assigned to me | Assignee |
| Task I own was updated | Owner |
| Comment on my task | Owner + assignee |
| @mentioned in comment | Mentioned user |
| Task moved to my sprint | Assignee |
| Sprint started/completed | All team members |
| Task overdue | Assignee + owner |

### Implementation

```java
// Notification.java
public class Notification {
    private Long id;
    private Long userId;              // Recipient
    private NotificationType type;
    private String title;
    private String message;
    private String link;              // /tasks/T123
    private boolean read;
    private Instant createdAt;
}

// NotificationService.java
@Service
public class NotificationService {
    public void notify(Long userId, NotificationType type, String message);
    public void notifyAssignee(Task task, String message);
    public void notifyMentioned(Comment comment);
    public List<Notification> getUnread(Long userId);
    public void markAsRead(Long notificationId);
}
```

### WebSocket Real-time Updates

```java
// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}

// Message topics:
// /topic/project/{projectId}/tasks   - Task updates
// /topic/project/{projectId}/board   - Board changes
// /user/queue/notifications          - Personal notifications
```

### Email Notifications (Optional)

```yaml
# application.yml
notifications:
  email:
    enabled: true
    digest: daily  # or: immediate, hourly, weekly
    templates:
      task-assigned: templates/email/task-assigned.html
      comment-added: templates/email/comment-added.html
```

---

## Phase 14: Epic & Roadmap

**Goal:** High-level planning and dependencies

### New Domain Models

```java
// Epic is just a Task with type=EPIC
// Stories link to epic via parentTaskId

// Dependency.java
public class TaskDependency {
    private Long id;
    private String blockingTaskId;   // This task blocks...
    private String blockedTaskId;    // ...this task
    private DependencyType type;     // BLOCKS, RELATES_TO
}

public enum DependencyType {
    BLOCKS,      // Must complete before
    RELATES_TO   // Related but not blocking
}
```

### Database Migration

```sql
-- V20__create_task_dependencies.sql
CREATE TABLE task_dependencies (
    id BIGSERIAL PRIMARY KEY,
    blocking_task_id BIGINT NOT NULL,
    blocked_task_id BIGINT NOT NULL,
    dependency_type VARCHAR(20) NOT NULL DEFAULT 'BLOCKS',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dep_blocking FOREIGN KEY (blocking_task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_dep_blocked FOREIGN KEY (blocked_task_id)
        REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT uq_task_dependency UNIQUE (blocking_task_id, blocked_task_id),
    CONSTRAINT chk_no_self_dep CHECK (blocking_task_id != blocked_task_id)
);
```

### Roadmap View

```
ui/contact-app/src/
├── components/
│   └── roadmap/
│       ├── RoadmapTimeline.tsx     # Gantt-style view
│       ├── EpicLane.tsx            # Epic row with child stories
│       ├── DependencyArrow.tsx     # SVG arrows between items
│       └── MilestoneMarker.tsx     # Key dates
└── pages/
    └── RoadmapPage.tsx
```

### API Endpoints

```
GET    /api/v1/projects/{id}/epics                    - List epics with stories
GET    /api/v1/projects/{id}/roadmap?months=6         - Timeline data
POST   /api/v1/tasks/{id}/dependencies                - Add dependency
DELETE /api/v1/tasks/{id}/dependencies/{depId}        - Remove dependency
GET    /api/v1/tasks/{id}/blocked-by                  - What blocks this?
GET    /api/v1/tasks/{id}/blocks                      - What does this block?
```

---

## Phase 15-17: Advanced Features (Future)

### Phase 15: Custom Workflows

- Define custom statuses per project
- Workflow transitions with permissions
- Automation rules (e.g., auto-assign on label)
- SLA tracking for issue resolution

### Phase 16: Integrations

- GitHub/GitLab: Link commits to tasks
- Slack: Post task updates to channels
- Webhooks: Custom integrations
- OAuth2: Public API access

### Phase 17: Teams & Permissions

- Team entity with member management
- Project-level role assignments
- Team workload distribution views
- Organization hierarchy

---

## Implementation Priority Matrix

| Phase | Business Value | Technical Effort | Priority |
|-------|---------------|------------------|----------|
| 8: Task Enhancements | HIGH | MEDIUM | **P1** |
| 9: Sprint Management | HIGH | MEDIUM | **P1** |
| 10: Activity & Comments | HIGH | MEDIUM | **P1** |
| 11: Kanban Board | VERY HIGH | HIGH | **P1** |
| 12: Reporting | MEDIUM | MEDIUM | **P2** |
| 13: Notifications | MEDIUM | HIGH | **P2** |
| 14: Epic & Roadmap | MEDIUM | HIGH | **P2** |
| 15: Custom Workflows | MEDIUM | VERY HIGH | **P3** |
| 16: Integrations | HIGH | HIGH | **P3** |
| 17: Teams | MEDIUM | HIGH | **P3** |

---

## Quality Standards (Maintain Throughout)

### Each Phase Must Include

- [ ] ADR documenting design decisions
- [ ] Flyway migration scripts (no breaking changes)
- [ ] Domain validation in `Validation.java`
- [ ] Full test coverage (unit + integration)
- [ ] API documentation (OpenAPI)
- [ ] UI components with accessibility
- [ ] Security review (tenant isolation, auth checks)

### CI/CD Requirements

- [ ] All existing tests pass
- [ ] Mutation testing coverage maintained (>80%)
- [ ] No new CodeQL warnings
- [ ] ZAP scan passes
- [ ] API fuzzing validates new endpoints
- [ ] Docker build succeeds

### Documentation

- [ ] Update ROADMAP.md with completion status
- [ ] Update threat model if security-relevant
- [ ] Add design notes for complex features
- [ ] Update CHANGELOG.md

---

## Quick Reference: What Makes This Jira-Like?

| Jira Feature | Our Implementation | Phase |
|--------------|-------------------|-------|
| Issues | Task entity | ✅ Done |
| Projects | Project entity | ✅ Done |
| Assignees | Task.assigneeId | ✅ Done |
| Status workflow | TaskStatus enum | ✅ Done |
| Due dates | Task.dueDate | ✅ Done |
| Issue types | TaskType enum | Phase 8 |
| Priority | Priority enum | Phase 8 |
| Story points | Task.storyPoints | Phase 8 |
| Labels | Label entity | Phase 8 |
| Subtasks | Task.parentTaskId | Phase 8 |
| Sprints | Sprint entity | Phase 9 |
| Backlog | Tasks with no sprint | Phase 9 |
| Velocity | SprintMetrics | Phase 9 |
| Comments | Comment entity | Phase 10 |
| Activity log | ActivityEntry | Phase 10 |
| Kanban board | React DnD UI | Phase 11 |
| Burndown charts | Chart.js/Recharts | Phase 12 |
| Notifications | WebSocket + DB | Phase 13 |
| Epics | Task with type=EPIC | Phase 14 |
| Dependencies | TaskDependency | Phase 14 |
| Custom workflows | WorkflowDefinition | Phase 15 |
| Integrations | Webhook system | Phase 16 |
| Teams | Team entity | Phase 17 |

---

## Next Steps

1. **Review this roadmap** with stakeholders
2. **Prioritize Phase 8** (Task Enhancements) as first iteration
3. **Create ADR-0050** for Phase 8 design decisions
4. **Estimate effort** for each phase
5. **Begin implementation** following established patterns

---

*This roadmap builds on the solid foundation of 49 ADRs, 1,026 tests, and enterprise-grade CI/CD already in place.*
