package com.palmera_junior.gestion_compras.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.BatchSize;
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

    @Column(name = "numero_poliza", length = 100)
    private String numeroPoliza;

    @Column(name = "valor_prima", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorPrima;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<DetallePrima> detallesPrima = new ArrayList<>();

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PolizaSedeAprobacion> aprobacionesPorSede = new ArrayList<>();

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @ToString.Exclude
    private List<PolizaAdjunto> adjuntos = new ArrayList<>();

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<PolizaRenovacion> renovaciones = new ArrayList<>();

    public void addDetallePrima(DetallePrima detalle) {
        detallesPrima.add(detalle);
        detalle.setPoliza(this);
    }

    public void addAprobacionPorSede(PolizaSedeAprobacion aprobacion) {
        aprobacionesPorSede.add(aprobacion);
        aprobacion.setPoliza(this);
    }

    public Optional<PolizaSedeAprobacion> obtenerAprobacionPorSede(Integer idSede) {
        if (idSede == null) {
            return Optional.empty();
        }
        return aprobacionesPorSede.stream()
                .filter(aprobacion -> aprobacion.getSede() != null
                        && idSede.equals(aprobacion.getSede().getIdSede()))
                .findFirst();
    }

    public boolean todasLasSedesAprobadas() {
        return aprobacionesPorSede != null
                && !aprobacionesPorSede.isEmpty()
                && aprobacionesPorSede.stream()
                        .allMatch(aprobacion -> aprobacion.getEstado() == EstadoAprobacionSede.APROBADA);
    }

    public EstadoPoliza getEstadoParaSede(Integer idSede) {
        boolean esSedePrincipal = sede != null && idSede != null && idSede.equals(sede.getIdSede());
        boolean aproboLaSedeSecundaria = !esSedePrincipal
                && estado == EstadoPoliza.BORRADOR
                && obtenerAprobacionPorSede(idSede)
                        .map(aprobacion -> aprobacion.getEstado() == EstadoAprobacionSede.APROBADA)
                        .orElse(false);
        return aproboLaSedeSecundaria ? EstadoPoliza.APROBADA : estado;
    }

    @Column(name = "valor_contrato", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorContrato;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_activacion")
    @ToString.Exclude
    private Usuario usuarioActivacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    @ToString.Exclude
    private Usuario usuarioAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDate fechaAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_terminacion")
    @ToString.Exclude
    private Usuario usuarioTerminacion;

    @Column(name = "fecha_terminacion")
    private LocalDate fechaTerminacion;

    @Column(name = "motivo_terminacion", columnDefinition = "TEXT")
    private String motivoTerminacion;

    public boolean estaProximaAVencer() {
        return estado != EstadoPoliza.ANULADA && estado != EstadoPoliza.TERMINADA
            && fechaVencimiento != null
                && !fechaVencimiento.isBefore(LocalDate.now())
                && !fechaVencimiento.isAfter(LocalDate.now().plusDays(30));
    }

    public void aprobar(Usuario aprobador, LocalDate fecha) {
        if (estado != EstadoPoliza.BORRADOR) {
            throw new IllegalStateException("Solo las pólizas en estado BORRADOR pueden aprobarse");
        }
        estado = EstadoPoliza.APROBADA;
        usuarioAprobacion = aprobador;
        fechaAprobacion = fecha;
    }

    public void addAdjunto(PolizaAdjunto adjunto) {
        if (adjunto == null) {
            throw new IllegalArgumentException("El adjunto de póliza es obligatorio");
        }
        adjuntos.add(adjunto);
        adjunto.setPoliza(this);
    }

    public void removeAdjunto(PolizaAdjunto adjunto) {
        if (adjunto != null && adjuntos.remove(adjunto)) {
            adjunto.setPoliza(null);
        }
    }

    @Transient
    public String getContratoAdjunto() {
        return obtenerAdjuntoContrato().map(PolizaAdjunto::getNombreOriginal).orElse(null);
    }

    @Transient
    public byte[] getContratoAdjuntoContenido() {
        return obtenerAdjuntoContrato().map(PolizaAdjunto::getContenido).orElse(null);
    }

    @Transient
    public String getContratoAdjuntoNombre() {
        return obtenerAdjuntoContrato().map(PolizaAdjunto::getNombreOriginal).orElse(null);
    }

    public Optional<PolizaAdjunto> obtenerAdjuntoContrato() {
        return adjuntos.stream()
                .filter(adjunto -> adjunto.getTipoAdjunto() == TipoAdjuntoPoliza.CONTRATO)
                .findFirst();
    }

    public void activar(String numeroPoliza, List<PolizaAdjunto> adjuntos) {
        if (estado != EstadoPoliza.APROBADA) {
            throw new IllegalStateException("Solo las pólizas APROBADAS pueden pasar a estado VIGENTE");
        }
        if (adjuntos == null || adjuntos.isEmpty()) {
            throw new IllegalArgumentException("Debe adjuntar al menos un PDF de la póliza física");
        }
        if (numeroPoliza == null || numeroPoliza.isBlank()) {
            throw new IllegalArgumentException("El número de póliza es obligatorio");
        }
        this.numeroPoliza = numeroPoliza.trim();
        adjuntos.forEach(adjunto -> {
            adjunto.setTipoAdjunto(TipoAdjuntoPoliza.POLIZA_FISICA);
            addAdjunto(adjunto);
        });
        estado = EstadoPoliza.VIGENTE;
    }

    public void anular() {
        if (estado != EstadoPoliza.BORRADOR && estado != EstadoPoliza.APROBADA) {
            throw new IllegalStateException("Solo se pueden anular pólizas borrador o aprobadas");
        }
        estado = EstadoPoliza.ANULADA;
    }

    public void terminar(Usuario usuario, LocalDate fecha, String motivo) {
        if (estado != EstadoPoliza.VIGENTE) {
            throw new IllegalStateException("Solo se pueden terminar pólizas vigentes");
        }
        if (usuario == null || fecha == null || motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El usuario, la fecha y el motivo de terminación son obligatorios");
        }
        estado = EstadoPoliza.TERMINADA;
        usuarioTerminacion = usuario;
        fechaTerminacion = fecha;
        motivoTerminacion = motivo.trim();
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
