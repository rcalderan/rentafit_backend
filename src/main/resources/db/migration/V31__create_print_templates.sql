CREATE TABLE print_templates (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    template_type VARCHAR(40) NOT NULL,
    page_format VARCHAR(20) NOT NULL,
    orientation VARCHAR(10) NOT NULL,
    page_width_mm NUMERIC(8, 2) NOT NULL,
    page_height_mm NUMERIC(8, 2),
    margin_top_mm NUMERIC(8, 2) NOT NULL DEFAULT 0,
    margin_bottom_mm NUMERIC(8, 2) NOT NULL DEFAULT 0,
    margin_left_mm NUMERIC(8, 2) NOT NULL DEFAULT 0,
    margin_right_mm NUMERIC(8, 2) NOT NULL DEFAULT 0,
    print_offset_mm NUMERIC(8, 2) NOT NULL DEFAULT 0,
    content_json JSONB,
    content_html TEXT NOT NULL DEFAULT '',
    css_styles TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_print_templates_type CHECK (template_type IN ('RENTAL_CONTRACT', 'RENTAL_CANCELLATION', 'FISCAL_RECEIPT_80MM', 'FISCAL_RECEIPT_58MM', 'CUSTOM')),
    CONSTRAINT ck_print_templates_format CHECK (page_format IN ('A4', 'THERMAL_80MM', 'THERMAL_58MM', 'CUSTOM')),
    CONSTRAINT ck_print_templates_orientation CHECK (orientation IN ('PORTRAIT', 'LANDSCAPE')),
    CONSTRAINT ck_print_templates_dimensions CHECK (page_width_mm > 0 AND (page_height_mm IS NULL OR page_height_mm > 0)),
    CONSTRAINT ck_print_templates_margins CHECK (margin_top_mm >= 0 AND margin_bottom_mm >= 0 AND margin_left_mm >= 0 AND margin_right_mm >= 0 AND print_offset_mm >= 0)
);

CREATE INDEX idx_print_templates_active_type ON print_templates (is_active, template_type, name);
CREATE UNIQUE INDEX uk_print_templates_default_type ON print_templates (template_type) WHERE is_default AND is_active;
