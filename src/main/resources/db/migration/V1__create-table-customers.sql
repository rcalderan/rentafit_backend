-- Initial Schema Migration
-- Includes: addresses, people, user_accounts, employees, customers, customer_phones

-- 1. Table: addresses
CREATE TABLE IF NOT EXISTS addresses (
    id UUID PRIMARY KEY,
    zip_code VARCHAR(20),
    street VARCHAR(255),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state CHAR(2)
);

-- 2. Table: people (Abstract Base)
CREATE TABLE IF NOT EXISTS people (
    id UUID PRIMARY KEY,
    legacy_id INT UNIQUE,
    name VARCHAR(255) NOT NULL,
    document VARCHAR(50) UNIQUE,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 3. Table: user_accounts
CREATE TABLE IF NOT EXISTS user_accounts (
    id UUID PRIMARY KEY REFERENCES people(id) ON DELETE CASCADE,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    pin VARCHAR(4),
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- 4. Table: employees
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY REFERENCES people(id) ON DELETE CASCADE,
    initials VARCHAR(10) UNIQUE,
    role_level INT DEFAULT 1
);

-- 5. Table: customers
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY REFERENCES people(id) ON DELETE CASCADE,
    is_authenticated BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_by_id UUID REFERENCES employees(id),
    address_id UUID REFERENCES addresses(id),
    number VARCHAR(20),
    complement VARCHAR(100)
);

-- 6. Table: customer_phones
CREATE TABLE IF NOT EXISTS customer_phones (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    phone VARCHAR(20) NOT NULL
);

-- Indexes for performance
CREATE INDEX idx_people_email ON people(email);
CREATE INDEX idx_people_document ON people(document);
CREATE INDEX idx_user_accounts_username ON user_accounts(username);
CREATE INDEX idx_customer_phones_customer_id ON customer_phones(customer_id);

