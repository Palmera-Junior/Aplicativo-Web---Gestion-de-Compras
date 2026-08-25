package com.palmera_junior.gestion_compras.dto;

import java.math.BigDecimal;
import java.util.List;

public class RecibirOrdenDTO {

    private Integer idOrden;

    private String numeroFactura;

    private String recibidoPor;

    private String observacionRecepcion;

    private BigDecimal valorFlete;

    private String fotoRecepcion;

    private List<ProductoRecepcionDTO> productos;

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

    public BigDecimal getValorFlete() {
        return valorFlete;
    }

    public void setValorFlete(BigDecimal valorFlete) {
        this.valorFlete = valorFlete;
    }

    public String getFotoRecepcion() {
        return fotoRecepcion;
    }

    public void setFotoRecepcion(String fotoRecepcion) {
        this.fotoRecepcion = fotoRecepcion;
    }

    public List<ProductoRecepcionDTO> getProductos() {
        return productos;
    }

    public void setProductos(List<ProductoRecepcionDTO> productos) {
        this.productos = productos;
    }

    public static class ProductoRecepcionDTO {
        private Integer idDetalle;
        private Long idProducto;
        private String codigoInventario;
        private String presentacion;
        private String descripcion;
        private Integer cantidadSolicitada;
        private Integer cantidadRecibida;
        private Boolean recibido;

        public Integer getIdDetalle() {
            return idDetalle;
        }

        public void setIdDetalle(Integer idDetalle) {
            this.idDetalle = idDetalle;
        }

        public Long getIdProducto() {
            return idProducto;
        }

        public void setIdProducto(Long idProducto) {
            this.idProducto = idProducto;
        }

        public String getCodigoInventario() {
            return codigoInventario;
        }

        public void setCodigoInventario(String codigoInventario) {
            this.codigoInventario = codigoInventario;
        }

        public String getPresentacion() {
            return presentacion;
        }

        public void setPresentacion(String presentacion) {
            this.presentacion = presentacion;
        }

        public String getDescripcion() {
            return descripcion;
        }

        public void setDescripcion(String descripcion) {
            this.descripcion = descripcion;
        }

        public Integer getCantidadSolicitada() {
            return cantidadSolicitada;
        }

        public void setCantidadSolicitada(Integer cantidadSolicitada) {
            this.cantidadSolicitada = cantidadSolicitada;
        }

        public Integer getCantidadRecibida() {
            return cantidadRecibida;
        }

        public void setCantidadRecibida(Integer cantidadRecibida) {
            this.cantidadRecibida = cantidadRecibida;
        }

        public Boolean getRecibido() {
            return recibido;
        }

        public void setRecibido(Boolean recibido) {
            this.recibido = recibido;
        }
    }
}
