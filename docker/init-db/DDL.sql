-- Esquema inicial de Gestion de Compras.
-- Se ejecuta despues de 01-create-schema.sql dentro de PostgreSQL.
SET search_path TO dep_compras;

CREATE TABLE sede (
    id_sede SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    prefijo_ciudad VARCHAR(10) NOT NULL UNIQUE,
    direccion VARCHAR(255)
);

CREATE TABLE centro_costo (
    id_centro_costo SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(20) UNIQUE,
    direccion VARCHAR(200),
    id_sede INT NOT NULL,
    CONSTRAINT fk_centro_costo_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede)
);

CREATE TABLE producto (
    id_producto SERIAL PRIMARY KEY,
    codigo_inventario VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    categoria VARCHAR(30) NOT NULL CHECK (categoria IN (
        'EPPS', 'MECANISMOS', 'EQUIPOS', 'PAPELERIA', 'DOTACION', 'REPUESTOS', 'PRODUCTOS'
    ))
);

CREATE TABLE presentacion_producto (
    id_presentacion SERIAL PRIMARY KEY,
    presentacion VARCHAR(150) NOT NULL,
    cantidad INT,
    unidad VARCHAR(20) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    id_producto INT NOT NULL,
    CONSTRAINT fk_presentacion_producto FOREIGN KEY (id_producto)
        REFERENCES producto (id_producto) ON DELETE CASCADE
);

CREATE TABLE proveedor (
    id_prov SERIAL PRIMARY KEY,
    nit VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(150),
    direccion VARCHAR(255),
    telefono VARCHAR(50),
    ciudad VARCHAR(150)
);

CREATE TABLE proveedor_sede (
    id_prov INT NOT NULL,
    id_sede INT NOT NULL,
    PRIMARY KEY (id_prov, id_sede),
    CONSTRAINT fk_proveedor_sede_proveedor FOREIGN KEY (id_prov)
        REFERENCES proveedor (id_prov) ON DELETE CASCADE,
    CONSTRAINT fk_proveedor_sede_sede FOREIGN KEY (id_sede)
        REFERENCES sede (id_sede) ON DELETE CASCADE
);

CREATE TABLE usuario (
    id_usuario SERIAL PRIMARY KEY,
    cedula VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    proveedor VARCHAR(100),
    proveedor_id VARCHAR(100),
    cargo VARCHAR(100),
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    contraseña VARCHAR(255) NOT NULL,
    rol VARCHAR(20) NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'APROBADOR', 'SOLICITANTE')),
    id_sede INT NOT NULL,
    CONSTRAINT fk_usuario_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede)
);

CREATE TABLE orden_compra (
    id_orden SERIAL PRIMARY KEY,
    id_prov INT,
    fecha DATE NOT NULL,
    numero_orden VARCHAR(20) UNIQUE,
    estado VARCHAR(20) NOT NULL CHECK (estado IN (
        'BORRADOR', 'APROBADA', 'RECIBIDA', 'FACTURADA', 'COMPLETADA', 'ANULADA'
    )),
    id_sede INT NOT NULL,
    id_centro_costo INT,
    observaciones TEXT,
    sub_total DECIMAL(10, 2) NOT NULL,
    iva_total DECIMAL(10, 2) NOT NULL,
    descuento DECIMAL(10, 2),
    paga_flete BOOLEAN NOT NULL DEFAULT FALSE,
    valor_flete DECIMAL(10, 2),
    total DECIMAL(10, 2) NOT NULL,
    id_usuario INT NOT NULL,
    id_usuario_aprobacion INT,
    fecha_aprobacion DATE,
    numero_factura VARCHAR(100),
    recibido_por VARCHAR(150),
    fecha_recepcion DATE,
    observacion_recepcion TEXT,
    foto_recepcion TEXT,
    foto_factura TEXT,
    se_recibio BOOLEAN NOT NULL DEFAULT FALSE,
    se_facturo BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_orden_proveedor FOREIGN KEY (id_prov)
        REFERENCES proveedor (id_prov) ON DELETE SET NULL,
    CONSTRAINT fk_orden_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT fk_orden_centro_costo FOREIGN KEY (id_centro_costo)
        REFERENCES centro_costo (id_centro_costo),
    CONSTRAINT fk_orden_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_orden_usuario_aprobacion FOREIGN KEY (id_usuario_aprobacion)
        REFERENCES usuario (id_usuario)
);

CREATE TABLE detalle_compra (
    id_detalle SERIAL PRIMARY KEY,
    cantidad INT NOT NULL,
    cantidad_recibida INT DEFAULT 0,
    recibido BOOLEAN DEFAULT FALSE,
    codigo_inventario VARCHAR(50) NOT NULL,
    descripcion TEXT,
    presentacion VARCHAR(150) NOT NULL,
    valor_unitario DECIMAL(10, 2) NOT NULL,
    iva_producto DECIMAL(10, 2) NOT NULL,
    valor_iva DECIMAL(10, 2) NOT NULL,
    valor_total_linea DECIMAL(10, 2) NOT NULL,
    id_producto INT,
    id_orden INT NOT NULL,
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_detalle_orden FOREIGN KEY (id_orden)
        REFERENCES orden_compra (id_orden) ON DELETE CASCADE
);

CREATE TABLE auditoria_envio_correo (
    id_auditoria_correo BIGSERIAL PRIMARY KEY,
    id_orden INT NOT NULL,
    destinatario VARCHAR(150) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN (
        'PENDIENTE', 'PROCESANDO', 'ENVIADO', 'REINTENTAR', 'FALLIDO'
    )),
    intentos INT NOT NULL DEFAULT 0,
    proximo_intento TIMESTAMP,
    bloqueado_en TIMESTAMP,
    enviado_en TIMESTAMP,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_error TEXT,
    CONSTRAINT fk_auditoria_orden FOREIGN KEY (id_orden)
        REFERENCES orden_compra (id_orden) ON DELETE CASCADE
);

CREATE INDEX idx_auditoria_correo_estado_reintento
    ON auditoria_envio_correo (estado, proximo_intento);
