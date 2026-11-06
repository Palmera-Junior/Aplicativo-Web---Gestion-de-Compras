ALTER TABLE core.empleado
    ADD COLUMN IF NOT EXISTS estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRO_INICIAL',
    ADD COLUMN aprobado_por INT,
    ADD COLUMN aprobado_en TIMESTAMPTZ,
    ADD COLUMN completado_por INT,
    ADD COLUMN completado_en TIMESTAMPTZ,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_empleado_aprobado_por FOREIGN KEY (aprobado_por)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_empleado_completado_por FOREIGN KEY (completado_por)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_empleado_aprobacion_auditoria CHECK (
        (aprobado_por IS NULL AND aprobado_en IS NULL)
        OR (aprobado_por IS NOT NULL AND aprobado_en IS NOT NULL)
    ),
    ADD CONSTRAINT chk_empleado_completitud_auditoria CHECK (
        (completado_por IS NULL AND completado_en IS NULL)
        OR (completado_por IS NOT NULL AND completado_en IS NOT NULL)
    );

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_empleado_estado'
          AND conrelid = 'core.empleado'::regclass
    ) THEN
        ALTER TABLE core.empleado
            ADD CONSTRAINT chk_empleado_estado CHECK (
                estado IN ('REGISTRO_INICIAL', 'PROCESO_CONTRATACION', 'ACTIVO', 'RETIRADO')
            );
    END IF;
END;
$$;

ALTER TABLE core.empleado
    ALTER COLUMN estado SET DEFAULT 'REGISTRO_INICIAL';

UPDATE core.empleado
SET estado = 'REGISTRO_INICIAL'
WHERE estado IS NULL;

ALTER TABLE core.empleado
    ALTER COLUMN estado SET NOT NULL;

CREATE TABLE rrhh.empleado_historial_estado (
    id_empleado_historial_estado BIGSERIAL PRIMARY KEY,
    id_empleado INT NOT NULL,
    estado_anterior VARCHAR(30),
    estado_nuevo VARCHAR(30) NOT NULL,
    id_usuario INT NOT NULL,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalle VARCHAR(500),
    CONSTRAINT chk_empleado_historial_estado_anterior CHECK (
        estado_anterior IS NULL
        OR estado_anterior IN (
            'REGISTRO_INICIAL', 'PROCESO_CONTRATACION', 'ACTIVO', 'RETIRADO'
        )
    ),
    CONSTRAINT chk_empleado_historial_estado_nuevo CHECK (
        estado_nuevo IN (
            'REGISTRO_INICIAL', 'PROCESO_CONTRATACION', 'ACTIVO', 'RETIRADO'
        )
    ),
    CONSTRAINT fk_empleado_historial_estado_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE RESTRICT,
    CONSTRAINT fk_empleado_historial_estado_usuario FOREIGN KEY (id_usuario)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT
);

CREATE INDEX idx_empleado_historial_estado_empleado_fecha
    ON rrhh.empleado_historial_estado (id_empleado, fecha_cambio DESC);

ALTER TABLE rrhh.empleado_adjunto_requerido
    ADD COLUMN etapa_requerida VARCHAR(30) NOT NULL DEFAULT 'CONTRATACION',
    ADD CONSTRAINT chk_empleado_adjunto_requerido_etapa CHECK (
        etapa_requerida IN ('REGISTRO_INICIAL', 'CONTRATACION')
    );

CREATE INDEX idx_empleado_adjunto_requerido_etapa
    ON rrhh.empleado_adjunto_requerido (id_empleado, etapa_requerida);

ALTER TABLE rrhh.empleado_adjunto
    ADD COLUMN publico_qr BOOLEAN NOT NULL DEFAULT FALSE;

CREATE FUNCTION rrhh.validar_adjunto_publico_qr()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    codigo_tipo VARCHAR(30);
BEGIN
    IF NOT NEW.publico_qr THEN
        RETURN NEW;
    END IF;

    SELECT tipo.codigo
    INTO codigo_tipo
    FROM rrhh.tipo_adjunto_empleado tipo
    WHERE tipo.id_tipo_adjunto_empleado = NEW.id_tipo_adjunto_empleado;

    IF codigo_tipo <> '3.1' THEN
        RAISE EXCEPTION 'Solo el certificado de ARL puede publicarse en la tarjeta QR'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_adjunto_publico_qr
    BEFORE INSERT OR UPDATE OF id_tipo_adjunto_empleado, publico_qr
    ON rrhh.empleado_adjunto
    FOR EACH ROW
    EXECUTE FUNCTION rrhh.validar_adjunto_publico_qr();

ALTER TABLE rrhh.contrato_empleado
    DROP CONSTRAINT chk_contrato_estado;

UPDATE rrhh.contrato_empleado
SET estado = 'TERMINADO'
WHERE estado = 'FINALIZADO';

ALTER TABLE rrhh.contrato_empleado
    ADD CONSTRAINT chk_contrato_estado CHECK (
        estado IN ('BORRADOR', 'PROGRAMADO', 'VIGENTE', 'VENCIDO', 'TERMINADO', 'ANULADO')
    ),
    ADD COLUMN terminado_por INT,
    ADD COLUMN terminado_en DATE,
    ADD COLUMN motivo_terminacion TEXT,
    ADD COLUMN anulado_por INT,
    ADD COLUMN anulado_en DATE,
    ADD COLUMN motivo_anulacion TEXT,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_contrato_terminado_por FOREIGN KEY (terminado_por)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT,
    ADD CONSTRAINT fk_contrato_anulado_por FOREIGN KEY (anulado_por)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT,
    ADD CONSTRAINT chk_contrato_terminacion_auditoria CHECK (
        (terminado_por IS NULL AND terminado_en IS NULL AND motivo_terminacion IS NULL)
        OR (
            terminado_por IS NOT NULL AND terminado_en IS NOT NULL
            AND motivo_terminacion IS NOT NULL
            AND length(btrim(motivo_terminacion)) > 0
        )
    ),
    ADD CONSTRAINT chk_contrato_anulacion_auditoria CHECK (
        (anulado_por IS NULL AND anulado_en IS NULL AND motivo_anulacion IS NULL)
        OR (
            anulado_por IS NOT NULL AND anulado_en IS NOT NULL
            AND motivo_anulacion IS NOT NULL
            AND length(btrim(motivo_anulacion)) > 0
        )
    ),
    ADD CONSTRAINT chk_contrato_estado_auditoria CHECK (
        (estado <> 'TERMINADO' OR terminado_por IS NOT NULL)
        AND (estado <> 'ANULADO' OR anulado_por IS NOT NULL)
    );

CREATE TABLE rrhh.contrato_empleado_historial_estado (
    id_contrato_historial_estado BIGSERIAL PRIMARY KEY,
    id_contrato_empleado INT NOT NULL,
    estado_anterior VARCHAR(20) NOT NULL,
    estado_nuevo VARCHAR(20) NOT NULL,
    id_usuario INT,
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    detalle VARCHAR(500) NOT NULL,
    CONSTRAINT chk_contrato_historial_estado_anterior CHECK (
        estado_anterior IN (
            'BORRADOR', 'PROGRAMADO', 'VIGENTE', 'VENCIDO', 'TERMINADO', 'ANULADO'
        )
    ),
    CONSTRAINT chk_contrato_historial_estado_nuevo CHECK (
        estado_nuevo IN (
            'BORRADOR', 'PROGRAMADO', 'VIGENTE', 'VENCIDO', 'TERMINADO', 'ANULADO'
        )
    ),
    CONSTRAINT fk_contrato_historial_estado_contrato FOREIGN KEY (id_contrato_empleado)
        REFERENCES rrhh.contrato_empleado (id_contrato_empleado) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_historial_estado_usuario FOREIGN KEY (id_usuario)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT
);

CREATE INDEX idx_contrato_historial_estado_contrato_fecha
    ON rrhh.contrato_empleado_historial_estado (id_contrato_empleado, fecha_cambio DESC);

CREATE TABLE rrhh.auditoria_envio_correo_empleado (
    id_auditoria_correo_empleado BIGSERIAL PRIMARY KEY,
    id_empleado INT NOT NULL,
    destinatario VARCHAR(150) NOT NULL,
    clave_evento VARCHAR(120) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (
        estado IN ('PENDIENTE', 'PROCESANDO', 'ENVIADO', 'REINTENTAR', 'FALLIDO')
    ),
    intentos INT NOT NULL DEFAULT 0 CHECK (intentos >= 0),
    proximo_intento TIMESTAMPTZ,
    bloqueado_en TIMESTAMPTZ,
    enviado_en TIMESTAMPTZ,
    creado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_error TEXT,
    CONSTRAINT uq_auditoria_correo_empleado_evento
        UNIQUE (id_empleado, destinatario, clave_evento),
    CONSTRAINT fk_auditoria_correo_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE CASCADE
);

CREATE INDEX idx_auditoria_correo_empleado_estado_reintento
    ON rrhh.auditoria_envio_correo_empleado (estado, proximo_intento);

CREATE TABLE rrhh.empleado_qr_token (
    id_empleado_qr_token BIGSERIAL PRIMARY KEY,
    id_empleado INT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    emitido_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    emitido_por INT NOT NULL,
    invalidado_en TIMESTAMPTZ,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_empleado_qr_token_hash CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT chk_empleado_qr_token_invalidacion CHECK (
        (activo AND invalidado_en IS NULL) OR (NOT activo AND invalidado_en IS NOT NULL)
    ),
    CONSTRAINT fk_empleado_qr_token_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE CASCADE,
    CONSTRAINT fk_empleado_qr_token_usuario FOREIGN KEY (emitido_por)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_empleado_qr_token_activo
    ON rrhh.empleado_qr_token (id_empleado)
    WHERE activo;

CREATE TABLE rrhh.empleado_foto (
    id_empleado INT PRIMARY KEY,
    nombre_original VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(150) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    contenido BYTEA NOT NULL,
    actualizado_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_empleado_foto_tamano CHECK (
        tamano_bytes > 0 AND octet_length(contenido) = tamano_bytes
    ),
    CONSTRAINT fk_empleado_foto_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE CASCADE
);

CREATE TABLE rrhh.herramienta (
    id_herramienta SERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.asignacion_herramienta (
    id_asignacion_herramienta BIGSERIAL PRIMARY KEY,
    id_herramienta INT NOT NULL,
    id_empleado INT NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    acta_nombre VARCHAR(255),
    acta_mime VARCHAR(150),
    acta_tamano_bytes BIGINT,
    acta_contenido BYTEA,
    entregada_en TIMESTAMPTZ,
    entregada_por INT,
    recibida_en TIMESTAMPTZ,
    recibida_por INT,
    CONSTRAINT chk_asignacion_herramienta_estado CHECK (
        estado IN ('PENDIENTE', 'ASIGNADA', 'RECIBIDO')
    ),
    CONSTRAINT chk_asignacion_herramienta_acta CHECK (
        (
            estado = 'PENDIENTE'
            AND acta_nombre IS NULL
            AND acta_mime IS NULL
            AND acta_tamano_bytes IS NULL
            AND acta_contenido IS NULL
            AND entregada_en IS NULL
            AND entregada_por IS NULL
        )
        OR
        (
            estado IN ('ASIGNADA', 'RECIBIDO')
            AND acta_nombre IS NOT NULL
            AND length(btrim(acta_nombre)) > 0
            AND acta_mime IS NOT NULL
            AND length(btrim(acta_mime)) > 0
            AND acta_tamano_bytes IS NOT NULL
            AND acta_tamano_bytes > 0
            AND acta_contenido IS NOT NULL
            AND octet_length(acta_contenido) = acta_tamano_bytes
            AND entregada_en IS NOT NULL
            AND entregada_por IS NOT NULL
        )
    ),
    CONSTRAINT chk_asignacion_herramienta_recepcion CHECK (
        (estado IN ('PENDIENTE', 'ASIGNADA')
            AND recibida_en IS NULL AND recibida_por IS NULL)
        OR (estado = 'RECIBIDO' AND recibida_en IS NOT NULL AND recibida_por IS NOT NULL)
    ),
    CONSTRAINT fk_asignacion_herramienta_herramienta FOREIGN KEY (id_herramienta)
        REFERENCES rrhh.herramienta (id_herramienta) ON DELETE RESTRICT,
    CONSTRAINT fk_asignacion_herramienta_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE RESTRICT,
    CONSTRAINT fk_asignacion_herramienta_entregada_por FOREIGN KEY (entregada_por)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT,
    CONSTRAINT fk_asignacion_herramienta_recibida_por FOREIGN KEY (recibida_por)
        REFERENCES core.usuario (id_usuario) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_asignacion_herramienta_activa
    ON rrhh.asignacion_herramienta (id_herramienta)
    WHERE estado = 'ASIGNADA';

CREATE UNIQUE INDEX uq_asignacion_herramienta_pendiente_empleado
    ON rrhh.asignacion_herramienta (id_empleado, id_herramienta)
    WHERE estado = 'PENDIENTE';

CREATE INDEX idx_asignacion_herramienta_empleado
    ON rrhh.asignacion_herramienta (id_empleado, entregada_en DESC);

INSERT INTO core.rol (codigo)
VALUES ('APROBADOR_RRHH'), ('GESTOR_RRHH')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO core.permiso (codigo, nombre) VALUES
    ('RRHH_VER', 'Consultar empleados y el directorio de RR. HH.'),
    ('RRHH_GESTIONAR', 'Crear empleados y completar contratación'),
    ('RRHH_REGISTRO_EDITAR', 'Editar datos del registro inicial'),
    ('RRHH_APROBAR', 'Aprobar el inicio del proceso de contratación'),
    ('RRHH_PERFIL_EDITAR', 'Editar el perfil de empleados activos'),
    ('RRHH_CONTRATO_TERMINAR', 'Terminar contratos de empleados'),
    ('RRHH_CONTRATO_ANULAR', 'Anular contratos inválidos'),
    ('RRHH_ASIGNACIONES_GESTIONAR', 'Gestionar herramientas y asignaciones'),
    ('RRHH_QR_GESTIONAR', 'Generar y renovar tarjetas QR de empleados')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO core.rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM core.rol rol
CROSS JOIN core.permiso permiso
WHERE rol.codigo IN ('SUPERADMINISTRADOR', 'ADMINISTRADOR')
  AND left(permiso.codigo, 5) = 'RRHH_'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO core.rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM core.rol rol
JOIN core.permiso permiso
    ON permiso.codigo IN ('RRHH_VER', 'RRHH_REGISTRO_EDITAR', 'RRHH_APROBAR')
WHERE rol.codigo = 'APROBADOR_RRHH'
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

INSERT INTO core.rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM core.rol rol
CROSS JOIN core.permiso permiso
WHERE rol.codigo = 'GESTOR_RRHH'
  AND permiso.codigo IN (
      'RRHH_VER', 'RRHH_GESTIONAR', 'RRHH_REGISTRO_EDITAR', 'RRHH_PERFIL_EDITAR',
      'RRHH_CONTRATO_TERMINAR', 'RRHH_ASIGNACIONES_GESTIONAR', 'RRHH_QR_GESTIONAR'
  )
ON CONFLICT (id_rol, id_permiso) DO NOTHING;

UPDATE core.rol
SET permisos_predeterminados_cargados = TRUE
WHERE codigo IN ('APROBADOR_RRHH', 'GESTOR_RRHH');
