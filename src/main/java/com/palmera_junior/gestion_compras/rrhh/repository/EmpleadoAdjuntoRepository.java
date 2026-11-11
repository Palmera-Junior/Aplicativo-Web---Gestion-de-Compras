package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;

public interface EmpleadoAdjuntoRepository extends JpaRepository<EmpleadoAdjunto, Long> {

    @EntityGraph(attributePaths = "tipoAdjunto")
    List<EmpleadoAdjunto> findAllByEmpleado_IdEmpleadoOrderByFechaCargaDesc(Integer idEmpleado);

    List<EmpleadoAdjunto> findAllByEmpleado_IdEmpleadoAndTipoAdjunto_IdTipoAdjuntoEmpleadoAndVigenteTrueOrderByFechaCargaDesc(
            Integer idEmpleado, Integer idTipoAdjuntoEmpleado);

    @EntityGraph(attributePaths = {"tipoAdjunto", "empleado"})
    List<EmpleadoAdjunto> findAllByEmpleado_IdEmpleadoAndPublicoQrTrueAndVigenteTrueOrderByFechaCargaDesc(
            Integer idEmpleado);

    Optional<EmpleadoAdjunto> findFirstByEmpleado_IdEmpleadoAndContrato_IdContratoEmpleadoAndTipoAdjunto_CodigoOrderByFechaCargaDesc(
            Integer idEmpleado, Integer idContratoEmpleado, String codigoTipoAdjunto);
}
