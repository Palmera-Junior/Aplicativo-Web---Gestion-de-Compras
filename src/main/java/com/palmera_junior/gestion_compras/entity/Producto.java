package com.palmera_junior.gestion_compras.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "producto")

public class Producto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "codigo_inventario",nullable = false, length = 50, unique = true)
    private String codigoInventario;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 100)
    private String presentacion;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Override
    public boolean equals(Object o) {
        if(this== o) return true;
        if(!(o instanceof Producto)) return false;
        Producto other = (Producto) o;
        return idProducto != null && idProducto.equals(other.getIdProducto());
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
