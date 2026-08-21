-- V15: Rename column logement_id to unit_id across tables
ALTER TABLE baux RENAME COLUMN logement_id TO unit_id;
ALTER TABLE electricity_statements RENAME COLUMN logement_id TO unit_id;
ALTER TABLE tickets RENAME COLUMN logement_id TO unit_id;
ALTER TABLE logement_equipements RENAME COLUMN logement_id TO unit_id;
