package com.palmera_junior.gestion_compras.dto;

public class FacturarOrdenDTO {

    private String numeroFactura;
    private String fotoFactura;
    private String fotoRecepcion;

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public void setNumeroFactura(String numeroFactura) {
        this.numeroFactura = numeroFactura;
    }

    public String getFotoFactura() {
        return fotoFactura;
    }

    public void setFotoFactura(String fotoFactura) {
        this.fotoFactura = fotoFactura;
    }

    public String getFotoRecepcion() {
        return fotoRecepcion;
    }

    public void setFotoRecepcion(String fotoRecepcion) {
        this.fotoRecepcion = fotoRecepcion;
    }
}
