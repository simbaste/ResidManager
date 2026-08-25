-- V19: Deduplicate any existing duplicate names per residence before adding unique constraint

-- Rename duplicates by appending row number (e.g., 'Appt1 (2)') so no data is lost
UPDATE residence_units
SET name = residence_units.name || ' (' || ranked_units.rnum || ')'
FROM (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY residence_id, name ORDER BY created_at ASC) as rnum
    FROM residence_units
) ranked_units
WHERE residence_units.id = ranked_units.id AND ranked_units.rnum > 1;

-- Add the unique constraint
ALTER TABLE residence_units ADD CONSTRAINT uk_residence_units_residence_name UNIQUE (residence_id, name);
