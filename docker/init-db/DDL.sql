-- DDL inicial para bases nuevas de Gestion de Compras.
-- Los schemas se crean en 01-create-schema.sql.

CREATE TABLE IF NOT EXISTS core.sede (
    id_sede SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    prefijo_ciudad VARCHAR(10) NOT NULL UNIQUE,
    direccion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS core.centro_costo (
    id_centro_costo SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    codigo VARCHAR(20) UNIQUE,
    direccion VARCHAR(200),
    id_sede INT NOT NULL,
    CONSTRAINT fk_centro_costo_sede FOREIGN KEY (id_sede)
        REFERENCES core.sede (id_sede)
);

CREATE TABLE IF NOT EXISTS core.proveedor (
    id_prov SERIAL PRIMARY KEY,
    nit BIGINT NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(150),
    direccion VARCHAR(255),
    telefono VARCHAR(50),
    ciudad VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS core.proveedor_sede (
    id_prov INT NOT NULL,
    id_sede INT NOT NULL,
    PRIMARY KEY (id_prov, id_sede),
    CONSTRAINT fk_proveedor_sede_proveedor FOREIGN KEY (id_prov)
        REFERENCES core.proveedor (id_prov) ON DELETE CASCADE,
    CONSTRAINT fk_proveedor_sede_sede FOREIGN KEY (id_sede)
        REFERENCES core.sede (id_sede) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS core.rol (
    id_rol SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    permisos_predeterminados_cargados BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS core.permiso (
    id_permiso SERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    nombre VARCHAR(120) NOT NULL
);

CREATE TABLE IF NOT EXISTS core.usuario (
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
    id_rol INT NOT NULL,
    id_sede INT NOT NULL,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol)
        REFERENCES core.rol (id_rol),
    CONSTRAINT fk_usuario_sede FOREIGN KEY (id_sede)
        REFERENCES core.sede (id_sede)
);

CREATE TABLE IF NOT EXISTS core.password_reset_token (
    id_password_reset_token BIGSERIAL PRIMARY KEY,
    id_usuario INT NOT NULL,
    token_hash CHAR(64) NOT NULL UNIQUE,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_en TIMESTAMPTZ NOT NULL,
    usado_en TIMESTAMPTZ,
    CONSTRAINT fk_password_reset_token_usuario FOREIGN KEY (id_usuario)
        REFERENCES core.usuario (id_usuario) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_password_reset_token_usuario
    ON core.password_reset_token (id_usuario);

CREATE TABLE IF NOT EXISTS core.password_reset_rate_limit (
    key_hash CHAR(64) PRIMARY KEY,
    window_start TIMESTAMPTZ NOT NULL,
    request_count INT NOT NULL
);

CREATE TABLE IF NOT EXISTS core.rol_permiso (
    id_rol INT NOT NULL,
    id_permiso INT NOT NULL,
    PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (id_rol)
        REFERENCES core.rol (id_rol) ON DELETE CASCADE,
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (id_permiso)
        REFERENCES core.permiso (id_permiso) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS core.usuario_permiso (
    id_usuario_permiso SERIAL PRIMARY KEY,
    id_usuario INT NOT NULL,
    id_permiso INT NOT NULL,
    concedido BOOLEAN NOT NULL,
    CONSTRAINT uq_usuario_permiso UNIQUE (id_usuario, id_permiso),
    CONSTRAINT fk_usuario_permiso_usuario FOREIGN KEY (id_usuario)
        REFERENCES core.usuario (id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_permiso_permiso FOREIGN KEY (id_permiso)
        REFERENCES core.permiso (id_permiso) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS compras.categoria (
    id_categoria SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(60) NOT NULL
);

CREATE TABLE IF NOT EXISTS compras.producto (
    id_producto SERIAL PRIMARY KEY,
    codigo_inventario VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    id_categoria INT NOT NULL,
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria)
        REFERENCES compras.categoria (id_categoria)
);

CREATE TABLE IF NOT EXISTS compras.presentacion_producto (
    id_presentacion SERIAL PRIMARY KEY,
    presentacion VARCHAR(150) NOT NULL,
    cantidad INT,
    unidad VARCHAR(20) NOT NULL,
    precio DECIMAL(10, 2) NOT NULL,
    id_producto INT NOT NULL,
    CONSTRAINT fk_presentacion_producto FOREIGN KEY (id_producto)
        REFERENCES compras.producto (id_producto) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS compras.orden_compra (
    id_orden SERIAL PRIMARY KEY,
    id_prov INT NOT NULL,
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
    id_usuario_anulacion INT,
    fecha_anulacion DATE,
    numero_factura VARCHAR(100),
    recibido_por VARCHAR(150),
    fecha_recepcion DATE,
    observacion_recepcion TEXT,
    foto_recepcion TEXT,
    foto_factura TEXT,
    se_recibio BOOLEAN NOT NULL DEFAULT FALSE,
    se_facturo BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_orden_proveedor FOREIGN KEY (id_prov)
        REFERENCES core.proveedor (id_prov) ON DELETE RESTRICT,
    CONSTRAINT fk_orden_sede FOREIGN KEY (id_sede)
        REFERENCES core.sede (id_sede),
    CONSTRAINT fk_orden_centro_costo FOREIGN KEY (id_centro_costo)
        REFERENCES core.centro_costo (id_centro_costo),
    CONSTRAINT fk_orden_usuario FOREIGN KEY (id_usuario)
        REFERENCES core.usuario (id_usuario),
    CONSTRAINT fk_orden_usuario_aprobacion FOREIGN KEY (id_usuario_aprobacion)
        REFERENCES core.usuario (id_usuario),
    CONSTRAINT fk_orden_usuario_anulacion FOREIGN KEY (id_usuario_anulacion)
        REFERENCES core.usuario (id_usuario)
);

CREATE TABLE IF NOT EXISTS compras.detalle_compra (
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
    CONSTRAINT fk_detalle_producto FOREIGN KEY (id_producto)
        REFERENCES compras.producto (id_producto),
    CONSTRAINT fk_detalle_orden FOREIGN KEY (id_orden)
        REFERENCES compras.orden_compra (id_orden) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS compras.auditoria_envio_correo (
    id_auditoria_correo BIGSERIAL PRIMARY KEY,
    id_orden INT NOT NULL,
    destinatario VARCHAR(150) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN (
        'PENDIENTE', 'PROCESANDO', 'ENVIADO', 'REINTENTAR', 'FALLIDO'
    )),
    tipo_envio VARCHAR(20) NOT NULL DEFAULT 'APROBACION' CHECK (tipo_envio IN (
        'APROBACION', 'FACTURACION'
    )),
    intentos INT NOT NULL DEFAULT 0,
    proximo_intento TIMESTAMP,
    bloqueado_en TIMESTAMP,
    enviado_en TIMESTAMP,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_error TEXT,
    CONSTRAINT fk_auditoria_orden FOREIGN KEY (id_orden)
        REFERENCES compras.orden_compra (id_orden) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auditoria_correo_estado_reintento
    ON compras.auditoria_envio_correo (estado, proximo_intento);

CREATE TABLE IF NOT EXISTS polizas.poliza (
    id_poliza SERIAL PRIMARY KEY,
    fecha_creacion DATE NOT NULL,
    fecha_vencimiento DATE,
    notificacion_proximo_vencimiento_en TIMESTAMP,
    id_prov INT NOT NULL,
    cliente VARCHAR(150) NOT NULL,
    descripcion TEXT,
    numero_contrato VARCHAR(50) NOT NULL UNIQUE,
    numero_poliza VARCHAR(100),
    valor_prima DECIMAL(15, 2) NOT NULL,
    valor_contrato DECIMAL(15, 2) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR' CHECK (estado IN (
        'BORRADOR', 'APROBADA', 'VIGENTE', 'VENCIDA', 'TERMINADA', 'ANULADA'
    )),
    id_sede INT NOT NULL,
    id_usuario INT NOT NULL,
    id_usuario_aprobacion INT,
    fecha_aprobacion DATE,
    id_usuario_activacion INT,
    id_usuario_anulacion INT,
    fecha_anulacion DATE,
    id_usuario_terminacion INT,
    fecha_terminacion DATE,
    motivo_terminacion TEXT,
    CONSTRAINT fk_poliza_proveedor FOREIGN KEY (id_prov)
        REFERENCES core.proveedor (id_prov),
    CONSTRAINT fk_poliza_sede FOREIGN KEY (id_sede)
        REFERENCES core.sede (id_sede),
    CONSTRAINT fk_poliza_usuario FOREIGN KEY (id_usuario)
        REFERENCES core.usuario (id_usuario),
    CONSTRAINT fk_poliza_usuario_aprobacion FOREIGN KEY (id_usuario_aprobacion)
        REFERENCES core.usuario (id_usuario),
    CONSTRAINT fk_poliza_usuario_activacion FOREIGN KEY (id_usuario_activacion)
        REFERENCES core.usuario (id_usuario),
    CONSTRAINT fk_poliza_usuario_anulacion FOREIGN KEY (id_usuario_anulacion)
        REFERENCES core.usuario (id_usuario),
    CONSTRAINT fk_poliza_usuario_terminacion FOREIGN KEY (id_usuario_terminacion)
        REFERENCES core.usuario (id_usuario),
    CONSTRAINT chk_poliza_valor_prima_no_negativo CHECK (valor_prima >= 0),
    CONSTRAINT chk_poliza_valor_contrato_no_negativo CHECK (valor_contrato >= 0)
);

CREATE INDEX IF NOT EXISTS idx_poliza_sede ON polizas.poliza (id_sede);
CREATE INDEX IF NOT EXISTS idx_poliza_estado ON polizas.poliza (estado);
CREATE INDEX IF NOT EXISTS idx_poliza_fecha_creacion ON polizas.poliza (fecha_creacion);

CREATE TABLE IF NOT EXISTS polizas.poliza_sede_aprobacion (
    id_poliza_sede_aprobacion SERIAL PRIMARY KEY,
    id_poliza INT NOT NULL,
    id_sede INT NOT NULL,
    estado_aprobacion VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE' CHECK (
        estado_aprobacion IN ('PENDIENTE', 'APROBADA', 'RECHAZADA')
    ),
    id_usuario_aprobacion INT,
    fecha_aprobacion DATE,
    CONSTRAINT uq_poliza_sede_aprobacion UNIQUE (id_poliza, id_sede),
    CONSTRAINT fk_poliza_sede_aprobacion_poliza FOREIGN KEY (id_poliza)
        REFERENCES polizas.poliza (id_poliza) ON DELETE CASCADE,
    CONSTRAINT fk_poliza_sede_aprobacion_sede FOREIGN KEY (id_sede)
        REFERENCES core.sede (id_sede),
    CONSTRAINT fk_poliza_sede_aprobacion_usuario FOREIGN KEY (id_usuario_aprobacion)
        REFERENCES core.usuario (id_usuario)
);

CREATE TABLE IF NOT EXISTS polizas.poliza_renovacion (
    id_renovacion SERIAL PRIMARY KEY,
    id_poliza INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    fecha_inicio_anterior DATE NOT NULL,
    fecha_fin_anterior DATE NOT NULL,
    fecha_registro TIMESTAMP NOT NULL,
    id_usuario INT NOT NULL,
    CONSTRAINT fk_poliza_renovacion_poliza FOREIGN KEY (id_poliza)
        REFERENCES polizas.poliza (id_poliza) ON DELETE CASCADE,
    CONSTRAINT fk_poliza_renovacion_usuario FOREIGN KEY (id_usuario)
        REFERENCES core.usuario (id_usuario),
    CONSTRAINT chk_poliza_renovacion_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT chk_poliza_renovacion_fechas_anteriores
        CHECK (fecha_fin_anterior >= fecha_inicio_anterior)
);

CREATE TABLE IF NOT EXISTS polizas.poliza_adjunto (
    id_adjunto_poliza SERIAL PRIMARY KEY,
    id_poliza INT NOT NULL,
    id_renovacion INT,
    tipo_adjunto VARCHAR(30) NOT NULL DEFAULT 'POLIZA_FISICA',
    contenido BYTEA,
    nombre_original VARCHAR(255) NOT NULL,
    fecha_carga TIMESTAMP NOT NULL,
    CONSTRAINT fk_poliza_adjunto_poliza FOREIGN KEY (id_poliza)
        REFERENCES polizas.poliza (id_poliza) ON DELETE CASCADE,
    CONSTRAINT fk_poliza_adjunto_renovacion FOREIGN KEY (id_renovacion)
        REFERENCES polizas.poliza_renovacion (id_renovacion) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_poliza_adjunto_poliza ON polizas.poliza_adjunto (id_poliza);

CREATE INDEX IF NOT EXISTS idx_poliza_renovacion_poliza
    ON polizas.poliza_renovacion (id_poliza, fecha_inicio);

CREATE INDEX IF NOT EXISTS idx_poliza_adjunto_renovacion
    ON polizas.poliza_adjunto (id_renovacion);

CREATE TABLE IF NOT EXISTS polizas.detalle_prima (
    id_detalle_prima SERIAL PRIMARY KEY,
    id_poliza INT NOT NULL,
    valor DECIMAL(15, 2) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    CONSTRAINT fk_detalle_prima_poliza FOREIGN KEY (id_poliza)
        REFERENCES polizas.poliza (id_poliza) ON DELETE CASCADE,
    CONSTRAINT chk_detalle_prima_valor_no_negativo CHECK (valor >= 0)
);

CREATE INDEX IF NOT EXISTS idx_detalle_prima_poliza ON polizas.detalle_prima (id_poliza);

CREATE TABLE IF NOT EXISTS polizas.auditoria_envio_correo_poliza (
    id_auditoria_correo_poliza BIGSERIAL PRIMARY KEY,
    id_poliza INT NOT NULL,
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
    CONSTRAINT fk_auditoria_poliza FOREIGN KEY (id_poliza)
        REFERENCES polizas.poliza (id_poliza) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auditoria_poliza_estado_reintento
    ON polizas.auditoria_envio_correo_poliza (estado, proximo_intento);
