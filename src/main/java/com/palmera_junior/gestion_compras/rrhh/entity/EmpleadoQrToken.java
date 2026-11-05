package com.palmera_junior.gestion_compras.rrhh.entity;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
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
@Table(name = "empleado_qr_token", schema = "rrhh")
public class EmpleadoQrToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado_qr_token")
    private Long idEmpleadoQrToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    @ToString.Exclude
    private Empleado empleado;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "emitido_en", nullable = false, updatable = false)
    private OffsetDateTime emitidoEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emitido_por", nullable = false)
    @ToString.Exclude
    private Usuario emitidoPor;

    @Column(name = "invalidado_en")
    private OffsetDateTime invalidadoEn;

    @Column(nullable = false)
    private boolean activo = true;

    @PrePersist
    void inicializarFechaEmision() {
        if (emitidoEn == null) {
            emitidoEn = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public void invalidar() {
        if (!activo) {
            throw new IllegalStateException("El QR ya está invalidado");
        }
        invalidadoEn = OffsetDateTime.now(ZoneOffset.UTC);
        activo = false;
    }
}
