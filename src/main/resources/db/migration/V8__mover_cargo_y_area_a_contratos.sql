WITH contratos_priorizados AS (
    SELECT
        contrato.id_contrato_empleado,
        empleado.id_cargo,
        empleado.id_area,
        ROW_NUMBER() OVER (
            PARTITION BY contrato.id_empleado
            ORDER BY
                (contrato.estado = 'VIGENTE') DESC,
                contrato.fecha_ingreso DESC,
                contrato.id_contrato_empleado DESC
        ) AS posicion
    FROM rrhh.contrato_empleado contrato
    JOIN core.empleado empleado
        ON empleado.id_empleado = contrato.id_empleado
)
UPDATE rrhh.contrato_empleado contrato
SET
    id_cargo = COALESCE(contrato.id_cargo, priorizado.id_cargo),
    id_area = COALESCE(contrato.id_area, priorizado.id_area)
FROM contratos_priorizados priorizado
WHERE priorizado.id_contrato_empleado = contrato.id_contrato_empleado
  AND priorizado.posicion = 1
  AND (contrato.id_cargo IS NULL OR contrato.id_area IS NULL);

ALTER TABLE core.empleado
    DROP CONSTRAINT IF EXISTS fk_empleado_cargo,
    DROP CONSTRAINT IF EXISTS fk_empleado_area,
    DROP COLUMN IF EXISTS id_cargo,
    DROP COLUMN IF EXISTS id_area;

DROP INDEX IF EXISTS core.idx_empleado_cargo;
DROP INDEX IF EXISTS core.idx_empleado_area;
