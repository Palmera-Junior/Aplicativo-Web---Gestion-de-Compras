package com.palmera_junior.gestion_compras.dto;

import java.math.BigDecimal;

public class DetalleCompraDTO {
    
    private Long idProducto; // FK a producto (opcional si es nuevo)
    
    // Datos snapshot del producto (como están en la tabla)
    private String codigoInventario;
    private String presentacion;
    private String descripcion;
    
    // Valores numéricos del detalle
    private Integer cantidad;
    private BigDecimal valorUnitario;
    private BigDecimal ivaProducto; // Porcentaje del IVA (ej: 19.00)
    private BigDecimal valorIva;    // Valor monetario calculado del IVA
    private BigDecimal valorTotalLinea;

    // Getters y Setters
    public Long getIdProducto() { return idProducto; }
    public void setIdProducto(Long idProducto) { this.idProducto = idProducto; }

    public String getCodigoInventario() { return codigoInventario; }
    public void setCodigoInventario(String codigoInventario) { this.codigoInventario = codigoInventario; }

    public String getPresentacion() { return presentacion; }
    public void setPresentacion(String presentacion) { this.presentacion = presentacion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }

    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }

    public BigDecimal getIvaProducto() { return ivaProducto; }
    public void setIvaProducto(BigDecimal ivaProducto) { this.ivaProducto = ivaProducto; }

    public BigDecimal getValorIva() { return valorIva; }
    public void setValorIva(BigDecimal valorIva) { this.valorIva = valorIva; }

    public BigDecimal getValorTotalLinea() { return valorTotalLinea; }
    public void setValorTotalLinea(BigDecimal valorTotalLinea) { this.valorTotalLinea = valorTotalLinea; }
}