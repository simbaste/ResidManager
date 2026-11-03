-- V27__Create_Residence_Unit_Photos_Table.sql
CREATE TABLE IF NOT EXISTS residence_unit_photos (
    id UUID PRIMARY KEY,
    residence_unit_id UUID NOT NULL REFERENCES residence_units(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    description VARCHAR(255) DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_residence_unit_photos_unit_id ON residence_unit_photos(residence_unit_id);

ALTER TABLE residence_units ADD COLUMN IF NOT EXISTS photo_url TEXT;
