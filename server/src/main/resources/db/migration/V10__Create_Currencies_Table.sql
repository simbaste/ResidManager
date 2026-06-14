-- Create table currencies
CREATE TABLE IF NOT EXISTS currencies (
    id UUID PRIMARY KEY,
    code VARCHAR(3) NOT NULL UNIQUE,
    symbol VARCHAR(10) NOT NULL,
    label VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default currencies
INSERT INTO currencies (id, code, symbol, label, created_at, updated_at) VALUES
('aaaaaa11-1111-1111-1111-111111111111', 'XOF', 'FCFA', 'Franc CFA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('bbbbbb22-2222-2222-2222-222222222222', 'EUR', '€', 'Euro', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cccccc33-3333-3333-3333-333333333333', 'USD', '$', 'US Dollar', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (code) DO NOTHING;

-- Modify residences table to reference currencies table
ALTER TABLE residences ADD COLUMN IF NOT EXISTS currency_id UUID REFERENCES currencies(id) ON DELETE RESTRICT;

-- Set existing residences currency to default Franc CFA (XOF)
UPDATE residences SET currency_id = 'aaaaaa11-1111-1111-1111-111111111111' WHERE currency_id IS NULL;

-- Set currency_id NOT NULL and drop the old currency_pivot column
ALTER TABLE residences ALTER COLUMN currency_id SET NOT NULL;
ALTER TABLE residences DROP COLUMN IF EXISTS currency_pivot;
