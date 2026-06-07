-- V2__Add_First_And_Last_Name.sql

-- Migration to add first_name and last_name safely if they don't exist yet
ALTER TABLE users ADD COLUMN IF NOT EXISTS first_name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_name VARCHAR(100);

-- Migrate old data from 'name' column if present
-- (Checks if 'name' exists in systems column schema to avoid sql parse error)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='name') THEN
        UPDATE users SET first_name = split_part(name, ' ', 1) WHERE first_name IS NULL;
        UPDATE users SET last_name = COALESCE(nullif(substring(name from ' (.*)'), ''), 'ResidManager') WHERE last_name IS NULL;
    END IF;
END $$;

-- Set default values for any rows that are still null
UPDATE users SET first_name = 'Admin' WHERE first_name IS NULL;
UPDATE users SET last_name = 'ResidManager' WHERE last_name IS NULL;

-- Set not null constraints
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name SET NOT NULL;

-- Safely drop old column if it exists
ALTER TABLE users DROP COLUMN IF EXISTS name;
