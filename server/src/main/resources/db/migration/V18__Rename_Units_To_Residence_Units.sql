-- V18: Rename units to residence_units and update foreign key columns
ALTER TABLE units RENAME TO residence_units;
ALTER TABLE leases RENAME COLUMN unit_id TO residence_unit_id;
ALTER TABLE electricity_statements RENAME COLUMN unit_id TO residence_unit_id;
ALTER TABLE tickets RENAME COLUMN unit_id TO residence_unit_id;
ALTER TABLE unit_equipments RENAME COLUMN unit_id TO residence_unit_id;
ALTER TABLE unit_equipments RENAME TO residence_unit_equipments;
