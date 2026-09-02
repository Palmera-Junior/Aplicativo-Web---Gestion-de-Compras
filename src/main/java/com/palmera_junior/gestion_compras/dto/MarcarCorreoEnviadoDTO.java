package com.palmera_junior.gestion_compras.dto;

import com.palmera_junior.gestion_compras.entity.TipoEnvioCorreo;

public class MarcarCorreoEnviadoDTO {

    private String descripcion;

    // Distingue cuál de los dos correos de la orden (proveedor o facturación) se está marcando
    private TipoEnvioCorreo tipoEnvio;

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoEnvioCorreo getTipoEnvio() {
        return tipoEnvio;
    }

    public void setTipoEnvio(TipoEnvioCorreo tipoEnvio) {
        this.tipoEnvio = tipoEnvio;
    }
}
