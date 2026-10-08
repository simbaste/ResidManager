-- V13__Create_Residence_Applications_Table.sql
CREATE TABLE IF NOT EXISTS residence_applications (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    residence_id UUID NOT NULL REFERENCES residences(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, residence_id)
);
