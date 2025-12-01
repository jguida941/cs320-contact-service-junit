-- Contact schema derived from contactapp.domain.Validation constraints.
CREATE TABLE IF NOT EXISTS contacts (
    contact_id VARCHAR(10) PRIMARY KEY,
    first_name VARCHAR(10) NOT NULL,
    last_name  VARCHAR(10) NOT NULL,
    phone      VARCHAR(10) NOT NULL,
    address    VARCHAR(30) NOT NULL,
    CONSTRAINT chk_contacts_phone_digits CHECK (
        length(phone) = 10
        AND phone BETWEEN '0000000000' AND '9999999999'
    )
);

CREATE INDEX IF NOT EXISTS idx_contacts_last_name ON contacts(last_name);
