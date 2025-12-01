-- Task schema derived from contactapp.domain.Validation constraints.
CREATE TABLE IF NOT EXISTS tasks (
    task_id     VARCHAR(10) PRIMARY KEY,
    name        VARCHAR(20) NOT NULL,
    description VARCHAR(50) NOT NULL
);
