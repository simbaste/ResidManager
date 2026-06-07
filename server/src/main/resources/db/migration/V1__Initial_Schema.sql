-- V1__Initial_Schema.sql
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE,
    phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS residences (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    address TEXT NOT NULL,
    photo_url TEXT,
    currency_pivot VARCHAR(3) DEFAULT 'XOF',
    kwh_price DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS residence_members (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    residence_id UUID NOT NULL REFERENCES residences(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, residence_id)
);

CREATE TABLE IF NOT EXISTS logements (
    id UUID PRIMARY KEY,
    residence_id UUID NOT NULL REFERENCES residences(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    floor VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    nominal_rent DOUBLE PRECISION NOT NULL,
    service_charges DOUBLE PRECISION NOT NULL,
    initial_electricity_index DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) DEFAULT 'AVAILABLE'
);

CREATE TABLE IF NOT EXISTS baux (
    id UUID PRIMARY KEY,
    logement_id UUID NOT NULL REFERENCES logements(id) ON DELETE RESTRICT,
    tenant_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    duration_months INT NOT NULL,
    payment_frequency VARCHAR(20) NOT NULL,
    deposit_amount DOUBLE PRECISION NOT NULL,
    deposit_status VARCHAR(20) DEFAULT 'PENDING',
    status VARCHAR(20) DEFAULT 'PENDING_PAYMENT',
    start_date DATE NOT NULL,
    end_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS electricity_statements (
    id UUID PRIMARY KEY,
    logement_id UUID NOT NULL REFERENCES logements(id) ON DELETE CASCADE,
    old_index DOUBLE PRECISION NOT NULL,
    new_index DOUBLE PRECISION NOT NULL,
    kwh_price_applied DOUBLE PRECISION NOT NULL,
    amount_due DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) DEFAULT 'UNPAID',
    statement_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS tickets (
    id UUID PRIMARY KEY,
    logement_id UUID NOT NULL REFERENCES logements(id) ON DELETE CASCADE,
    creator_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    urgency VARCHAR(20) NOT NULL,
    status VARCHAR(20) DEFAULT 'OPEN',
    intervention_cost DOUBLE PRECISION DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS financial_transactions (
    id UUID PRIMARY KEY,
    residence_id UUID NOT NULL REFERENCES residences(id) ON DELETE CASCADE,
    type VARCHAR(10) NOT NULL,
    category VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    description TEXT NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    transaction_date DATE NOT NULL
);
