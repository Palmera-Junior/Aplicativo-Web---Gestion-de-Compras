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

    @ManyToOne(fetch = FetchType.LAZY , optional=true)
    @JoinColumn(name = "id_prov", nullable = true)
    @ToString.Exclude
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "numero_orden", length = 20, unique = true)
    private String numeroOrden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede", nullable = false)
    @ToString.Exclude
    private Sede sede;

    @Column(name = "nombre_prov", nullable = false, length = 150)
    private String nombreProv;

    @Column(name = "nit_prov", nullable = false, length = 50)
    private String nitProv;

    @Column(name = "direccion_prov", nullable = false, length = 255)
    private String direccionProv;

    @Column(name = "ciudad_prov", nullable = false, length = 150)
    private String ciudadProv;

    @Column(name = "telefono_prov", nullable = false, length = 50)
    private String telefonoProv;

    @Column(name = "correo_prov", nullable = false, length = 150)
    private String correoProv;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "sub_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal subTotal; 

    @Column(name = "iva_total" , nullable = false, precision = 10, scale = 2)
    private BigDecimal ivaTotal;

    @Column(precision = 10, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @OneToMany(mappedBy = "ordenCompra", cascade = CascadeType.ALL, orphanRemoval = true   )
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

    @Override
    public boolean equals(Object o) {
        if(this== o) return true;
        if(!(o instanceof OrdenCompra)) return false;
        OrdenCompra other = (OrdenCompra) o;
        return idOrden != null && idOrden.equals(other.getIdOrden());
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }













    
}
