-- V25__Create_Guarantors_Table.sql
-- Table dédiée pour les garants associés aux locataires
CREATE TABLE IF NOT EXISTS guarantors (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_name VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    monthly_salary DOUBLE PRECISION,
    relationship VARCHAR(100),
    job_title VARCHAR(150),
    employer VARCHAR(150),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_guarantors_tenant_id ON guarantors(tenant_id);

-- Associer le garant au contrat de bail
ALTER TABLE leases ADD COLUMN IF NOT EXISTS guarantor_id UUID REFERENCES guarantors(id) ON DELETE SET NULL;
