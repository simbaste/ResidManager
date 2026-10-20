-- V24__Add_Avatar_Url_To_Users.sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url TEXT;

