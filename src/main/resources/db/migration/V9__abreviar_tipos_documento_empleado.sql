ALTER TABLE core.empleado
    DROP CONSTRAINT chk_empleado_tipo_documento;

UPDATE core.empleado
SET tipo_documento = CASE tipo_documento
    WHEN 'CEDULA_CIUDADANIA' THEN 'CC'
    WHEN 'CEDULA_EXTRANJERIA' THEN 'CE'
    WHEN 'TARJETA_IDENTIDAD' THEN 'TI'
    WHEN 'REGISTRO_CIVIL' THEN 'RC'
    WHEN 'PASAPORTE' THEN 'P'
    WHEN 'PERMISO_PROTECCION_TEMPORAL' THEN 'PPT'
    ELSE tipo_documento
END;

ALTER TABLE core.empleado
    ADD CONSTRAINT chk_empleado_tipo_documento CHECK (
        tipo_documento IN ('CC', 'CE', 'TI', 'RC', 'P', 'PPT')
    );
