package com.palmera_junior.gestion_compras.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
@Table(name = "usuario", schema = "core")

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "email", length = 100, unique = true)
    private String email;

    @Column(length = 100)
    private String proveedor;

    @Column(length = 100)
    private String proveedorId;

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

    @Transient
    @ToString.Exclude
    private Set<Permiso> permisosSuperAdministrador = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_sede", nullable = false)
    @ToString.Exclude
    private Sede sede;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "usuario_sede", schema = "core",
            joinColumns = @JoinColumn(name = "id_usuario"),
            inverseJoinColumns = @JoinColumn(name = "id_sede"))
    @JsonIgnore
    @ToString.Exclude
    private Set<Sede> sedes = new LinkedHashSet<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_empleado", unique = true)
    @JsonIgnore
    @ToString.Exclude
    private Empleado empleado;

    @Transient
    public String getNombre() {
        return empleado == null
                ? nombreUsuario
                : unirNombres(empleado.getPrimerNombre(), empleado.getSegundoNombre());
    }

    @Transient
    public String getApellido() {
        return empleado == null
                ? ""
                : unirNombres(empleado.getPrimerApellido(), empleado.getSegundoApellido());
    }

    @Transient
    public String getCedula() {
        return empleado == null ? null : empleado.getNumeroDocumento();
    }

    @Transient
    public String getCargo() {
        return empleado == null || empleado.getCargo() == null
                ? null
                : empleado.getCargo().getNombre();
    }

    @Transient
    @JsonIgnore
    public String getSedeIdsCsv() {
        Set<Integer> ids = getIdsSedesAsignadas();
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    @Transient
    @JsonIgnore
    public Set<Integer> getIdsSedesAsignadas() {
        Set<Integer> ids = new LinkedHashSet<>();
        if (sedes != null) {
            sedes.stream()
                    .map(Sede::getIdSede)
                    .filter(java.util.Objects::nonNull)
                    .forEach(ids::add);
        }
        if (sede != null && sede.getIdSede() != null) {
            ids.add(sede.getIdSede());
        }
        return ids;
    }

    @Transient
    @JsonIgnore
    public boolean tieneSedeAsignada(Integer idSede) {
        return idSede != null && getIdsSedesAsignadas().contains(idSede);
    }

    private static String unirNombres(String primero, String segundo) {
        if (primero == null || primero.isBlank()) {
            return segundo == null ? "" : segundo;
        }
        return segundo == null || segundo.isBlank() ? primero : primero + " " + segundo;
    }

    public Set<Permiso> getPermisos() {
        Set<Permiso> permisosEfectivos = new LinkedHashSet<>();
        if (rol != null && rol.getPermisos() != null) {
            permisosEfectivos.addAll(rol.getPermisos());
        }
        if (esSuperAdministrador()) {
            if (permisosSuperAdministrador != null) {
                permisosEfectivos.addAll(permisosSuperAdministrador);
            }
            return permisosEfectivos;
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

    @Transient
    @JsonIgnore
    public boolean esSuperAdministrador() {
        return rol != null && "SUPERADMINISTRADOR".equalsIgnoreCase(rol.getCodigo());
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
