ALTER TABLE categories
    ADD COLUMN legacy_id INTEGER UNIQUE;

CREATE INDEX idx_categories_legacy_id ON categories(legacy_id);
