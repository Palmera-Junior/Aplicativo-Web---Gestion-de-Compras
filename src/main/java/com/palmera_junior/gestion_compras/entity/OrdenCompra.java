package com.palmera_junior.gestion_compras.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "orden_compra")

public class OrdenCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Integer idOrden;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "id_prov", nullable = true)
    @ToString.Exclude
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "numero_orden", length = 20, unique = true)
    private String numeroOrden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoOrdenCompra estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede", nullable = false)
    @ToString.Exclude
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_centro_costo")
    @ToString.Exclude
    private CentroCosto centroCosto;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "sub_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "iva_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal ivaTotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuento;

    @Column(name = "paga_flete", nullable = false)
    private Boolean pagaFlete = false;

    @Column(name = "valor_flete", precision = 10, scale = 2)
    private BigDecimal valorFlete;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_aprobacion")
    @ToString.Exclude
    private Usuario usuarioAprobacion;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    @Column(name = "numero_factura", length = 100)
    private String numeroFactura;

    @Column(name = "recibido_por", length = 150)
    private String recibidoPor;

    @Column(name = "fecha_recepcion")
    private LocalDate fechaRecepcion;

    @Column(name = "observacion_recepcion")
    private String observacionRecepcion;

    @Column(name = "foto_recepcion", columnDefinition = "TEXT")
    private String fotoRecepcion;

    @Column(name = "foto_factura", columnDefinition = "TEXT")
    private String fotoFactura;

    @Column(name = "se_recibio", nullable = false)
    private Boolean seRecibio = false;

    @Column(name = "se_facturo", nullable = false)
    private Boolean seFacturo = false;

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<DetalleCompra> detalles = new ArrayList<>();

    public void addDetalle(DetalleCompra detalle) {
        detalles.add(detalle);
        detalle.setOrdenCompra(this);
    }

    public void removeDetalle(DetalleCompra detalle) {
        detalles.remove(detalle);
        detalle.setOrdenCompra(null);

    }

    public void marcarRecibida(String recibidoPor, LocalDate fechaRecepcion, String observacion) {
        this.recibidoPor = recibidoPor;
        this.fechaRecepcion = fechaRecepcion;
        this.observacionRecepcion = observacion;
        this.seRecibio = true;
        this.estado = EstadoOrdenCompra.RECIBIDA;
        verificarCompletada();
    }

    public void marcarFacturada(String numeroFactura) {
        this.numeroFactura = numeroFactura;
        this.seFacturo = true;
        this.estado = EstadoOrdenCompra.FACTURADA;
        verificarCompletada();
    }

    private void verificarCompletada() {
        if (Boolean.TRUE.equals(this.seRecibio) && Boolean.TRUE.equals(this.seFacturo)) {
            this.estado = EstadoOrdenCompra.COMPLETADA;
        }
    }

    public void anular() {
        if (this.estado == EstadoOrdenCompra.BORRADOR || this.estado == EstadoOrdenCompra.APROBADA) {
            this.estado = EstadoOrdenCompra.ANULADA;
        } else {
            throw new IllegalStateException("No es posible anular una orden en estado: " + this.estado);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof OrdenCompra))
            return false;
        OrdenCompra other = (OrdenCompra) o;
        return idOrden != null && idOrden.equals(other.getIdOrden());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
