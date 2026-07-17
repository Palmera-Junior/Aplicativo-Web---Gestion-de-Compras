package com.palmera_junior.gestion_compras.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "detalle_compra")

public class DetalleCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle")
    private Integer idDetalle;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "codigo_inventario", nullable = false, length = 50)
    private String codigoInventario;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false, length = 150)
    private String presentacion;

    @Column(name = "valor_unitario" , nullable = false, precision = 10, scale = 2 )
    private BigDecimal valorUnitario;

    @Column(name ="iva_producto", nullable = false, precision = 10, scale = 2)
    private BigDecimal ivaProducto;

    @Column(name = "valor_iva", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorIva;

    @Column(name = "valor_total_linea", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotalLinea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    @ToString.Exclude
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = false)
    @ToString.Exclude
    private OrdenCompra ordenCompra;

    @Override
    public boolean equals(Object o) {
        if(this== o) return true;
        if(!(o instanceof DetalleCompra)) return false;
        DetalleCompra other = (DetalleCompra) o;
        return idDetalle != null && idDetalle.equals(other.getIdDetalle());
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }







    

    
    
}
