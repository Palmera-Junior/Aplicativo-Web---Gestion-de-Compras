package com.palmera_junior.gestion_compras.rrhh.entity;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Usuario;

import jakarta.persistence.Basic;
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
@Table(name = "asignacion_herramienta", schema = "rrhh")
public class AsignacionHerramienta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_asignacion_herramienta")
    private Long idAsignacionHerramienta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_herramienta", nullable = false)
    @ToString.Exclude
    private Herramienta herramienta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    @ToString.Exclude
    private Empleado empleado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsignacionHerramienta estado = EstadoAsignacionHerramienta.PENDIENTE;

    @Column(name = "acta_nombre", length = 255)
    private String actaNombre;

    @Column(name = "acta_mime", length = 150)
    private String actaMime;

    @Column(name = "acta_tamano_bytes")
    private Long actaTamanoBytes;

    @Basic(fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "acta_contenido", columnDefinition = "BYTEA")
    @ToString.Exclude
    private byte[] actaContenido;

    @Column(name = "entregada_en")
    private OffsetDateTime entregadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entregada_por")
    @ToString.Exclude
    private Usuario entregadaPor;

    @Column(name = "recibida_en")
    private OffsetDateTime recibidaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recibida_por")
    @ToString.Exclude
    private Usuario recibidaPor;
}
