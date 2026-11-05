package com.palmera_junior.gestion_compras.rrhh.entity;

import java.time.LocalDate;

public enum EstadoContratoEmpleado {
    BORRADOR,
    PROGRAMADO,
    VIGENTE,
    VENCIDO,
    TERMINADO,
    ANULADO;

    public static EstadoContratoEmpleado calcular(
            LocalDate fechaIngreso, LocalDate fechaFin, LocalDate fechaActual) {
        if (fechaIngreso == null || fechaActual == null) {
            throw new IllegalArgumentException("La fecha de ingreso y la fecha actual son obligatorias");
        }
        if (fechaIngreso.isAfter(fechaActual)) {
            return PROGRAMADO;
        }
        if (fechaFin != null && fechaFin.isBefore(fechaActual)) {
            return VENCIDO;
        }
        return VIGENTE;
    }
}
