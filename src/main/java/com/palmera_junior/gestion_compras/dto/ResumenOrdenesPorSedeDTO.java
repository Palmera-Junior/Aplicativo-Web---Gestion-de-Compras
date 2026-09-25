package com.palmera_junior.gestion_compras.dto;

import java.math.BigDecimal;

/** Datos de una sede para la distribución visual de órdenes de compra. */
public record ResumenOrdenesPorSedeDTO(
        String nombre,
        BigDecimal total,
        double porcentaje,
        double porcentajeBarra,
        double inicio,
        double longitud,
        String color) {
}
