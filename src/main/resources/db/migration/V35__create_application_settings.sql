CREATE TABLE application_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(100) NOT NULL,
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO application_settings (setting_key, setting_value)
VALUES ('rental.conflictWindowDays', '2');
