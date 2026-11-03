package com.palmera_junior.gestion_compras.rrhh.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "tipo_adjunto_empleado", schema = "rrhh")
public class TipoAdjuntoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_adjunto_empleado")
    private Integer idTipoAdjuntoEmpleado;

    @Column(nullable = false, length = 30, unique = true)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GrupoAdjuntoEmpleado grupo;

    @Column(nullable = false, unique = true)
    private short orden;

    @Column(name = "permite_multiples_vigentes", nullable = false)
    private boolean permiteMultiplesVigentes;

    @Column(nullable = false)
    private boolean activo = true;
}
