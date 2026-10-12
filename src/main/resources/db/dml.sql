-- Datos mínimos de arranque de Gestión de Compras.
-- Se ejecuta al iniciar Spring mediante spring.sql.init.data-locations.
-- La contraseña de admin no se almacena en texto plano: es un hash BCrypt de admin123.

SET search_path TO dep_compras;

ALTER TABLE poliza ADD COLUMN IF NOT EXISTS id_usuario_terminacion INT;
ALTER TABLE poliza ADD COLUMN IF NOT EXISTS fecha_terminacion DATE;
ALTER TABLE poliza ADD COLUMN IF NOT EXISTS motivo_terminacion TEXT;
ALTER TABLE poliza ADD COLUMN IF NOT EXISTS id_usuario_activacion INT;
ALTER TABLE poliza ADD COLUMN IF NOT EXISTS id_usuario_anulacion INT;
ALTER TABLE poliza ADD COLUMN IF NOT EXISTS fecha_anulacion DATE;
ALTER TABLE orden_compra ADD COLUMN IF NOT EXISTS id_usuario_anulacion INT;
ALTER TABLE orden_compra ADD COLUMN IF NOT EXISTS fecha_anulacion DATE;
ALTER TABLE poliza ADD COLUMN IF NOT EXISTS notificacion_proximo_vencimiento_en TIMESTAMP;

-- Garantía física: no puede haber dos proveedores con el mismo NIT.
CREATE UNIQUE INDEX IF NOT EXISTS ux_proveedor_nit ON proveedor (nit);

-- La tabla está vacía: se eliminan directamente los campos históricos de proveedor.
ALTER TABLE orden_compra DROP COLUMN IF EXISTS proveedor_nit_snapshot;
ALTER TABLE orden_compra DROP COLUMN IF EXISTS proveedor_nombre_snapshot;
ALTER TABLE orden_compra DROP COLUMN IF EXISTS proveedor_ciudad_snapshot;
ALTER TABLE orden_compra DROP COLUMN IF EXISTS proveedor_direccion_snapshot;
ALTER TABLE orden_compra DROP COLUMN IF EXISTS proveedor_telefono_snapshot;
ALTER TABLE orden_compra DROP COLUMN IF EXISTS proveedor_correo_snapshot;

CREATE TABLE IF NOT EXISTS poliza_adjunto (
    id_adjunto_poliza SERIAL PRIMARY KEY,
    id_poliza INT NOT NULL REFERENCES poliza (id_poliza) ON DELETE CASCADE,
    tipo_adjunto VARCHAR(30) NOT NULL DEFAULT 'POLIZA_FISICA',
    contenido BYTEA,
    nombre_original VARCHAR(255) NOT NULL,
    fecha_carga TIMESTAMP NOT NULL
);
ALTER TABLE poliza_adjunto
    ADD COLUMN IF NOT EXISTS tipo_adjunto VARCHAR(30) NOT NULL DEFAULT 'POLIZA_FISICA';
ALTER TABLE poliza_adjunto ADD COLUMN IF NOT EXISTS contenido BYTEA;
ALTER TABLE poliza_adjunto ADD COLUMN IF NOT EXISTS id_renovacion INT;
CREATE INDEX IF NOT EXISTS idx_poliza_adjunto_renovacion ON poliza_adjunto (id_renovacion);
CREATE INDEX IF NOT EXISTS idx_poliza_adjunto_poliza ON poliza_adjunto (id_poliza);

ALTER TABLE poliza DROP CONSTRAINT IF EXISTS poliza_estado_check;
UPDATE poliza SET estado = 'TERMINADA'
WHERE estado = 'ANULADA' AND motivo_terminacion IS NOT NULL;
ALTER TABLE poliza ADD CONSTRAINT poliza_estado_check CHECK (estado IN (
    'BORRADOR', 'APROBADA', 'VIGENTE', 'VENCIDA', 'TERMINADA', 'ANULADA'
));

INSERT INTO sede (nombre, prefijo_ciudad, direccion)
VALUES ('Sede Nacional', 'NAC', 'Sede principal')
ON CONFLICT (prefijo_ciudad) DO NOTHING;

INSERT INTO proveedor_sede (id_prov, id_sede)
SELECT p.id_prov, s.id_sede
FROM proveedor p
JOIN sede s ON s.prefijo_ciudad = 'NAC'
ON CONFLICT (id_prov, id_sede) DO NOTHING;

INSERT INTO usuario (
    cedula,
    nombre,
    apellido,
    email,
    cargo,
    nombre_usuario,
    contraseña,
    rol,
    id_sede
)
SELECT
    '0000000000',
    'Administrador',
    'Sistema',
    'admin@palmerajunior.com',
    'Administrador del sistema',
    'admin',
    '$2a$10$LrFBseqXiuhqWYw1Y5fJA.dhsY2o4KozS/a.9eZ7rrACG.jJTggna',
    'ADMINISTRADOR',
    s.id_sede
FROM sede s
WHERE s.prefijo_ciudad = 'NAC'
  AND NOT EXISTS (
      SELECT 1
      FROM usuario u
      WHERE u.nombre_usuario = 'admin'
  );
