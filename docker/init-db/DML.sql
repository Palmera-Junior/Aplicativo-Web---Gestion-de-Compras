-- Datos iniciales de Gestion de Compras.
-- Se ejecuta despues de DDL.sql en una base PostgreSQL nueva.
SET search_path TO dep_compras;

INSERT INTO sede (id_sede, nombre, prefijo_ciudad, direccion) VALUES
    (1, 'Sede Nacional', 'NAC', 'Calle 10 # 40-20'),
    (2, 'Sede Bogota', 'BOG', 'Carrera 15 # 93-60')
ON CONFLICT (id_sede) DO NOTHING;

INSERT INTO centro_costo (id_centro_costo, nombre, codigo, direccion, id_sede) VALUES
    (1, 'Operaciones Medellin', 'CC-MED-001', 'Calle 10 # 40-20', 1),
    (2, 'Administracion Bogota', 'CC-BOG-001', 'Carrera 15 # 93-60', 2)
ON CONFLICT (id_centro_costo) DO NOTHING;

-- Usuario admin. La clave en texto plano es: admin123.
INSERT INTO usuario (
    id_usuario, cedula, nombre, apellido, email, cargo, nombre_usuario, contraseña, rol, id_sede
) VALUES (
    1, '1000000001', 'Administrador', 'Sistema', 'notificaciones@palmerajunior.com',
    'Administrador del sistema', 'admin',
    '$2a$10$OeAdhw8pNCI3H.ZPGKKQYOq8KFQ9Xo.YfRT.u1ipv2pTW.MYE203q',
    'ADMINISTRADOR', 1
)
ON CONFLICT (id_usuario) DO NOTHING;

INSERT INTO usuario (
    id_usuario, cedula, nombre, apellido, email, cargo, nombre_usuario, contraseña, rol, id_sede
) VALUES (
    2, '1000000002', 'Usuario', 'Aprobador', 'aprobador@palmerajunior.com',
    'Aprobador de compras', 'aprobador',
    '$2a$10$OeAdhw8pNCI3H.ZPGKKQYOq8KFQ9Xo.YfRT.u1ipv2pTW.MYE203q',
    'APROBADOR', 1
)
ON CONFLICT (id_usuario) DO NOTHING;

INSERT INTO usuario (
    id_usuario, cedula, nombre, apellido, email, cargo, nombre_usuario, contraseña, rol, id_sede
) VALUES (
    3, '1000000003', 'Usuario', 'Solicitante', 'solicitante@palmerajunior.com',
    'Solicitante de compras', 'solicitante',
    '$2a$10$OeAdhw8pNCI3H.ZPGKKQYOq8KFQ9Xo.YfRT.u1ipv2pTW.MYE203q',
    'SOLICITANTE', 1
)
ON CONFLICT (id_usuario) DO NOTHING;

INSERT INTO proveedor (id_prov, nit, nombre, correo, direccion, telefono, ciudad) VALUES
    (1, '900123456-1', 'Suministros Andinos SAS', 'compras@suministrosandinos.com',
     'Carrera 50 # 20-10', '6044440000', 'Medellin'),
    (2, '901234567-2', 'Equipos Nacionales SAS', 'ventas@equiposnacionales.com',
     'Calle 80 # 12-30', '6015550000', 'Bogota')
ON CONFLICT (id_prov) DO NOTHING;

INSERT INTO proveedor_sede (id_prov, id_sede) VALUES
    (1, 1),
    (2, 2)
ON CONFLICT DO NOTHING;

INSERT INTO producto (id_producto, codigo_inventario, nombre, categoria) VALUES
    (1, 'EPP-001', 'Guantes de nitrilo', 'EPPS'),
    (2, 'PAP-001', 'Resma de papel carta', 'PAPELERIA'),
    (3, 'EQP-001', 'Monitor empresarial 24 pulgadas', 'EQUIPOS')
ON CONFLICT (id_producto) DO NOTHING;

INSERT INTO presentacion_producto (id_presentacion, presentacion, cantidad, unidad, precio, id_producto) VALUES
    (1, 'Caja', 100, 'unidades', 85000.00, 1),
    (2, 'Paquete', 500, 'hojas', 22000.00, 2),
    (3, 'Unidad', 1, 'unidad', 650000.00, 3)
ON CONFLICT (id_presentacion) DO NOTHING;


-- Ajustar secuencias despues de insertar IDs iniciales explicitos.
SELECT setval(pg_get_serial_sequence('sede', 'id_sede'), COALESCE(MAX(id_sede), 1)) FROM sede;
SELECT setval(pg_get_serial_sequence('centro_costo', 'id_centro_costo'), COALESCE(MAX(id_centro_costo), 1)) FROM centro_costo;
SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'), COALESCE(MAX(id_usuario), 1)) FROM usuario;
SELECT setval(pg_get_serial_sequence('proveedor', 'id_prov'), COALESCE(MAX(id_prov), 1)) FROM proveedor;
SELECT setval(pg_get_serial_sequence('producto', 'id_producto'), COALESCE(MAX(id_producto), 1)) FROM producto;
SELECT setval(pg_get_serial_sequence('presentacion_producto', 'id_presentacion'), COALESCE(MAX(id_presentacion), 1)) FROM presentacion_producto;
SELECT setval(pg_get_serial_sequence('orden_compra', 'id_orden'), COALESCE(MAX(id_orden), 1)) FROM orden_compra;
SELECT setval(pg_get_serial_sequence('detalle_compra', 'id_detalle'), COALESCE(MAX(id_detalle), 1)) FROM detalle_compra;
SELECT setval(pg_get_serial_sequence('auditoria_envio_correo', 'id_auditoria_correo'), COALESCE(MAX(id_auditoria_correo), 1)) FROM auditoria_envio_correo;
