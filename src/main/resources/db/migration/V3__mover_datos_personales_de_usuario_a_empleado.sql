INSERT INTO rrhh.cargo (codigo, nombre)
SELECT DISTINCT
    'LEGACY_' || substr(md5(btrim(usuario.cargo)), 1, 23),
    btrim(usuario.cargo)
FROM core.usuario usuario
WHERE NULLIF(btrim(usuario.cargo), '') IS NOT NULL
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO core.empleado (
    tipo_documento,
    numero_documento,
    primer_nombre,
    primer_apellido,
    id_cargo
)
SELECT
    'CEDULA_CIUDADANIA',
    usuario.cedula,
    COALESCE(NULLIF(btrim(usuario.nombre), ''), 'Sin nombre'),
    COALESCE(NULLIF(btrim(usuario.apellido), ''), 'Sin apellido'),
    cargo.id_cargo
FROM core.usuario usuario
LEFT JOIN rrhh.cargo cargo
    ON cargo.nombre = NULLIF(btrim(usuario.cargo), '')
WHERE NOT EXISTS (
    SELECT 1
    FROM core.empleado empleado
    WHERE empleado.tipo_documento = 'CEDULA_CIUDADANIA'
      AND empleado.numero_documento = usuario.cedula
)
ON CONFLICT (tipo_documento, numero_documento) DO NOTHING;

UPDATE core.usuario usuario
SET id_empleado = empleado.id_empleado
FROM core.empleado empleado
WHERE usuario.id_empleado IS NULL
  AND empleado.tipo_documento = 'CEDULA_CIUDADANIA'
  AND empleado.numero_documento = usuario.cedula;

ALTER TABLE core.usuario
    DROP COLUMN IF EXISTS cedula,
    DROP COLUMN IF EXISTS nombre,
    DROP COLUMN IF EXISTS apellido,
    DROP COLUMN IF EXISTS cargo;
