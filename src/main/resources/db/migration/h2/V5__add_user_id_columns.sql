-- V5: Add user_id columns for per-user data isolation
-- H2-specific migration since ON CONFLICT/setval are unavailable

-- Insert the system user if it does not already exist. The password hash is provided via the
-- Flyway placeholder ${system_user_password_hash} so credentials never live in source control.
INSERT INTO users (id, username, email, password, role, enabled)
SELECT 1, 'system', 'system@localhost', '${system_user_password_hash}', 'USER', FALSE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

-- Ensure the reserved system user row always has the expected values
UPDATE users
SET username = 'system',
    email = 'system@localhost',
    password = '${system_user_password_hash}',
    role = 'USER',
    enabled = FALSE
WHERE id = 1;

-- Reset the identity counter so new users start at id=2
ALTER TABLE users ALTER COLUMN id RESTART WITH 2;

-- Add user_id column to contacts table
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT 1;
ALTER TABLE contacts ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE contacts ADD CONSTRAINT IF NOT EXISTS fk_contacts_user_id
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_contacts_user_id ON contacts(user_id);

-- Add user_id column to tasks table
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT 1;
ALTER TABLE tasks ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE tasks ADD CONSTRAINT IF NOT EXISTS fk_tasks_user_id
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_tasks_user_id ON tasks(user_id);

-- Add user_id column to appointments table
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT 1;
ALTER TABLE appointments ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE appointments ADD CONSTRAINT IF NOT EXISTS fk_appointments_user_id
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
CREATE INDEX IF NOT EXISTS idx_appointments_user_id ON appointments(user_id);

-- Legacy records now belong to the system account (id=1, disabled)
