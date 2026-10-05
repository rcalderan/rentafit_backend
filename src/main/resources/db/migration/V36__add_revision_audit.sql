ALTER TABLE rental_contracts
    ADD COLUMN revised_by_account_id UUID,
    ADD COLUMN confirmed_by_account_id UUID,
    ADD COLUMN revision_confirmed_at TIMESTAMPTZ,
    ADD COLUMN parent_snapshot TEXT;
