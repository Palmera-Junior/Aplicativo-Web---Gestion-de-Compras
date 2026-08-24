-- Migration: Add flags se_recibio and se_facturo to orden_compra
-- PostgreSQL

ALTER TABLE orden_compra
    ADD COLUMN IF NOT EXISTS se_recibio boolean NOT NULL DEFAULT false;

ALTER TABLE orden_compra
    ADD COLUMN IF NOT EXISTS se_facturo boolean NOT NULL DEFAULT false;

-- Ensure existing rows default to false (redundant due to DEFAULT and NOT NULL IF NOT EXISTS clause)
UPDATE orden_compra SET se_recibio = false WHERE se_recibio IS NULL;
UPDATE orden_compra SET se_facturo = false WHERE se_facturo IS NULL;
