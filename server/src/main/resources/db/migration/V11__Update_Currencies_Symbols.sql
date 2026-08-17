-- Update currencies symbols
UPDATE currencies SET symbol = 'FRANC_CFA' WHERE code = 'XOF';
UPDATE currencies SET symbol = 'EURO' WHERE code = 'EUR';
UPDATE currencies SET symbol = 'DOLLAR_US' WHERE code = 'USD';

-- Delete label column from currencies tables
ALTER TABLE currencies DROP COLUMN label;
