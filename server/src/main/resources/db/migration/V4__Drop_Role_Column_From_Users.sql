-- V4__Drop_Role_Column_From_Users.sql

-- Safely drop the old 'role' column from the 'users' table,
-- as roles are now correctly managed per-residence in 'residence_members'.
ALTER TABLE users DROP COLUMN IF EXISTS role;
