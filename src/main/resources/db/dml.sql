-- Datos mínimos de arranque de Gestión de Compras.
-- Se ejecuta al iniciar Spring mediante spring.sql.init.data-locations.
-- La contraseña de admin no se almacena en texto plano: es un hash BCrypt de admin123.

SET search_path TO dep_compras;

INSERT INTO sede (nombre, prefijo_ciudad, direccion)
VALUES ('Sede Nacional', 'NAC', 'Sede principal')
ON CONFLICT (prefijo_ciudad) DO NOTHING;

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
