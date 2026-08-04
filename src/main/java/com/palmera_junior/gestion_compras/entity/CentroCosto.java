package com.palmera_junior.gestion_compras.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "centro_costo")
public class CentroCosto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_centro_costo")
    private Integer idCentroCosto;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20, unique = true)
    private String codigo;

    @Column(length = 200)
    private String direccion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede", nullable = false)
    @ToString.Exclude
    private Sede sede;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CentroCosto)) return false;
        CentroCosto other = (CentroCosto) o;
        return idCentroCosto != null && idCentroCosto.equals(other.getIdCentroCosto());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

