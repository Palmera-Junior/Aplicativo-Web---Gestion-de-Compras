package com.palmera_junior.gestion_compras.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "poliza_renovacion")
public class PolizaRenovacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_renovacion")
    private Integer idRenovacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poliza", nullable = false)
    @ToString.Exclude
    private Poliza poliza;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "fecha_inicio_anterior", nullable = false)
    private LocalDate fechaInicioAnterior;

    @Column(name = "fecha_fin_anterior", nullable = false)
    private LocalDate fechaFinAnterior;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @OneToMany(mappedBy = "renovacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PolizaAdjunto> adjuntos = new ArrayList<>();

    public void addAdjunto(PolizaAdjunto adjunto) {
        adjuntos.add(adjunto);
        adjunto.setRenovacion(this);
        adjunto.setPoliza(poliza);
        adjunto.setTipoAdjunto(TipoAdjuntoPoliza.RENOVACION);
    }
}