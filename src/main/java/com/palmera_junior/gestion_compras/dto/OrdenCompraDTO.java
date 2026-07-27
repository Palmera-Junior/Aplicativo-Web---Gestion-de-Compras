package com.palmera_junior.gestion_compras.dto;

import java.math.BigDecimal;
import java.util.List;

public class OrdenCompraDTO {

    // Claves foráneas (opcionales si vienen del select del form)
    private Long idProv;
    private Long idCentroCosto;

    // Datos del proveedor y orden (Snapshot)
    private String fecha;          // Fecha seleccionada por el usuario (formato YYYY-MM-DD desde el frontend)
    private String nitProv;
    private String nombreProv;
    private String telefonoProv;
    private String ciudadProv;
    private String correoProv;
    private String direccionProv;
    private String observaciones;

    // Totales
    private BigDecimal descuento;
    private BigDecimal subTotal;
    private BigDecimal ivaTotal;
    private BigDecimal total;

    // Relación con el detalle
    private List<DetalleCompraDTO> detalles;

    // Getters y Setters
    public Long getIdProv() { return idProv; }
    public void setIdProv(Long idProv) { this.idProv = idProv; }

    public Long getIdCentroCosto() { return idCentroCosto; }
    public void setIdCentroCosto(Long idCentroCosto) { this.idCentroCosto = idCentroCosto; }

    public String getFecha() { return fecha; }
    public void setFecha(String fecha) { this.fecha = fecha; }


    public String getNitProv() { return nitProv; }
    public void setNitProv(String nitProv) { this.nitProv = nitProv; }

    public String getNombreProv() { return nombreProv; }
    public void setNombreProv(String nombreProv) { this.nombreProv = nombreProv; }

    public String getTelefonoProv() { return telefonoProv; }
    public void setTelefonoProv(String telefonoProv) { this.telefonoProv = telefonoProv; }

    public String getCiudadProv() { return ciudadProv; }
    public void setCiudadProv(String ciudadProv) { this.ciudadProv = ciudadProv; }

    public String getCorreoProv() { return correoProv; }
    public void setCorreoProv(String correoProv) { this.correoProv = correoProv; }

    public String getDireccionProv() { return direccionProv; }
    public void setDireccionProv(String direccionProv) { this.direccionProv = direccionProv; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public BigDecimal getDescuento() { return descuento; }
    public void setDescuento(BigDecimal descuento) { this.descuento = descuento; }

    public BigDecimal getSubTotal() { return subTotal; }
    public void setSubTotal(BigDecimal subTotal) { this.subTotal = subTotal; }

    public BigDecimal getIvaTotal() { return ivaTotal; }
    public void setIvaTotal(BigDecimal ivaTotal) { this.ivaTotal = ivaTotal; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public List<DetalleCompraDTO> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCompraDTO> detalles) { this.detalles = detalles; }
}