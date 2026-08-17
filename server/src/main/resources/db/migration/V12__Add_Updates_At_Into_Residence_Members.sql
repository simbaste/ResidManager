-- Update residence_members to add updated_at column
ALTER TABLE residence_members
ADD COLUMN updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
