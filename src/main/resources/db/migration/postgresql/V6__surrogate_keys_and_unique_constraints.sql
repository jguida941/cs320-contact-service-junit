-- V6: Introduce surrogate primary keys and enforce per-user uniqueness

-- Fail fast if any foreign keys still reference the natural IDs. Admins must drop/update those
-- constraints before running this migration so we don't silently corrupt relationships.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint c
        WHERE c.contype = 'f'
          AND c.confrelid = 'contacts'::regclass
          AND EXISTS (
              SELECT 1
              FROM unnest(c.confkey) colnum
              JOIN pg_attribute a ON a.attrelid = c.confrelid AND a.attnum = colnum
              WHERE a.attname = 'contact_id'
          )
    ) THEN
        RAISE EXCEPTION 'Foreign keys still reference contacts.contact_id. Drop/update dependents before V6.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_constraint c
        WHERE c.contype = 'f'
          AND c.confrelid = 'tasks'::regclass
          AND EXISTS (
              SELECT 1
              FROM unnest(c.confkey) colnum
              JOIN pg_attribute a ON a.attrelid = c.confrelid AND a.attnum = colnum
              WHERE a.attname = 'task_id'
          )
    ) THEN
        RAISE EXCEPTION 'Foreign keys still reference tasks.task_id. Drop/update dependents before V6.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_constraint c
        WHERE c.contype = 'f'
          AND c.confrelid = 'appointments'::regclass
          AND EXISTS (
              SELECT 1
              FROM unnest(c.confkey) colnum
              JOIN pg_attribute a ON a.attrelid = c.confrelid AND a.attnum = colnum
              WHERE a.attname = 'appointment_id'
          )
    ) THEN
        RAISE EXCEPTION 'Foreign keys still reference appointments.appointment_id. Drop/update dependents before V6.';
    END IF;
END $$;

-- Contacts -------------------------------------------------------------------
ALTER TABLE contacts DROP CONSTRAINT IF EXISTS contacts_pkey;
ALTER TABLE contacts ADD COLUMN IF NOT EXISTS id BIGINT;
CREATE SEQUENCE IF NOT EXISTS contacts_id_seq;
ALTER SEQUENCE contacts_id_seq OWNED BY contacts.id;
UPDATE contacts SET id = nextval('contacts_id_seq') WHERE id IS NULL;
DO $$
DECLARE
    max_contact_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO max_contact_id FROM contacts;
    IF max_contact_id = 0 THEN
        PERFORM setval('contacts_id_seq', 1, false);
    ELSE
        PERFORM setval('contacts_id_seq', max_contact_id, true);
    END IF;
END $$;
ALTER TABLE contacts ALTER COLUMN id SET NOT NULL;
ALTER TABLE contacts ALTER COLUMN id SET DEFAULT nextval('contacts_id_seq');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'contacts_pkey') THEN
        ALTER TABLE contacts ADD CONSTRAINT contacts_pkey PRIMARY KEY (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_contacts_contact_id_user_id') THEN
        ALTER TABLE contacts ADD CONSTRAINT uq_contacts_contact_id_user_id UNIQUE (contact_id, user_id);
    END IF;
END $$;

-- Tasks ----------------------------------------------------------------------
ALTER TABLE tasks DROP CONSTRAINT IF EXISTS tasks_pkey;
ALTER TABLE tasks ADD COLUMN IF NOT EXISTS id BIGINT;
CREATE SEQUENCE IF NOT EXISTS tasks_id_seq;
ALTER SEQUENCE tasks_id_seq OWNED BY tasks.id;
UPDATE tasks SET id = nextval('tasks_id_seq') WHERE id IS NULL;
DO $$
DECLARE
    max_task_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO max_task_id FROM tasks;
    IF max_task_id = 0 THEN
        PERFORM setval('tasks_id_seq', 1, false);
    ELSE
        PERFORM setval('tasks_id_seq', max_task_id, true);
    END IF;
END $$;
ALTER TABLE tasks ALTER COLUMN id SET NOT NULL;
ALTER TABLE tasks ALTER COLUMN id SET DEFAULT nextval('tasks_id_seq');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'tasks_pkey') THEN
        ALTER TABLE tasks ADD CONSTRAINT tasks_pkey PRIMARY KEY (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_tasks_task_id_user_id') THEN
        ALTER TABLE tasks ADD CONSTRAINT uq_tasks_task_id_user_id UNIQUE (task_id, user_id);
    END IF;
END $$;

-- Appointments ---------------------------------------------------------------
ALTER TABLE appointments DROP CONSTRAINT IF EXISTS appointments_pkey;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS id BIGINT;
CREATE SEQUENCE IF NOT EXISTS appointments_id_seq;
ALTER SEQUENCE appointments_id_seq OWNED BY appointments.id;
UPDATE appointments SET id = nextval('appointments_id_seq') WHERE id IS NULL;
DO $$
DECLARE
    max_appt_id BIGINT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO max_appt_id FROM appointments;
    IF max_appt_id = 0 THEN
        PERFORM setval('appointments_id_seq', 1, false);
    ELSE
        PERFORM setval('appointments_id_seq', max_appt_id, true);
    END IF;
END $$;
ALTER TABLE appointments ALTER COLUMN id SET NOT NULL;
ALTER TABLE appointments ALTER COLUMN id SET DEFAULT nextval('appointments_id_seq');
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'appointments_pkey') THEN
        ALTER TABLE appointments ADD CONSTRAINT appointments_pkey PRIMARY KEY (id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_appointments_appointment_id_user_id') THEN
        ALTER TABLE appointments ADD CONSTRAINT uq_appointments_appointment_id_user_id UNIQUE (appointment_id, user_id);
    END IF;
END $$;
