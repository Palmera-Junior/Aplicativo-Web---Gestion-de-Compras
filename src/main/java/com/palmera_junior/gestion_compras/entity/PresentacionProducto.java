package com.palmera_junior.gestion_compras.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "presentacion_producto")
public class PresentacionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_presentacion")
    private Integer idPresentacion;

    @Column(nullable = false, length = 150)
    private String presentacion;

    @Column(nullable = true)
    private Integer cantidad; 

    @Column(nullable = false, length = 20)
    private String unidad;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_producto", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    private Producto producto;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof PresentacionProducto))
            return false;
        PresentacionProducto other = (PresentacionProducto) o;
        return idPresentacion != null && idPresentacion.equals(other.getIdPresentacion());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
