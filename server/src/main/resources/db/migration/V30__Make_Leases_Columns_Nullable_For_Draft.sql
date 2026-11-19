-- V30__Make_Leases_Columns_Nullable_For_Draft.sql
ALTER TABLE leases ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE leases ALTER COLUMN duration_months DROP NOT NULL;
ALTER TABLE leases ALTER COLUMN payment_frequency DROP NOT NULL;
ALTER TABLE leases ALTER COLUMN deposit_amount DROP NOT NULL;
ALTER TABLE leases ALTER COLUMN start_date DROP NOT NULL;
ALTER TABLE leases ALTER COLUMN end_date DROP NOT NULL;
ALTER TABLE leases ALTER COLUMN advance_months DROP NOT NULL;
ALTER TABLE leases ALTER COLUMN advance_payment_amount DROP NOT NULL;
ALTER TABLE leases ALTER COLUMN deposit_status DROP NOT NULL;
