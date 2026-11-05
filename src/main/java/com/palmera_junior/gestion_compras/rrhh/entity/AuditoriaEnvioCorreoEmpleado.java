package com.palmera_junior.gestion_compras.rrhh.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "auditoria_envio_correo_empleado", schema = "rrhh", uniqueConstraints = {
        @UniqueConstraint(name = "uq_auditoria_correo_empleado_evento",
                columnNames = { "id_empleado", "destinatario", "clave_evento" })
})
public class AuditoriaEnvioCorreoEmpleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria_correo_empleado")
    private Long idAuditoriaCorreoEmpleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    @ToString.Exclude
    private Empleado empleado;

    @Column(nullable = false, length = 150)
    private String destinatario;

    @Column(name = "clave_evento", nullable = false, length = 120)
    private String claveEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEnvioCorreo estado = EstadoEnvioCorreo.PENDIENTE;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_intento")
    private OffsetDateTime proximoIntento;

    @Column(name = "bloqueado_en")
    private OffsetDateTime bloqueadoEn;

    @Column(name = "enviado_en")
    private OffsetDateTime enviadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    @PrePersist
    void inicializarFechas() {
        OffsetDateTime ahora = ahoraUtc();
        if (creadoEn == null) {
            creadoEn = ahora;
        }
        actualizadoEn = ahora;
    }

    @PreUpdate
    void actualizarFecha() {
        actualizadoEn = ahoraUtc();
    }

    private static OffsetDateTime ahoraUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
