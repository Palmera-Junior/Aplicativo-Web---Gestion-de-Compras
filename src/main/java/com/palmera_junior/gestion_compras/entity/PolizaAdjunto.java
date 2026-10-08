package com.palmera_junior.gestion_compras.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
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

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "poliza_adjunto")
public class PolizaAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adjunto_poliza")
    private Integer idAdjuntoPoliza;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poliza", nullable = false)
    @ToString.Exclude
    private Poliza poliza;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_renovacion", foreignKey = @ForeignKey(name = "fk_poliza_adjunto_renovacion"))
    @ToString.Exclude
    private PolizaRenovacion renovacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_adjunto", nullable = false, length = 30)
    private TipoAdjuntoPoliza tipoAdjunto;

    @Basic
    @Column(name = "contenido", columnDefinition = "BYTEA")
    private byte[] contenido;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;
}