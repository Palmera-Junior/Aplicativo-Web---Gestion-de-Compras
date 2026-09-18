package com.palmera_junior.gestion_compras.entity;

import java.math.BigDecimal;
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
@Table(name = "poliza")
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_poliza")
    private Integer idPoliza;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_prov", nullable = false)
    @ToString.Exclude
    private Proveedor proveedor;

    @Column(nullable = false, length = 150)
    private String cliente;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "numero_contrato", nullable = false, length = 50, unique = true)
    private String numeroContrato;

    @Column(name = "valor_prima", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPrima;

    @Column(name = "valor_contrato", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorContrato;

    @Column(name = "contrato_adjunto", columnDefinition = "TEXT")
    private String contratoAdjunto;

    @Column(name = "contrato_adjunto_nombre", length = 255)
    private String contratoAdjuntoNombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPoliza estado = EstadoPoliza.BORRADOR;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    @ToString.Exclude
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    @ToString.Exclude
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_aprobacion")
    @ToString.Exclude
    private Usuario usuarioAprobacion;

    @Column(name = "fecha_aprobacion")
    private LocalDate fechaAprobacion;

    public boolean estaProximaAVencer() {
        return fechaVencimiento != null
                && !fechaVencimiento.isAfter(LocalDate.now().plusDays(10));
    }

    public void aprobar(Usuario aprobador, LocalDate fecha) {
        if (estado != EstadoPoliza.BORRADOR) {
            throw new IllegalStateException("Solo las pólizas en estado BORRADOR pueden aprobarse");
        }
        estado = EstadoPoliza.APROBADA;
        usuarioAprobacion = aprobador;
        fechaAprobacion = fecha;
    }

    public void anular() {
        if (estado != EstadoPoliza.BORRADOR && estado != EstadoPoliza.APROBADA) {
            throw new IllegalStateException("Solo se pueden anular pólizas borrador o aprobadas");
        }
        estado = EstadoPoliza.ANULADA;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Poliza other)) {
            return false;
        }
        return idPoliza != null && idPoliza.equals(other.getIdPoliza());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}