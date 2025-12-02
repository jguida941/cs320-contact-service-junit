-- Junction table for Project-Contact relationship (stakeholders)
CREATE TABLE project_contacts (
    project_id BIGINT NOT NULL,
    contact_id BIGINT NOT NULL,
    role VARCHAR(50),  -- 'CLIENT', 'STAKEHOLDER', 'VENDOR', etc.
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, contact_id),
    CONSTRAINT fk_project_contacts_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE CASCADE,
    CONSTRAINT fk_project_contacts_contact FOREIGN KEY (contact_id)
        REFERENCES contacts(id) ON DELETE CASCADE
);

CREATE INDEX idx_project_contacts_project_id ON project_contacts(project_id);
CREATE INDEX idx_project_contacts_contact_id ON project_contacts(contact_id);
