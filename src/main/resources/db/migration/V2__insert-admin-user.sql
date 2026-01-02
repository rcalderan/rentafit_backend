-- Inserir a Pessoa (Base para o Usuário)
INSERT INTO people (id, name, document, email, created_at, updated_at)
VALUES (
    '0194269a-0000-7000-8000-000000000001', -- UUID v7 manual/fixo para o admin
    'System Administrator',
    '00000000000',
    'admin@rentafit.com.br',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Inserir o Funcionário (O admin é um funcionário com nível máximo)
INSERT INTO employees (id, initials, role_level)
VALUES (
    '0194269a-0000-7000-8000-000000000001',
    'ADM',
    99
);

-- Inserir a Conta de Usuário (Auth)
-- Senha: 'admin123' (BCrypt hash gerado com strength 10)
INSERT INTO user_accounts (id, username, password, role, is_active)
VALUES (
    '0194269a-0000-7000-8000-000000000001',
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye1J8Cz7B.8WkQdGlKb7VYLj/XoqQD5bi', -- BCrypt para 'admin123'
    'ROLE_ADMIN',
    true
);
