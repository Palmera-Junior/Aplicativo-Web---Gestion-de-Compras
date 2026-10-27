-- Datos mínimos de arranque de Gestión de Compras.
-- Se ejecuta al iniciar Spring mediante spring.sql.init.data-locations.
-- La contraseña de admin no se almacena en texto plano: es un hash BCrypt de admin123.

INSERT INTO compras.categoria (codigo, nombre) VALUES
    ('EPPS', 'EPPS'),
    ('MECANISMOS', 'Mecanismos'),
    ('EQUIPOS', 'Equipos'),
    ('PAPELERIA', 'Papelería'),
    ('DOTACION', 'Dotación'),
    ('REPUESTOS', 'Repuestos'),
    ('PRODUCTOS', 'Productos')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO core.rol (codigo) VALUES
    ('SUPERADMINISTRADOR'),
    ('ADMINISTRADOR'),
    ('APROBADOR'),
    ('SOLICITANTE'),
    ('COMERCIAL')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO core.permiso (codigo, nombre) VALUES
    ('PANEL_ADMINISTRACION', 'Acceder al panel de administración'),
    ('PRODUCTOS_GESTIONAR', 'Gestionar productos'),
    ('PROVEEDORES_GESTIONAR', 'Gestionar proveedores'),
    ('SEDES_GESTIONAR', 'Gestionar sedes'),
    ('CENTROS_COSTO_GESTIONAR', 'Gestionar centros de costo'),
    ('POLIZAS_VER', 'Consultar pólizas'),
    ('POLIZAS_GESTIONAR', 'Gestionar pólizas'),
    ('POLIZAS_APROBAR', 'Aprobar pólizas'),
    ('ORDENES_VER', 'Consultar órdenes de compra'),
    ('ORDENES_GESTIONAR', 'Gestionar órdenes de compra'),
    ('ORDENES_APROBAR', 'Aprobar órdenes de compra')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO core.rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM (
    VALUES
        ('SUPERADMINISTRADOR', 'PANEL_ADMINISTRACION'),
        ('SUPERADMINISTRADOR', 'PRODUCTOS_GESTIONAR'),
        ('SUPERADMINISTRADOR', 'PROVEEDORES_GESTIONAR'),
        ('SUPERADMINISTRADOR', 'SEDES_GESTIONAR'),
        ('SUPERADMINISTRADOR', 'CENTROS_COSTO_GESTIONAR'),
        ('SUPERADMINISTRADOR', 'POLIZAS_VER'),
        ('SUPERADMINISTRADOR', 'POLIZAS_GESTIONAR'),
        ('SUPERADMINISTRADOR', 'POLIZAS_APROBAR'),
        ('SUPERADMINISTRADOR', 'ORDENES_VER'),
        ('SUPERADMINISTRADOR', 'ORDENES_GESTIONAR'),
        ('SUPERADMINISTRADOR', 'ORDENES_APROBAR'),
        ('ADMINISTRADOR', 'PANEL_ADMINISTRACION'),
        ('ADMINISTRADOR', 'PRODUCTOS_GESTIONAR'),
        ('ADMINISTRADOR', 'PROVEEDORES_GESTIONAR'),
        ('ADMINISTRADOR', 'SEDES_GESTIONAR'),
        ('ADMINISTRADOR', 'CENTROS_COSTO_GESTIONAR'),
        ('ADMINISTRADOR', 'POLIZAS_VER'),
        ('ADMINISTRADOR', 'POLIZAS_GESTIONAR'),
        ('ADMINISTRADOR', 'POLIZAS_APROBAR'),
        ('ADMINISTRADOR', 'ORDENES_VER'),
        ('ADMINISTRADOR', 'ORDENES_GESTIONAR'),
        ('ADMINISTRADOR', 'ORDENES_APROBAR'),
        ('APROBADOR', 'POLIZAS_VER'),
        ('APROBADOR', 'POLIZAS_GESTIONAR'),
        ('APROBADOR', 'POLIZAS_APROBAR'),
        ('APROBADOR', 'ORDENES_VER'),
        ('APROBADOR', 'ORDENES_APROBAR'),
        ('SOLICITANTE', 'ORDENES_VER'),
        ('SOLICITANTE', 'ORDENES_GESTIONAR'),
        ('COMERCIAL', 'POLIZAS_VER'),
        ('COMERCIAL', 'POLIZAS_GESTIONAR')
) AS defaults(codigo_rol, codigo_permiso)
JOIN core.rol r ON r.codigo = defaults.codigo_rol
JOIN core.permiso p ON p.codigo = defaults.codigo_permiso
WHERE NOT r.permisos_predeterminados_cargados
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

UPDATE core.rol
SET permisos_predeterminados_cargados = TRUE
WHERE codigo IN ('SUPERADMINISTRADOR', 'ADMINISTRADOR', 'APROBADOR', 'SOLICITANTE', 'COMERCIAL')
  AND NOT permisos_predeterminados_cargados;

INSERT INTO core.sede (nombre, prefijo_ciudad, direccion)
VALUES ('Sede Nacional', 'NAC', 'Sede principal')
ON CONFLICT (prefijo_ciudad) DO NOTHING;

INSERT INTO core.usuario (
    cedula,
    nombre,
    apellido,
    email,
    cargo,
    nombre_usuario,
    contraseña,
    id_rol,
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
    r.id_rol,
    s.id_sede
FROM core.sede s
JOIN core.rol r ON r.codigo = 'SUPERADMINISTRADOR'
WHERE s.prefijo_ciudad = 'NAC'
ON CONFLICT (nombre_usuario) DO UPDATE
SET id_rol = EXCLUDED.id_rol;
