-- V20: Create inspection_reports and inspection_report_items tables

CREATE TABLE IF NOT EXISTS inspection_reports (
    id UUID PRIMARY KEY,
    lease_id UUID NOT NULL REFERENCES leases(id) ON DELETE CASCADE,
    inspector_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    type VARCHAR(20) NOT NULL,
    inspection_date DATE NOT NULL,
    general_condition VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    electricity_meter_index DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    water_meter_index DOUBLE PRECISION,
    gas_meter_index DOUBLE PRECISION,
    keys_count INT NOT NULL DEFAULT 1,
    comments TEXT,
    photo_urls TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_inspection_reports_lease_type UNIQUE (lease_id, type)
);

CREATE TABLE IF NOT EXISTS inspection_report_items (
    id UUID PRIMARY KEY,
    inspection_report_id UUID NOT NULL REFERENCES inspection_reports(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    name VARCHAR(255) NOT NULL,
    condition VARCHAR(20) NOT NULL DEFAULT 'GOOD',
    quantity INT NOT NULL DEFAULT 1,
    observations TEXT,
    photo_urls TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
