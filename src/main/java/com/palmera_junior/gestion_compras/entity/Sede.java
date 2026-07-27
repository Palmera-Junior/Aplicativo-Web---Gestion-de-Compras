package com.palmera_junior.gestion_compras.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;


@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "sede")
public class Sede {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    private Integer idSede;
    @Column(nullable = false, length = 100)
    private String nombre;
    @Column(name = "prefijo_ciudad", nullable = false, length = 10, unique = true)
    private String prefijoCiudad;
    @Column(length = 255)
    private String direccion;

    @OneToMany(mappedBy = "sede", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<CentroCosto> centroCostos = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if(this== o) return true;
        if(!(o instanceof Sede)) return false;
        Sede other = (Sede) o;
        return idSede != null && idSede.equals(other.getIdSede());
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }






    
    
    
}
