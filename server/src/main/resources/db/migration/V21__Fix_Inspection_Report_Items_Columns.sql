-- V21: Ensure all expected columns exist on inspection_report_items and inspection_reports

ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS photo_urls TEXT;
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS observations TEXT;
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE inspection_reports ADD COLUMN IF NOT EXISTS gas_meter_index DOUBLE PRECISION;
ALTER TABLE inspection_reports ADD COLUMN IF NOT EXISTS water_meter_index DOUBLE PRECISION;
ALTER TABLE inspection_reports ADD COLUMN IF NOT EXISTS photo_urls TEXT;
ALTER TABLE inspection_reports ADD COLUMN IF NOT EXISTS comments TEXT;
ALTER TABLE inspection_reports ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE inspection_reports ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
