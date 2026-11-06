CREATE SCHEMA IF NOT EXISTS rrhh;

CREATE TABLE rrhh.area (
    id_area SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.cargo (
    id_cargo SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.nivel_educativo (
    id_nivel_educativo SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.tipo_contrato (
    id_tipo_contrato SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.tipo_pago (
    id_tipo_pago SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.entidad_bancaria (
    id_entidad_bancaria SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.eps (
    id_eps SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.afp (
    id_afp SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.arl (
    id_arl SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE rrhh.caja_compensacion (
    id_caja_compensacion SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE core.empleado (
    id_empleado SERIAL PRIMARY KEY,
    tipo_documento VARCHAR(30) NOT NULL,
    numero_documento VARCHAR(30) NOT NULL,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    id_cargo INT,
    id_area INT,
    id_centro_costo INT,
    subcentro VARCHAR(100),
    fecha_expedicion DATE,
    lugar_expedicion VARCHAR(150),
    fecha_nacimiento DATE,
    lugar_nacimiento VARCHAR(150),
    tipo_sangre VARCHAR(2),
    rh VARCHAR(10),
    direccion_residencia VARCHAR(255),
    barrio VARCHAR(100),
    celular VARCHAR(30),
    correo_electronico VARCHAR(150),
    municipio VARCHAR(100),
    departamento VARCHAR(100),
    id_nivel_educativo INT,
    profesion VARCHAR(150),
    CONSTRAINT uq_empleado_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT chk_empleado_tipo_documento CHECK (
        tipo_documento IN (
            'CEDULA_CIUDADANIA',
            'CEDULA_EXTRANJERIA',
            'TARJETA_IDENTIDAD',
            'REGISTRO_CIVIL',
            'PASAPORTE',
            'PERMISO_PROTECCION_TEMPORAL'
        )
    ),
    CONSTRAINT chk_empleado_tipo_sangre CHECK (
        tipo_sangre IS NULL OR tipo_sangre IN ('A', 'B', 'AB', 'O')
    ),
    CONSTRAINT chk_empleado_rh CHECK (
        rh IS NULL OR rh IN ('POSITIVO', 'NEGATIVO')
    ),
    CONSTRAINT fk_empleado_cargo FOREIGN KEY (id_cargo)
        REFERENCES rrhh.cargo (id_cargo) ON DELETE RESTRICT,
    CONSTRAINT fk_empleado_area FOREIGN KEY (id_area)
        REFERENCES rrhh.area (id_area) ON DELETE RESTRICT,
    CONSTRAINT fk_empleado_centro_costo FOREIGN KEY (id_centro_costo)
        REFERENCES core.centro_costo (id_centro_costo) ON DELETE RESTRICT,
    CONSTRAINT fk_empleado_nivel_educativo FOREIGN KEY (id_nivel_educativo)
        REFERENCES rrhh.nivel_educativo (id_nivel_educativo) ON DELETE RESTRICT
);

CREATE INDEX idx_empleado_cargo ON core.empleado (id_cargo);
CREATE INDEX idx_empleado_area ON core.empleado (id_area);
CREATE INDEX idx_empleado_centro_costo ON core.empleado (id_centro_costo);
CREATE INDEX idx_empleado_nivel_educativo ON core.empleado (id_nivel_educativo);
CREATE UNIQUE INDEX uq_empleado_correo_normalizado
    ON core.empleado (lower(btrim(correo_electronico)))
    WHERE correo_electronico IS NOT NULL AND btrim(correo_electronico) <> '';

ALTER TABLE core.usuario
    ADD COLUMN id_empleado INT,
    ADD CONSTRAINT uq_usuario_empleado UNIQUE (id_empleado),
    ADD CONSTRAINT fk_usuario_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE RESTRICT;

CREATE TABLE rrhh.contrato_empleado (
    id_contrato_empleado SERIAL PRIMARY KEY,
    id_empleado INT NOT NULL,
    id_tipo_contrato INT,
    id_tipo_pago INT,
    id_entidad_bancaria INT,
    numero_cuenta_bancaria VARCHAR(50),
    fecha_ingreso DATE NOT NULL,
    fecha_fin DATE,
    salario_basico NUMERIC(10, 2) NOT NULL,
    auxilio_transporte_aplica BOOLEAN NOT NULL DEFAULT FALSE,
    auxilio_transporte_valor NUMERIC(10, 2),
    auxilio_rodamiento_aplica BOOLEAN NOT NULL DEFAULT FALSE,
    auxilio_rodamiento_valor NUMERIC(10, 2),
    bonificacion_no_salarial_aplica BOOLEAN NOT NULL DEFAULT FALSE,
    bonificacion_no_salarial_valor NUMERIC(10, 2),
    condiciones_especiales_aplica BOOLEAN NOT NULL DEFAULT FALSE,
    condiciones_especiales TEXT,
    id_eps INT,
    id_afp INT,
    id_arl INT,
    tarifa_arl_porcentaje NUMERIC(6, 3),
    id_caja_compensacion INT,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    CONSTRAINT chk_contrato_fechas CHECK (
        fecha_fin IS NULL OR fecha_fin >= fecha_ingreso
    ),
    CONSTRAINT chk_contrato_salario_no_negativo CHECK (salario_basico >= 0),
    CONSTRAINT chk_contrato_auxilio_transporte CHECK (
        (auxilio_transporte_aplica AND auxilio_transporte_valor IS NOT NULL
            AND auxilio_transporte_valor >= 0)
        OR (NOT auxilio_transporte_aplica AND auxilio_transporte_valor IS NULL)
    ),
    CONSTRAINT chk_contrato_auxilio_rodamiento CHECK (
        (auxilio_rodamiento_aplica AND auxilio_rodamiento_valor IS NOT NULL
            AND auxilio_rodamiento_valor >= 0)
        OR (NOT auxilio_rodamiento_aplica AND auxilio_rodamiento_valor IS NULL)
    ),
    CONSTRAINT chk_contrato_bonificacion_no_salarial CHECK (
        (bonificacion_no_salarial_aplica AND bonificacion_no_salarial_valor IS NOT NULL
            AND bonificacion_no_salarial_valor >= 0)
        OR (NOT bonificacion_no_salarial_aplica AND bonificacion_no_salarial_valor IS NULL)
    ),
    CONSTRAINT chk_contrato_condiciones_especiales CHECK (
        (condiciones_especiales_aplica AND condiciones_especiales IS NOT NULL
            AND length(btrim(condiciones_especiales)) > 0)
        OR (NOT condiciones_especiales_aplica AND condiciones_especiales IS NULL)
    ),
    CONSTRAINT chk_contrato_tarifa_arl CHECK (
        tarifa_arl_porcentaje IS NULL
        OR tarifa_arl_porcentaje BETWEEN 0 AND 100
    ),
    CONSTRAINT chk_contrato_estado CHECK (
        estado IN ('BORRADOR', 'VIGENTE', 'FINALIZADO', 'ANULADO')
    ),
    CONSTRAINT chk_contrato_afiliaciones_vigente CHECK (
        estado <> 'VIGENTE'
        OR (
            id_eps IS NOT NULL
            AND id_afp IS NOT NULL
            AND id_arl IS NOT NULL
            AND id_caja_compensacion IS NOT NULL
        )
    ),
    CONSTRAINT fk_contrato_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_tipo FOREIGN KEY (id_tipo_contrato)
        REFERENCES rrhh.tipo_contrato (id_tipo_contrato) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_tipo_pago FOREIGN KEY (id_tipo_pago)
        REFERENCES rrhh.tipo_pago (id_tipo_pago) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_entidad_bancaria FOREIGN KEY (id_entidad_bancaria)
        REFERENCES rrhh.entidad_bancaria (id_entidad_bancaria) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_eps FOREIGN KEY (id_eps)
        REFERENCES rrhh.eps (id_eps) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_afp FOREIGN KEY (id_afp)
        REFERENCES rrhh.afp (id_afp) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_arl FOREIGN KEY (id_arl)
        REFERENCES rrhh.arl (id_arl) ON DELETE RESTRICT,
    CONSTRAINT fk_contrato_caja_compensacion FOREIGN KEY (id_caja_compensacion)
        REFERENCES rrhh.caja_compensacion (id_caja_compensacion) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_contrato_empleado_vigente
    ON rrhh.contrato_empleado (id_empleado)
    WHERE estado = 'VIGENTE';

CREATE INDEX idx_contrato_empleado_historial
    ON rrhh.contrato_empleado (id_empleado, fecha_ingreso DESC);

ALTER TABLE rrhh.contrato_empleado
    ADD CONSTRAINT uq_contrato_empleado_id_empleado
        UNIQUE (id_contrato_empleado, id_empleado);

CREATE TABLE rrhh.tipo_adjunto_empleado (
    id_tipo_adjunto_empleado SERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descripcion VARCHAR(255) NOT NULL,
    grupo VARCHAR(20) NOT NULL,
    orden SMALLINT NOT NULL UNIQUE,
    permite_multiples_vigentes BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_tipo_adjunto_empleado_grupo CHECK (
        grupo IN ('INGRESO', 'VEHICULO', 'AFILIACIONES', 'EVALUACIONES', 'RETIRO')
    )
);

INSERT INTO rrhh.tipo_adjunto_empleado
    (codigo, descripcion, grupo, orden, permite_multiples_vigentes)
VALUES
    ('1.1', 'F4-PRH01 Ficha de ingreso', 'INGRESO', 1, FALSE),
    ('1.2', 'Hoja de vida', 'INGRESO', 2, FALSE),
    ('1.3', 'Fotocopia de cédula', 'INGRESO', 3, FALSE),
    ('1.4', 'Certificaciones laborales que sumen la experiencia del perfil', 'INGRESO', 4, TRUE),
    ('1.5', 'Certificados de estudio', 'INGRESO', 5, TRUE),
    ('1.6', 'Tarjeta profesional', 'INGRESO', 6, FALSE),
    ('1.7', 'Registro de consulta de antecedentes judiciales', 'INGRESO', 7, FALSE),
    ('1.8', 'Exámenes de ingreso', 'INGRESO', 8, FALSE),
    ('1.9', 'Certificado bancario - cuenta de nómina', 'INGRESO', 9, FALSE),
    ('2.1', 'Licencia de conducción', 'VEHICULO', 10, FALSE),
    ('2.2', 'Seguro Obligatorio SOAT', 'VEHICULO', 11, FALSE),
    ('2.3', 'Certificado de revisión técnico-mecánica', 'VEHICULO', 12, FALSE),
    ('2.4', 'Licencia de tránsito de la motocicleta', 'VEHICULO', 13, FALSE),
    ('2.5', 'Registro de multas (SIMIT)', 'VEHICULO', 14, FALSE),
    ('2.6', 'Declaración extrajuicio: moto libre de requerimientos judiciales', 'VEHICULO', 15, FALSE),
    ('2.7', 'Autorización de uso cuando el propietario no es el candidato', 'VEHICULO', 16, FALSE),
    ('2.8', 'Pruebas psicométricas, teóricas y prácticas', 'VEHICULO', 17, TRUE),
    ('3.1', 'Afiliación ARL', 'AFILIACIONES', 18, FALSE),
    ('3.2', 'Certificado EPS', 'AFILIACIONES', 19, FALSE),
    ('3.3', 'Afiliación EPS', 'AFILIACIONES', 20, FALSE),
    ('3.4', 'Certificado de pensión', 'AFILIACIONES', 21, FALSE),
    ('3.5', 'Afiliación a pensión si es nuevo en el sistema', 'AFILIACIONES', 22, FALSE),
    ('3.6', 'Afiliación a caja de compensación', 'AFILIACIONES', 23, FALSE),
    ('3.7', 'Documentos para afiliación del núcleo familiar (RH-F-03 Beneficiarios afiliaciones EPS CCF)', 'AFILIACIONES', 24, TRUE),
    ('3.8', 'Contrato de trabajo', 'AFILIACIONES', 25, FALSE),
    ('4.1', 'RH-F-01 Inducción de personal', 'EVALUACIONES', 26, FALSE),
    ('4.2', 'RH-F-06 Evaluación de personal nuevo', 'EVALUACIONES', 27, TRUE),
    ('4.3', 'RH-F-07 Evaluación de la inducción', 'EVALUACIONES', 28, TRUE),
    ('4.4', 'RH-F-08 Evaluación de desempeño', 'EVALUACIONES', 29, TRUE),
    ('4.5', 'RH-F-11 Evaluación de habilidades', 'EVALUACIONES', 30, TRUE),
    ('5.1', 'Liquidación de prestaciones sociales', 'RETIRO', 31, FALSE),
    ('5.2', 'Documentos de retiro', 'RETIRO', 32, TRUE),
    ('5.3', 'RH-F-14 Orden examen de egreso', 'RETIRO', 33, FALSE),
    ('5.4', 'RH-F-04 Paz y salvo', 'RETIRO', 34, FALSE);

CREATE TABLE rrhh.empleado_adjunto_requerido (
    id_empleado INT NOT NULL,
    id_tipo_adjunto_empleado INT NOT NULL,
    PRIMARY KEY (id_empleado, id_tipo_adjunto_empleado),
    CONSTRAINT fk_empleado_adjunto_requerido_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE CASCADE,
    CONSTRAINT fk_empleado_adjunto_requerido_tipo FOREIGN KEY (id_tipo_adjunto_empleado)
        REFERENCES rrhh.tipo_adjunto_empleado (id_tipo_adjunto_empleado) ON DELETE RESTRICT
);

CREATE INDEX idx_empleado_adjunto_requerido_tipo
    ON rrhh.empleado_adjunto_requerido (id_tipo_adjunto_empleado);

CREATE TABLE rrhh.empleado_adjunto (
    id_empleado_adjunto BIGSERIAL PRIMARY KEY,
    id_empleado INT NOT NULL,
    id_tipo_adjunto_empleado INT NOT NULL,
    id_contrato_empleado INT,
    nombre_original VARCHAR(255) NOT NULL,
    tipo_mime VARCHAR(150) NOT NULL,
    tamano_bytes BIGINT NOT NULL,
    contenido BYTEA NOT NULL,
    fecha_carga TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    es_vigente BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_empleado_adjunto_nombre CHECK (length(btrim(nombre_original)) > 0),
    CONSTRAINT chk_empleado_adjunto_mime CHECK (length(btrim(tipo_mime)) > 0),
    CONSTRAINT chk_empleado_adjunto_tamano CHECK (
        tamano_bytes > 0 AND octet_length(contenido) = tamano_bytes
    ),
    CONSTRAINT fk_empleado_adjunto_empleado FOREIGN KEY (id_empleado)
        REFERENCES core.empleado (id_empleado) ON DELETE CASCADE,
    CONSTRAINT fk_empleado_adjunto_tipo FOREIGN KEY (id_tipo_adjunto_empleado)
        REFERENCES rrhh.tipo_adjunto_empleado (id_tipo_adjunto_empleado) ON DELETE RESTRICT,
    CONSTRAINT fk_empleado_adjunto_contrato_empleado FOREIGN KEY (
        id_contrato_empleado,
        id_empleado
    ) REFERENCES rrhh.contrato_empleado (
        id_contrato_empleado,
        id_empleado
    ) ON DELETE RESTRICT
);

CREATE INDEX idx_empleado_adjunto_tipo_vigente
    ON rrhh.empleado_adjunto (id_empleado, id_tipo_adjunto_empleado, es_vigente);

CREATE INDEX idx_empleado_adjunto_contrato
    ON rrhh.empleado_adjunto (id_contrato_empleado)
    WHERE id_contrato_empleado IS NOT NULL;

CREATE FUNCTION rrhh.validar_multiplicidad_adjunto_empleado()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    permite_multiples BOOLEAN;
BEGIN
    IF NOT NEW.es_vigente THEN
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(
        hashtextextended(NEW.id_empleado::TEXT || ':' || NEW.id_tipo_adjunto_empleado::TEXT, 0)
    );

    SELECT tipo.permite_multiples_vigentes
    INTO permite_multiples
    FROM rrhh.tipo_adjunto_empleado tipo
    WHERE tipo.id_tipo_adjunto_empleado = NEW.id_tipo_adjunto_empleado;

    IF NOT permite_multiples AND EXISTS (
        SELECT 1
        FROM rrhh.empleado_adjunto adjunto
        WHERE adjunto.id_empleado = NEW.id_empleado
          AND adjunto.id_tipo_adjunto_empleado = NEW.id_tipo_adjunto_empleado
          AND adjunto.es_vigente
          AND adjunto.id_empleado_adjunto <> COALESCE(NEW.id_empleado_adjunto, -1)
    ) THEN
        RAISE EXCEPTION 'Solo se permite un adjunto vigente de este tipo por empleado'
            USING ERRCODE = '23505';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validar_multiplicidad_adjunto_empleado
    BEFORE INSERT OR UPDATE OF id_empleado, id_tipo_adjunto_empleado, es_vigente
    ON rrhh.empleado_adjunto
    FOR EACH ROW
    EXECUTE FUNCTION rrhh.validar_multiplicidad_adjunto_empleado();
