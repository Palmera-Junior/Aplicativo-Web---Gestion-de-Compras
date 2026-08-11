package com.palmera_junior.gestion_compras.entity;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Categoria categoria;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PresentacionProducto> presentaciones = new ArrayList<>();

    public void addPresentacion(PresentacionProducto presentacion) {
        presentaciones.add(presentacion);
        presentacion.setProducto(this);
    }

    public void removePresentacion(PresentacionProducto presentacion) {
        presentaciones.remove(presentacion);
        presentacion.setProducto(null);
    }

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

