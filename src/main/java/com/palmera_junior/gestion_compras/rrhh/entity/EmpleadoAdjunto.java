package com.palmera_junior.gestion_compras.rrhh.entity;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.palmera_junior.gestion_compras.entity.Empleado;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "empleado_adjunto", schema = "rrhh")
public class EmpleadoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado_adjunto")
    private Long idEmpleadoAdjunto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    @ToString.Exclude
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_tipo_adjunto_empleado", nullable = false)
    @ToString.Exclude
    private TipoAdjuntoEmpleado tipoAdjunto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_contrato_empleado",
            foreignKey = @ForeignKey(name = "fk_empleado_adjunto_contrato_empleado"))
    @ToString.Exclude
    private ContratoEmpleado contrato;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_mime", nullable = false, length = 150)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Basic(fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "contenido", nullable = false, columnDefinition = "BYTEA")
    @ToString.Exclude
    private byte[] contenido;

    @Column(name = "fecha_carga", nullable = false)
    private OffsetDateTime fechaCarga;

    @Column(name = "es_vigente", nullable = false)
    private boolean vigente = true;
}
