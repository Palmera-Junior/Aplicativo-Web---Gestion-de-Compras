package com.palmera_junior.gestion_compras.rrhh.entity;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class EmpleadoAdjuntoRequeridoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @Column(name = "id_tipo_adjunto_empleado")
    private Integer idTipoAdjuntoEmpleado;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EmpleadoAdjuntoRequeridoId that)) {
            return false;
        }
        return Objects.equals(idEmpleado, that.idEmpleado)
                && Objects.equals(idTipoAdjuntoEmpleado, that.idTipoAdjuntoEmpleado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idEmpleado, idTipoAdjuntoEmpleado);
    }
} 
