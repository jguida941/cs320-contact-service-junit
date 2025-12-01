-- V6: Introduce surrogate primary keys and enforce per-user uniqueness (H2)

-- Contacts -------------------------------------------------------------------
ALTER TABLE contacts DROP PRIMARY KEY;
ALTER TABLE contacts ADD COLUMN id BIGINT;
CREATE SEQUENCE contacts_id_seq START WITH 1 INCREMENT BY 1;
UPDATE contacts SET id = NEXT VALUE FOR contacts_id_seq;
ALTER TABLE contacts ALTER COLUMN id SET NOT NULL;
ALTER TABLE contacts ALTER COLUMN id SET DEFAULT NEXT VALUE FOR contacts_id_seq;
ALTER TABLE contacts ADD PRIMARY KEY (id);
ALTER TABLE contacts ADD CONSTRAINT uq_contacts_contact_id_user_id UNIQUE (contact_id, user_id);

-- Tasks ----------------------------------------------------------------------
ALTER TABLE tasks DROP PRIMARY KEY;
ALTER TABLE tasks ADD COLUMN id BIGINT;
CREATE SEQUENCE tasks_id_seq START WITH 1 INCREMENT BY 1;
UPDATE tasks SET id = NEXT VALUE FOR tasks_id_seq;
ALTER TABLE tasks ALTER COLUMN id SET NOT NULL;
ALTER TABLE tasks ALTER COLUMN id SET DEFAULT NEXT VALUE FOR tasks_id_seq;
ALTER TABLE tasks ADD PRIMARY KEY (id);
ALTER TABLE tasks ADD CONSTRAINT uq_tasks_task_id_user_id UNIQUE (task_id, user_id);

-- Appointments ---------------------------------------------------------------
ALTER TABLE appointments DROP PRIMARY KEY;
ALTER TABLE appointments ADD COLUMN id BIGINT;
CREATE SEQUENCE appointments_id_seq START WITH 1 INCREMENT BY 1;
UPDATE appointments SET id = NEXT VALUE FOR appointments_id_seq;
ALTER TABLE appointments ALTER COLUMN id SET NOT NULL;
ALTER TABLE appointments ALTER COLUMN id SET DEFAULT NEXT VALUE FOR appointments_id_seq;
ALTER TABLE appointments ADD PRIMARY KEY (id);
ALTER TABLE appointments ADD CONSTRAINT uq_appointments_appointment_id_user_id UNIQUE (appointment_id, user_id);
