-- V5: Add user_id columns for per-user data isolation
-- PostgreSQL-specific migration to leverage ON CONFLICT and setval()

-- Create or update the system user that will own legacy data (id=1)
INSERT INTO users (id, username, email, password, role, enabled)
VALUES (1, 'system', 'system@localhost', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.ZH6vKKxYFBH8WW/l6W', 'USER', FALSE)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    password = EXCLUDED.password,
    role = EXCLUDED.role,
    enabled = EXCLUDED.enabled;

-- Ensure the backing sequence advances past the reserved system user
SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 1) FROM users));

-- Add user_id column to contacts table
ALTER TABLE contacts ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE contacts ADD CONSTRAINT fk_contacts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_contacts_user_id ON contacts(user_id);

-- Add user_id column to tasks table
ALTER TABLE tasks ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_tasks_user_id ON tasks(user_id);

-- Add user_id column to appointments table
ALTER TABLE appointments ADD COLUMN user_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX idx_appointments_user_id ON appointments(user_id);

-- Legacy records now belong to the system account (id=1, disabled)
