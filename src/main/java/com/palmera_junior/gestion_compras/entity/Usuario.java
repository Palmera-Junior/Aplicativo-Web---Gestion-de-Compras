package com.palmera_junior.gestion_compras.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashSet;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Table(name = "usuario")

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 20, unique = true)
    private String cedula;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String apellido;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column (length = 100)
    private String proveedor;

    @Column(length = 100)
    private String proveedorId;

    @Column(length = 100)
    private String cargo;

    @Column(name = "nombre_usuario" ,nullable = false, length = 50, unique = true)
    private String nombreUsuario;
    
    @ToString.Exclude
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false, length = 255)
    private String contraseña;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<PermisoUsuario> permisosDirectos = new HashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sede", nullable = false)
    @ToString.Exclude
    private Sede sede;

    public Set<Permiso> getPermisos() {
        Set<Permiso> permisosEfectivos = new LinkedHashSet<>();
        if (rol != null && rol.getPermisos() != null) {
            permisosEfectivos.addAll(rol.getPermisos());
        }
        for (PermisoUsuario permisoUsuario : permisosDirectos == null ? Set.<PermisoUsuario>of() : permisosDirectos) {
            if (permisoUsuario.isConcedido()) {
                permisosEfectivos.add(permisoUsuario.getPermiso());
            } else {
                permisosEfectivos.remove(permisoUsuario.getPermiso());
            }
        }
        return permisosEfectivos;
    }

    public void setPermisos(Set<Permiso> permisos) {
        if (permisosDirectos == null) {
            permisosDirectos = new HashSet<>();
        }
        permisosDirectos.clear();
        if (permisos == null) {
            return;
        }
        for (Permiso permiso : permisos) {
            PermisoUsuario override = new PermisoUsuario();
            override.setUsuario(this);
            override.setPermiso(permiso);
            override.setConcedido(true);
            permisosDirectos.add(override);
        }
    }

    public void setPermisosDirectos(Set<PermisoUsuario> permisosDirectos) {
        this.permisosDirectos = permisosDirectos == null ? new HashSet<>() : permisosDirectos;
        this.permisosDirectos.forEach(permisoUsuario -> permisoUsuario.setUsuario(this));
    }

    @Override
    public boolean equals(Object o) {
        if(this== o) return true;
        if(!(o instanceof Usuario)) return false;
        Usuario other = (Usuario) o;
        return idUsuario != null && idUsuario.equals(other.getIdUsuario());
    }
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
    
}
