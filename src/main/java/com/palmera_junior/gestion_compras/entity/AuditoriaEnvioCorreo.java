package com.palmera_junior.gestion_compras.entity;

import java.time.LocalDateTime;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "auditoria_envio_correo", indexes = {
        @Index(name = "idx_auditoria_correo_estado_reintento", columnList = "estado,proximo_intento")
})
public class AuditoriaEnvioCorreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria_correo")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_orden", nullable = false)
    private OrdenCompra ordenCompra;

    @Column(nullable = false, length = 150)
    private String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
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
