-- V26__Create_Residence_Photos_Table.sql
CREATE TABLE IF NOT EXISTS residence_photos (
    id UUID PRIMARY KEY,
    residence_id UUID NOT NULL REFERENCES residences(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    description VARCHAR(255) DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_residence_photos_residence_id ON residence_photos(residence_id);
