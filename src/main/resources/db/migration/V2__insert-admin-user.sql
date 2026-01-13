-- ============================================================
-- V2: Populate Initial Data
-- ============================================================
-- This migration inserts initial reference data and the admin user

-- 1. Insert Roles
INSERT INTO roles (role) VALUES ('ADMIN') ON CONFLICT (role) DO NOTHING;
INSERT INTO roles (role) VALUES ('MANAGER') ON CONFLICT (role) DO NOTHING;
INSERT INTO roles (role) VALUES ('EMPLOYEE') ON CONFLICT (role) DO NOTHING;
INSERT INTO roles (role) VALUES ('CUSTOMER') ON CONFLICT (role) DO NOTHING;

-- 2. Insert Admin Person
INSERT INTO people (id, name, document, email, created_at, updated_at)
VALUES (
    '0194269a-0000-7000-8000-000000000001',
    'System Administrator',
    '00000000000',
    'admin@rentafit.com.br',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

-- 3. Insert Admin Employee
INSERT INTO employees (id, initials, role_level)
VALUES (
    '0194269a-0000-7000-8000-000000000001',
    'ADM',
    99
) ON CONFLICT (id) DO NOTHING;

-- 4. Insert Admin User Account
-- Password: 'admin123' (BCrypt hash with strength 10)
INSERT INTO user_accounts (id, username, password, is_active)
VALUES (
    '0194269a-0000-7000-8000-000000000001',
    'admin',
    '$2a$10$l8fut/N97MKsXSnhMQAMuutgfrqpY6daZDCPZOX4hD7HnOUqS6aQi',
    true
) ON CONFLICT (id) DO NOTHING;

-- 5. Assign ADMIN role to admin user
INSERT INTO user_roles (user_id, role_id)
SELECT '0194269a-0000-7000-8000-000000000001', id
FROM roles
WHERE role = 'ADMIN'
ON CONFLICT DO NOTHING;
