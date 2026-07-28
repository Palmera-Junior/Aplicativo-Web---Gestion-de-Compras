package com.palmera_junior.gestion_compras.dto;

public class RecibirOrdenDTO {

    private Integer idOrden;

    private String numeroFactura;

    private String recibidoPor;

    private String observacionRecepcion;

    public Integer getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(Integer idOrden) {
        this.idOrden = idOrden;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public String getRecibidoPor() {
        return recibidoPor;
    }

    public void setRecibidoPor(String recibidoPor) {
        this.recibidoPor = recibidoPor;
    }

    public String getObservacionRecepcion() {
        return observacionRecepcion;
    }

    public void setObservacionRecepcion(String observacionRecepcion) {
        this.observacionRecepcion = observacionRecepcion;
    }
}