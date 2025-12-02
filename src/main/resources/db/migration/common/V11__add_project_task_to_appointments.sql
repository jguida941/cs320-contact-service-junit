-- V11: Link appointments to projects and tasks
-- Enables appointments to be associated with projects and tasks (nullable FKs)

ALTER TABLE appointments ADD COLUMN project_id VARCHAR(10);
ALTER TABLE appointments ADD COLUMN task_id VARCHAR(10);

-- Indexes for efficient filtering by project and task
CREATE INDEX idx_appointments_project_id ON appointments(project_id);
CREATE INDEX idx_appointments_task_id ON appointments(task_id);

-- Note: No FK constraints initially - allows independent appointment/project/task creation
-- Appointments can exist without a project or task (both IDs are nullable)
-- Service layer validates project/task existence when linking
