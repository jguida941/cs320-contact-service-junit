-- V4: Create users table for authentication
-- Part of Phase 5: Security + Observability
-- Field sizes from Validation.java: MAX_USERNAME_LENGTH=50, MAX_EMAIL_LENGTH=100, MAX_PASSWORD_LENGTH=255

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,      -- Validation.MAX_USERNAME_LENGTH
    email VARCHAR(100) NOT NULL UNIQUE,        -- Validation.MAX_EMAIL_LENGTH
    password VARCHAR(255) NOT NULL,            -- Validation.MAX_PASSWORD_LENGTH (bcrypt + headroom)
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Note: updated_at column is maintained via JPA lifecycle callbacks in User.java

-- Index for username lookups (used during authentication)
CREATE INDEX idx_users_username ON users(username);

-- Index for email lookups (used during registration validation)
CREATE INDEX idx_users_email ON users(email);
