-- V9: Enhance tasks table with status tracking and due date functionality
-- Adds workflow status, due date tracking, and audit timestamps

-- Add status column with default TODO
ALTER TABLE tasks ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'TODO';

-- Add due_date column (nullable as not all tasks have deadlines)
ALTER TABLE tasks ADD COLUMN due_date DATE;

-- Add audit timestamp columns
ALTER TABLE tasks ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE tasks ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Add constraint to enforce valid status values
ALTER TABLE tasks ADD CONSTRAINT chk_tasks_status
    CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE'));

-- Create indexes for efficient querying by status and due date
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_tasks_due_date ON tasks(due_date);

-- Note: updated_at column is maintained via JPA lifecycle callbacks in Task.java
