package com.palmera_junior.gestion_compras.entity;

public enum EstadoOrdenCompra {

    BORRADOR,
    APROBADA,
    RECIBIDA,
    FACTURADA,
    COMPLETADA,
    ANULADA;

    public boolean equalsIgnoreCase(String other) {
        return this.name().equalsIgnoreCase(other);
    }

}