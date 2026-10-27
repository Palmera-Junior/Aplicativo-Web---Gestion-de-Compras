package com.palmera_junior.gestion_compras.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol", schema = "core")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer idRol;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "permisos_predeterminados_cargados", nullable = false,
            columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
    private boolean permisosPredeterminadosCargados;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rol_permiso", schema = "core",
            joinColumns = @JoinColumn(name = "id_rol"),
            inverseJoinColumns = @JoinColumn(name = "id_permiso"))
    @ToString.Exclude
    private Set<Permiso> permisos = new HashSet<>();

    public Rol(Integer idRol, String codigo) {
        this.idRol = idRol;
        this.codigo = codigo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rol other)) return false;
        return idRol != null && idRol.equals(other.idRol);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
