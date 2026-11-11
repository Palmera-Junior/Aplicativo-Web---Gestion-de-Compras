package com.palmera_junior.gestion_compras.rrhh.service;

import java.math.BigDecimal;
import java.time.LocalDate;

final class ValidacionDatosEmpleado {

    private static final String CORREO_VALIDO =
            "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$";

    private ValidacionDatosEmpleado() {
    }

    static void validarSoloDigitos(String valor, String campo) {
        if (valor != null && !valor.isBlank() && !valor.matches("[0-9]+")) {
            throw new IllegalArgumentException(campo + " solo puede contener números");
        }
    }

    static void validarCorreo(String correo) {
        if (correo != null && !correo.isBlank() && !correo.trim().matches(CORREO_VALIDO)) {
            throw new IllegalArgumentException("El correo electrónico no tiene un formato válido");
        }
    }

    static void validarFechasIdentificacion(LocalDate fechaExpedicion, LocalDate fechaNacimiento) {
        LocalDate hoy = LocalDate.now();
        if (fechaExpedicion != null && fechaExpedicion.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de expedición no puede ser futura");
        }
        if (fechaNacimiento != null && fechaNacimiento.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
        if (fechaNacimiento != null) {
            LocalDate referenciaEdad = fechaExpedicion == null ? hoy : fechaExpedicion;
            if (fechaNacimiento.isAfter(referenciaEdad.minusYears(18))) {
                throw new IllegalArgumentException(
                        "La persona debe tener al menos 18 años en la fecha de expedición");
            }
        }
    }

    static void validarFechaIngresoNoPasada(LocalDate fechaIngreso) {
        if (fechaIngreso != null && fechaIngreso.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de ingreso no puede ser anterior a hoy");
        }
    }

    static void validarFechaFinPosterior(LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio != null && fechaFin != null && !fechaFin.isAfter(fechaInicio)) {
            throw new IllegalArgumentException(
                    "La fecha de finalización debe ser posterior a la fecha de inicio");
        }
    }

    static void validarNoNegativo(String concepto, BigDecimal valor) {
        if (valor != null && valor.signum() < 0) {
            throw new IllegalArgumentException(concepto + " no puede ser negativo");
        }
    }
}
