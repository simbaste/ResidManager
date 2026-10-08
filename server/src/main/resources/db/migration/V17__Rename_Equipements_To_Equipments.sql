-- V17: Rename table equipements to equipments, and logement_equipements to unit_equipments
ALTER TABLE equipements RENAME TO equipments;
ALTER TABLE logement_equipements RENAME TO unit_equipments;
ALTER TABLE unit_equipments RENAME COLUMN equipement_id TO equipment_id;
