-- V45__add_user_preferences_and_pin_trust_setting.sql
-- Persiste preferências de interface por usuário e janela global de confiança do PIN.

ALTER TABLE user_accounts
    ADD COLUMN ui_variant      VARCHAR(20) NOT NULL DEFAULT 'simplified',
    ADD COLUMN sidebar_visible BOOLEAN     NOT NULL DEFAULT TRUE,
    ADD CONSTRAINT chk_user_accounts_ui_variant
        CHECK (ui_variant IN ('simplified', 'legacy', 'atelier'));

INSERT INTO application_settings (setting_key, setting_value)
VALUES ('operator.pinTrustMinutes', '5')
ON CONFLICT (setting_key) DO NOTHING;
