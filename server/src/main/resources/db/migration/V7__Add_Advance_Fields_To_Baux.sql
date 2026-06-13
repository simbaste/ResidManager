-- V7__Add_Advance_Fields_To_Baux.sql

-- Add advance columns to baux table
ALTER TABLE baux ADD COLUMN IF NOT EXISTS advance_months INT NOT NULL DEFAULT 1;
ALTER TABLE baux ADD COLUMN IF NOT EXISTS advance_payment_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0;
