package com.palmera_junior.gestion_compras.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "categoria", schema = "compras")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Integer idCategoria;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Categoria other)) return false;
        return idCategoria != null && idCategoria.equals(other.idCategoria);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
