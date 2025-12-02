-- V8: Create projects table
-- Field sizes from Validation.java: MAX_PROJECT_NAME_LENGTH=50, MAX_PROJECT_DESCRIPTION_LENGTH=100

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    project_id VARCHAR(10) NOT NULL,               -- Validation.MAX_ID_LENGTH
    name VARCHAR(50) NOT NULL,                     -- Validation.MAX_PROJECT_NAME_LENGTH
    description VARCHAR(100),                      -- Validation.MAX_PROJECT_DESCRIPTION_LENGTH
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- Status enum: ACTIVE, COMPLETED, ARCHIVED
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0 NOT NULL,             -- Optimistic locking (ADR-0044)
    CONSTRAINT fk_projects_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_projects_project_id_user_id UNIQUE (project_id, user_id),
    CONSTRAINT chk_projects_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'ARCHIVED', 'ON_HOLD'))
);

-- Note: updated_at column is maintained via JPA lifecycle callbacks in Project.java

-- Index for user_id lookups (user's projects)
CREATE INDEX idx_projects_user_id ON projects(user_id);

-- Index for status filtering (active projects query)
CREATE INDEX idx_projects_status ON projects(status);
