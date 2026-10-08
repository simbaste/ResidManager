-- V23__Normalize_Financial_Transactions_Categories.sql
-- Normalize existing financial_transactions category and type columns to uppercase enum values

UPDATE financial_transactions
SET category = CASE UPPER(TRIM(category))
    WHEN 'RENT' THEN 'RENT'
    WHEN 'DEPOSIT' THEN 'DEPOSIT'
    WHEN 'LEASE PAYMENT' THEN 'LEASE_PAYMENT'
    WHEN 'LEASE_PAYMENT' THEN 'LEASE_PAYMENT'
    WHEN 'ELECTRICITY' THEN 'ELECTRICITY'
    WHEN 'MAINTENANCE' THEN 'MAINTENANCE'
    WHEN 'CLEANING' THEN 'CLEANING'
    WHEN 'FUEL' THEN 'FUEL'
    WHEN 'SECURITY' THEN 'SECURITY'
    WHEN 'TAXES' THEN 'TAXES'
    WHEN 'OTHER' THEN 'OTHER'
    ELSE UPPER(REPLACE(TRIM(category), ' ', '_'))
END
WHERE category IS NOT NULL;

UPDATE financial_transactions
SET type = UPPER(TRIM(type))
WHERE type IS NOT NULL;

UPDATE financial_transactions
SET related_entity_type = CASE UPPER(TRIM(related_entity_type))
    WHEN 'BAIL' THEN 'BAIL'
    WHEN 'ELECTRICITY_STATEMENT' THEN 'ELECTRICITY_STATEMENT'
    WHEN 'ELECTRICITY STATEMENT' THEN 'ELECTRICITY_STATEMENT'
    WHEN 'TICKET' THEN 'TICKET'
    ELSE UPPER(REPLACE(TRIM(related_entity_type), ' ', '_'))
END
WHERE related_entity_type IS NOT NULL;
