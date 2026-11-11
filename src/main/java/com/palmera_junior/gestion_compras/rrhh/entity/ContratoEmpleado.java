package com.palmera_junior.gestion_compras.rrhh.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.Area;
import com.palmera_junior.gestion_compras.rrhh.entity.Cargo;

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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "contrato_empleado", schema = "rrhh", uniqueConstraints = {
        @UniqueConstraint(name = "uq_contrato_empleado_id_empleado",
                columnNames = { "id_contrato_empleado", "id_empleado" })
})
public class ContratoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato_empleado")
    private Integer idContratoEmpleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    @ToString.Exclude
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_contrato")
    @ToString.Exclude
    private TipoContrato tipoContrato;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cargo")
    @ToString.Exclude
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area")
    @ToString.Exclude
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_pago")
    @ToString.Exclude
    private TipoPago tipoPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_entidad_bancaria")
    @ToString.Exclude
    private EntidadBancaria entidadBancaria;

    @Column(name = "numero_cuenta_bancaria", length = 50)
    private String numeroCuentaBancaria;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDate fechaIngreso;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "salario_basico", nullable = false, precision = 10, scale = 2)
    private BigDecimal salarioBasico;

    @Column(name = "auxilio_transporte_aplica", nullable = false)
    private boolean auxilioTransporteAplica;

    @Column(name = "auxilio_transporte_valor", precision = 10, scale = 2)
    private BigDecimal auxilioTransporteValor;

    @Column(name = "auxilio_rodamiento_aplica", nullable = false)
    private boolean auxilioRodamientoAplica;

    @Column(name = "auxilio_rodamiento_valor", precision = 10, scale = 2)
    private BigDecimal auxilioRodamientoValor;

    @Column(name = "bonificacion_no_salarial_aplica", nullable = false)
    private boolean bonificacionNoSalarialAplica;

    @Column(name = "bonificacion_no_salarial_valor", precision = 10, scale = 2)
    private BigDecimal bonificacionNoSalarialValor;

    @Column(name = "condiciones_especiales_aplica", nullable = false)
    private boolean condicionesEspecialesAplica;

    @Column(name = "condiciones_especiales", columnDefinition = "TEXT")
    private String condicionesEspeciales;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_eps")
    @ToString.Exclude
    private Eps eps;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_afp")
    @ToString.Exclude
    private Afp afp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_arl")
    @ToString.Exclude
    private Arl arl;

    @Column(name = "tarifa_arl_porcentaje", precision = 10, scale = 2)
    private BigDecimal tarifaArlPorcentaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_compensacion")
    @ToString.Exclude
    private CajaCompensacion cajaCompensacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContratoEmpleado estado = EstadoContratoEmpleado.BORRADOR;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminado_por")
    @ToString.Exclude
    private Usuario terminadoPor;

    @Column(name = "terminado_en")
    private LocalDate terminadoEn;

    @Column(name = "motivo_terminacion", columnDefinition = "TEXT")
    private String motivoTerminacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por")
    @ToString.Exclude
    private Usuario anuladoPor;

    @Column(name = "anulado_en")
    private LocalDate anuladoEn;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @Version
    @Column(nullable = false)
    private Long version;

    public boolean sincronizarEstado(LocalDate fechaActual) {
        if (estado == EstadoContratoEmpleado.BORRADOR
                || estado == EstadoContratoEmpleado.TERMINADO
                || estado == EstadoContratoEmpleado.ANULADO) {
            return false;
        }
        EstadoContratoEmpleado estadoCalculado =
                EstadoContratoEmpleado.calcular(fechaIngreso, fechaFin, fechaActual);
        if (estado == estadoCalculado) {
            return false;
        }
        estado = estadoCalculado;
        return true;
    }

    public void terminar(Usuario usuario, LocalDate fechaEfectiva, String motivo) {
        if (usuario == null || fechaEfectiva == null || motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Usuario, fecha y motivo son obligatorios para terminar el contrato");
        }
        if (estado == EstadoContratoEmpleado.BORRADOR
                || estado == EstadoContratoEmpleado.TERMINADO
                || estado == EstadoContratoEmpleado.ANULADO) {
            throw new IllegalStateException("El contrato no se puede terminar desde su estado actual");
        }
        estado = EstadoContratoEmpleado.TERMINADO;
        terminadoPor = usuario;
        terminadoEn = fechaEfectiva;
        motivoTerminacion = motivo.trim();
    }

    public void anular(Usuario usuario, LocalDate fechaEfectiva, String motivo) {
        if (usuario == null || fechaEfectiva == null || motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Usuario, fecha y motivo son obligatorios para anular el contrato");
        }
        if (estado == EstadoContratoEmpleado.TERMINADO
                || estado == EstadoContratoEmpleado.ANULADO) {
            throw new IllegalStateException("El contrato no se puede anular desde su estado actual");
        }
        estado = EstadoContratoEmpleado.ANULADO;
        anuladoPor = usuario;
        anuladoEn = fechaEfectiva;
        motivoAnulacion = motivo.trim();
    }
}
