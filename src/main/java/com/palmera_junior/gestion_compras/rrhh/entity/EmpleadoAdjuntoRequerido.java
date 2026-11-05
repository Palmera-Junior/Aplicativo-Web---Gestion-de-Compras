package com.palmera_junior.gestion_compras.rrhh.entity;

import com.palmera_junior.gestion_compras.entity.Empleado;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
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
@Table(name = "empleado_adjunto_requerido", schema = "rrhh")
public class EmpleadoAdjuntoRequerido {

    @EmbeddedId
    private EmpleadoAdjuntoRequeridoId id;

    @MapsId("idEmpleado")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    @ToString.Exclude
    private Empleado empleado;

    @MapsId("idTipoAdjuntoEmpleado")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_adjunto_empleado", nullable = false)
    @ToString.Exclude
    private TipoAdjuntoEmpleado tipoAdjunto;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa_requerida", nullable = false, length = 30)
    private EtapaAdjuntoEmpleado etapaRequerida = EtapaAdjuntoEmpleado.CONTRATACION;
}
