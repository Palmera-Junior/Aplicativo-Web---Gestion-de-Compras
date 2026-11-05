package com.palmera_junior.gestion_compras.rrhh.entity;

public enum EstadoEmpleado {
    REGISTRO_INICIAL("Registro inicial"),
    PROCESO_CONTRATACION("Proceso de contratación"),
    ACTIVO("Activo"),
    RETIRADO("Retirado");

    private final String etiqueta;

    EstadoEmpleado(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
