-- V22: Add all required columns to inspection_report_items in case previous migration missed them

ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS photo_urls TEXT;
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS observations TEXT;
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS quantity INT DEFAULT 1;
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS condition VARCHAR(20) DEFAULT 'GOOD';
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS category VARCHAR(30) DEFAULT 'OTHER';
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE inspection_report_items ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
