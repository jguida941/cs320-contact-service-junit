-- Appointment schema derived from contactapp.domain.Validation constraints.
CREATE TABLE IF NOT EXISTS appointments (
    appointment_id   VARCHAR(10) PRIMARY KEY,
    appointment_date TIMESTAMP WITH TIME ZONE NOT NULL,
    description      VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_appointments_date ON appointments(appointment_date);
