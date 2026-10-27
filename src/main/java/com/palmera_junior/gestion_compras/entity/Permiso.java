package com.palmera_junior.gestion_compras.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permiso", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Integer idPermiso;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Permiso other)) return false;
        return idPermiso != null && idPermiso.equals(other.idPermiso);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
