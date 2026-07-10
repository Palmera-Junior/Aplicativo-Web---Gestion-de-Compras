-- =============================================================================
-- 1. INSERTAR SEDES (Para probar los prefijos automáticos)
-- =============================================================================
INSERT INTO sede (nombre, prefijo_ciudad, direccion) VALUES
('Bogotá D.C.', 'BOG', 'Calle 26 # 69-76, Centro Administrativo'),
('Bucaramanga', 'BUC', 'Carrera 27 # 36-14, Bucaramanga, Santander');

-- =============================================================================
-- 2. INSERTAR USUARIOS (Para probar aislamiento de sedes y roles)
-- =============================================================================
-- Las contraseñas están en texto plano para la prueba, en producción usarás BCrypt.
INSERT INTO usuario (cedula, nombre, apellido, cargo, nombre_usuario, contrasenia, rol, id_sede) VALUES
('1010123456', 'Carlos', 'Gómez', 'Administrador General', 'carlos_admin', 'admin123', 'ADMIN', 1),
('1020987654', 'Felipe', 'Corzo', 'Auxiliar de Compras', 'felipe_dev', 'user123', 'USER', 1), -- Sede BOG
('1030456789', 'Ana', 'Martínez', 'Coordinadora de Sede', 'ana_buc', 'user456', 'USER', 2);   -- Sede BUC

-- =============================================================================
-- 3. INSERTAR PROVEEDORES (Frecuentes por Sede)
-- =============================================================================
INSERT INTO proveedor (nit, nombre, correo, direccion, telefono, ciudad, id_sede) VALUES
('860.123.456-1', 'Papelería El Centro S.A.S.', 'ventas@papeleriacentro.com', 'Av. Dorado # 45-12', '6015551234', 'Bogotá', 1),
('900.987.654-2', 'Suministros del Oriente', 'contacto@sumioriente.com', 'Calle 35 # 18-22', '6076334455', 'Bucaramanga', 2);

-- =============================================================================
-- 4. INSERTAR PRODUCTOS (Catálogo General)
-- =============================================================================
INSERT INTO producto (codigo_inventario, nombre, presentacion, descripcion) VALUES
('PROD-OFF-01', 'Resma de Papel Carta', 'Caja x 5 Resmas', 'Papel multipropósito de 75g para impresión'),
('PROD-TEC-02', 'Tinta Impresora Negra', 'Unidad 100ml', 'Tinta original de alta densidad'),
('PROD-CAF-03', 'Café Institucional', 'Bolsa x 500g', 'Café molido tipo exportación para cafetería');

-- =============================================================================
-- 5. PRUEBA ESCENARIO 1: Orden de Compra en Sede BOG con Proveedor Frecuente
-- =============================================================================
-- Insertar la cabecera de la orden (Generada por Felipe Corzo para Sede BOG)
INSERT INTO orden_compra (
    id_prov, id_sede, id_usuario, numero_orden, fecha,
    nombre_prov, nit_prov, direccion_prov, ciudad_prov, telefono_prov, correo_prov,
    observaciones, sub_total, iva_total, descuento, total
) VALUES (
    1, -- id_prov (Papelería El Centro)
    1, -- id_sede (Bogotá)
    2, -- id_usuario (Felipe Corzo)
    'BOG0701', -- Formato: Prefijo + Mes (07) + Consecutivo (01)
    CURRENT_TIMESTAMP,
    'Papelería El Centro S.A.S.', '860.123.456-1', 'Av. Dorado # 45-12', 'Bogotá', '6015551234', 'ventas@papeleriacentro.com',
    'Entrega prioritaria en la oficina del segundo piso.',
    145000.00, 27550.00, 0.00, 172550.00
);

-- Detalles de la Orden BOG0701 (id_orden = 1)
INSERT INTO detalle_compra (
    id_orden, id_producto, cantidad, codigo_inventario, descripcion, presentacion, valor_unitario, iva_producto, valor_total_linea
) VALUES 
(1, 1, 2, 'PROD-OFF-01', 'Resma de Papel Carta', 'Caja x 5 Resmas', 50000.00, 9500.00, 119000.00), -- 2 cajas de papel
(1, 2, 1, 'PROD-TEC-02', 'Tinta Impresora Negra', 'Unidad 100ml', 45000.00, 8550.00, 53550.00);    -- 1 tinta

-- =============================================================================
-- 6. PRUEBA ESCENARIO 2: Orden de Compra en Sede BUC usando opción 'OTRO'
-- =============================================================================
-- Insertar la cabecera (Generada por Ana Martínez para Sede BUC, id_prov queda NULL)
INSERT INTO orden_compra (
    id_prov, id_sede, id_usuario, numero_orden, fecha,
    nombre_prov, nit_prov, direccion_prov, ciudad_prov, telefono_prov, correo_prov,
    observaciones, sub_total, iva_total, descuento, total
) VALUES (
    NULL, -- id_prov es NULL porque se seleccionó 'Otro' (Proveedor casual)
    2,    -- id_sede (Bucaramanga)
    3,    -- id_usuario (Ana Martínez)
    'BUC0701', -- Formato: Prefijo BUC + Mes (07) + Consecutivo (01)
    CURRENT_TIMESTAMP,
    'Ferretería Don Pedro Casual', '912.345.678-9', 'Av. Quebradaseca # 14-05', 'Bucaramanga', '3159998877', 'donpedro@gmail.com',
    'Compra esporádica de insumos de reparación. No requiere guardar proveedor.',
    30000.00, 5700.00, 0.00, 35700.00
);

-- Detalles de la Orden BUC0701 (id_orden = 2)
INSERT INTO detalle_compra (
    id_orden, id_producto, cantidad, codigo_inventario, descripcion, presentacion, valor_unitario, iva_producto, valor_total_linea
) VALUES 
(2, 3, 2, 'PROD-CAF-03', 'Café Institucional', 'Bolsa x 500g', 15000.00, 2850.00, 35700.00);    -- 2 bolsas de café

