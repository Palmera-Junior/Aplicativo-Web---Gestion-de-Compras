package com.palmera_junior.gestion_compras.rrhh.entity;

import java.time.OffsetDateTime;

import com.palmera_junior.gestion_compras.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "contrato_empleado_historial_estado", schema = "rrhh")
public class ContratoEmpleadoHistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato_historial_estado")
    private Long idContratoHistorialEstado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_contrato_empleado", nullable = false)
    @ToString.Exclude
    private ContratoEmpleado contrato;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    private EstadoContratoEmpleado estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoContratoEmpleado estadoNuevo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    @ToString.Exclude
    private Usuario usuario;

    @Column(name = "fecha_cambio", nullable = false)
    private OffsetDateTime fechaCambio;

    @Column(nullable = false, length = 500)
    private String detalle;
}
