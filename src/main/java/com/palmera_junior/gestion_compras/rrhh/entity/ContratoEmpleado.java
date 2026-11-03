package com.palmera_junior.gestion_compras.rrhh.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.palmera_junior.gestion_compras.entity.Empleado;

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

    @Column(name = "salario_basico", nullable = false, precision = 14, scale = 2)
    private BigDecimal salarioBasico;

    @Column(name = "auxilio_transporte_aplica", nullable = false)
    private boolean auxilioTransporteAplica;

    @Column(name = "auxilio_transporte_valor", precision = 14, scale = 2)
    private BigDecimal auxilioTransporteValor;

    @Column(name = "auxilio_rodamiento_aplica", nullable = false)
    private boolean auxilioRodamientoAplica;

    @Column(name = "auxilio_rodamiento_valor", precision = 14, scale = 2)
    private BigDecimal auxilioRodamientoValor;

    @Column(name = "bonificacion_no_salarial_aplica", nullable = false)
    private boolean bonificacionNoSalarialAplica;

    @Column(name = "bonificacion_no_salarial_valor", precision = 14, scale = 2)
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

    @Column(name = "tarifa_arl_porcentaje", precision = 6, scale = 3)
    private BigDecimal tarifaArlPorcentaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_caja_compensacion")
    @ToString.Exclude
    private CajaCompensacion cajaCompensacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoContratoEmpleado estado = EstadoContratoEmpleado.BORRADOR;
}
