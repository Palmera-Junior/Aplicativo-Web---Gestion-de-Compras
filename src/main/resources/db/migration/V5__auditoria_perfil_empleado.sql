ALTER TABLE rrhh.contrato_empleado
    ALTER COLUMN tarifa_arl_porcentaje TYPE NUMERIC(10, 2),
    ADD COLUMN IF NOT EXISTS id_cargo INT,
    ADD COLUMN IF NOT EXISTS id_area INT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_contrato_empleado_cargo'
          AND conrelid = 'rrhh.contrato_empleado'::regclass
    ) THEN
        ALTER TABLE rrhh.contrato_empleado
            ADD CONSTRAINT fk_contrato_empleado_cargo
            FOREIGN KEY (id_cargo) REFERENCES rrhh.cargo(id_cargo) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_contrato_empleado_area'
          AND conrelid = 'rrhh.contrato_empleado'::regclass
    ) THEN
        ALTER TABLE rrhh.contrato_empleado
            ADD CONSTRAINT fk_contrato_empleado_area
            FOREIGN KEY (id_area) REFERENCES rrhh.area(id_area) ON DELETE RESTRICT;
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_contrato_empleado_cargo
    ON rrhh.contrato_empleado (id_cargo);

CREATE INDEX IF NOT EXISTS idx_contrato_empleado_area
    ON rrhh.contrato_empleado (id_area);

UPDATE rrhh.tipo_adjunto_empleado
SET permite_multiples_vigentes = TRUE
WHERE codigo = '3.8';

CREATE TABLE IF NOT EXISTS rrhh.empleado_perfil_historial (
    id_empleado_perfil_historial BIGSERIAL PRIMARY KEY,
    id_empleado INT NOT NULL REFERENCES core.empleado(id_empleado) ON DELETE CASCADE,
    id_usuario INT NOT NULL REFERENCES core.usuario(id_usuario) ON DELETE RESTRICT,
    campo VARCHAR(80) NOT NULL,
    valor_anterior TEXT,
    valor_nuevo TEXT,
    motivo TEXT NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_empleado_perfil_historial_empleado_fecha
    ON rrhh.empleado_perfil_historial (id_empleado, fecha_cambio DESC);