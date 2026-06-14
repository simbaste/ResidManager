-- Create table ticket_categories
CREATE TABLE IF NOT EXISTS ticket_categories (
    id UUID PRIMARY KEY,
    residence_id UUID REFERENCES residences(id) ON DELETE CASCADE,
    key VARCHAR(50) NOT NULL UNIQUE,
    label VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default categories with standard static UUIDs
INSERT INTO ticket_categories (id, key, label, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 'PLUMBING', 'Plomberie', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222222', 'ELECTRICITY', 'Électricité', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', 'TILES', 'Carrelage', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('44444444-4444-4444-4444-444444444444', 'CEILING', 'Plafond / Cloisons', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555555', 'OTHER', 'Autre incident', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (key) DO NOTHING;

-- Modify tickets table to support category_id relation
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS category_id UUID REFERENCES ticket_categories(id) ON DELETE RESTRICT;

-- Migrate existing data based on varchar category name
UPDATE tickets SET category_id = '11111111-1111-1111-1111-111111111111' WHERE category = 'PLUMBING';
UPDATE tickets SET category_id = '22222222-2222-2222-2222-222222222222' WHERE category = 'ELECTRICITY';
UPDATE tickets SET category_id = '33333333-3333-3333-3333-333333333333' WHERE category = 'TILES';
UPDATE tickets SET category_id = '44444444-4444-4444-4444-444444444444' WHERE category = 'CEILING';
UPDATE tickets SET category_id = '55555555-5555-5555-5555-555555555555' WHERE category = 'OTHER';

-- Fallback for any other custom ones
UPDATE tickets SET category_id = '55555555-5555-5555-5555-555555555555' WHERE category_id IS NULL;

-- Set category_id NOT NULL and drop the old varchar column
ALTER TABLE tickets ALTER COLUMN category_id SET NOT NULL;
ALTER TABLE tickets DROP COLUMN IF EXISTS category;
