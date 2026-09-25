package com.palmera_junior.gestion_compras.entity;

import java.time.LocalDate;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Table(name = "poliza_sede_aprobacion",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_poliza", "id_sede"}))
public class PolizaSedeAprobacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_poliza_sede_aprobacion")
    private Integer idPolizaSedeAprobacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poliza", nullable = false)
    @ToString.Exclude
    private Poliza poliza;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    @ToString.Exclude
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aprobacion", nullable = false, length = 20)
    private EstadoAprobacionSede estado = EstadoAprobacionSede.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_aprobacion")
    @ToString.Exclude
    private Usuario usuarioAprobacion;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    public void aprobar(Usuario aprobador, LocalDate fecha) {
        if (aprobador == null || fecha == null) {
            throw new IllegalArgumentException("El aprobador y la fecha son obligatorios");
        }
        this.estado = EstadoAprobacionSede.APROBADA;
        this.usuarioAprobacion = aprobador;
        this.fechaAprobacion = fecha;
    }
}
