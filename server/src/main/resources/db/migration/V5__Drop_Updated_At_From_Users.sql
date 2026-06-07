-- V5__Drop_Updated_At_From_Users.sql

-- Safely drop the old 'updated_at' column from the 'users' table,
-- as it is no longer mapped or required by the updated relational schema.
ALTER TABLE users DROP COLUMN IF EXISTS updated_at;
