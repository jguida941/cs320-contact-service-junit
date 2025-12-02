-- V12: Add assignee support to tasks
-- Enables tasks to be assigned to users for collaboration

ALTER TABLE tasks ADD COLUMN assignee_id BIGINT;

-- Index for efficient filtering by assignee
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);

-- Note: No FK constraint initially - allows flexible assignment
-- Tasks can exist without an assignee (assignee_id is nullable)
-- Service layer validates assignee existence when assigning
