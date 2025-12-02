-- V10: Link tasks to projects
-- Enables tasks to be associated with projects (nullable FK)

ALTER TABLE tasks ADD COLUMN project_id VARCHAR(10);

-- Index for efficient filtering by project
CREATE INDEX idx_tasks_project_id ON tasks(project_id);

-- Note: No FK constraint initially - allows independent task/project creation
-- Tasks can exist without a project (project_id is nullable)
-- Service layer validates project existence when linking
