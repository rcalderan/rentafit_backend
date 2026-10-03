ALTER TABLE print_templates
    ADD COLUMN version INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN previous_version_id VARCHAR(100);

CREATE INDEX idx_print_templates_previous_version ON print_templates (previous_version_id);
