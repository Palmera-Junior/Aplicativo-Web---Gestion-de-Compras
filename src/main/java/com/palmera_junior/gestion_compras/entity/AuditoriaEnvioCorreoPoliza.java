package com.palmera_junior.gestion_compras.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "auditoria_envio_correo_poliza", indexes = {
        @Index(name = "idx_auditoria_poliza_estado_reintento", columnList = "estado,proximo_intento")
})
public class AuditoriaEnvioCorreoPoliza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria_correo_poliza")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_poliza", nullable = false)
    private Poliza poliza;

    @Column(nullable = false, length = 150)
    private String destinatario;

    @Column(nullable = false, length = 20)
    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    private EstadoEnvioCorreo estado;

    @Column(nullable = false)
    private Integer intentos = 0;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    @Column(name = "bloqueado_en")
    private LocalDateTime bloqueadoEn;

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @Column(name = "ultimo_error", columnDefinition = "TEXT")
    private String ultimoError;

    @PrePersist
    void prepararCreacion() {
        LocalDateTime ahora = LocalDateTime.now();
        creadoEn = ahora;
        actualizadoEn = ahora;
        if (proximoIntento == null) {
            proximoIntento = ahora;
        }
    }
}