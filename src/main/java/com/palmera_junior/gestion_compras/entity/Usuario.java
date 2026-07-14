package com.palmera_junior.gestion_compras.entity;

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

    @Column(length = 100)
    private String cargo;

    @Column(name = "nombre_usuario" ,nullable = false, length = 50, unique = true)
    private String nombreUsuario;

    @Column(nullable = false, length = 255)
    private String contraseña;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sede", nullable = false)
    @ToString.Exclude
    private Sede sede;

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
