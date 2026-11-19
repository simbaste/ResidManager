-- V28__Add_Draft_Status_To_Leases.sql
-- In PostgreSQL we don't necessarily have to alter an enum if it's stored as VARCHAR
-- The status column in leases (formerly baux) is VARCHAR(20) DEFAULT 'PENDING_PAYMENT'
-- 'DRAFT' will fit without schema changes.

SELECT 1;
