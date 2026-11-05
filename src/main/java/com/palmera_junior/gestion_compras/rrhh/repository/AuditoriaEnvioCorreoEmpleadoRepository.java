package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.AuditoriaEnvioCorreoEmpleado;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;

public interface AuditoriaEnvioCorreoEmpleadoRepository
        extends JpaRepository<AuditoriaEnvioCorreoEmpleado, Long> {

    List<AuditoriaEnvioCorreoEmpleado> findAllByEstadoInOrderByCreadoEnAsc(
            List<EstadoEnvioCorreo> estados);
}
