-- Initial Schema Migration
-- Refactored address architecture with zipCode as primary key and address history

-- 1. Table: addresses (immutable, indexed by zip_code)
CREATE TABLE IF NOT EXISTS addresses (
    zip_code VARCHAR(8) PRIMARY KEY,
    street VARCHAR(255) NOT NULL,
    neighborhood VARCHAR(100),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL
);

-- 2. Table: people (Abstract Base)
CREATE TABLE IF NOT EXISTS people (
    id UUID PRIMARY KEY,
    legacy_id INT UNIQUE,
    name VARCHAR(255) NOT NULL,
    document VARCHAR(50) UNIQUE,
    email VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Table: person_address_details (current address with person-specific data)
CREATE TABLE IF NOT EXISTS person_address_details (
    id UUID PRIMARY KEY,
    person_id UUID NOT NULL,
    zip_code VARCHAR(8) NOT NULL,
    number VARCHAR(20),
    complement VARCHAR(100),
    start_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_date TIMESTAMPTZ,
    CONSTRAINT fk_person_address_person FOREIGN KEY (person_id) REFERENCES people(id) ON DELETE CASCADE,
    CONSTRAINT fk_person_address_zipcode FOREIGN KEY (zip_code) REFERENCES addresses(zip_code) ON DELETE CASCADE
);

-- 4. Table: person_address_history (audit trail for address changes)
CREATE TABLE IF NOT EXISTS person_address_history (
    id UUID PRIMARY KEY,
    person_id UUID NOT NULL,
    zip_code VARCHAR(8) NOT NULL,
    street VARCHAR(255),
    neighborhood VARCHAR(100),
    city VARCHAR(100),
    state VARCHAR(2),
    number VARCHAR(20),
    complement VARCHAR(100),
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Table: user_accounts
CREATE TABLE IF NOT EXISTS user_accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    pin VARCHAR(4),
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_user_accounts_people FOREIGN KEY (id) REFERENCES people(id) ON DELETE CASCADE
);

-- 6. Table: employees
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY,
    initials VARCHAR(10) UNIQUE,
    role_level INT DEFAULT 1,
    CONSTRAINT fk_employees_people FOREIGN KEY (id) REFERENCES people(id) ON DELETE CASCADE
);

-- 7. Table: customers (no address fields, managed via person_address_details)
CREATE TABLE IF NOT EXISTS customers (
    id UUID PRIMARY KEY,
    is_authenticated BOOLEAN DEFAULT FALSE,
    notes TEXT,
    created_by_id UUID,
    CONSTRAINT fk_customers_people FOREIGN KEY (id) REFERENCES people(id) ON DELETE CASCADE,
    CONSTRAINT fk_customers_created_by FOREIGN KEY (created_by_id) REFERENCES employees(id)
);

-- 8. Table: customer_phones
CREATE TABLE IF NOT EXISTS customer_phones (
    customer_id UUID NOT NULL,
    phone VARCHAR(20) NOT NULL,
    PRIMARY KEY (customer_id, phone),
    CONSTRAINT fk_customer_phones_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_people_email ON people(email);
CREATE INDEX idx_people_document ON people(document);
CREATE INDEX idx_people_legacy_id ON people(legacy_id);
CREATE INDEX idx_user_accounts_username ON user_accounts(username);
CREATE INDEX idx_person_address_details_person_id ON person_address_details(person_id);
CREATE INDEX idx_person_address_details_end_date ON person_address_details(end_date);
CREATE INDEX idx_person_address_history_person_id ON person_address_history(person_id);
CREATE INDEX idx_person_address_history_start_date ON person_address_history(start_date DESC);
CREATE INDEX idx_customer_phones_customer_id ON customer_phones(customer_id);
CREATE INDEX idx_customers_created_by ON customers(created_by_id);
