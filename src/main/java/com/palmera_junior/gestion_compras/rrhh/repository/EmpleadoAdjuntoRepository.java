package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;

public interface EmpleadoAdjuntoRepository extends JpaRepository<EmpleadoAdjunto, Long> {

    List<EmpleadoAdjunto> findAllByEmpleado_IdEmpleadoOrderByFechaCargaDesc(Integer idEmpleado);

    List<EmpleadoAdjunto> findAllByEmpleado_IdEmpleadoAndTipoAdjunto_IdTipoAdjuntoEmpleadoAndVigenteTrueOrderByFechaCargaDesc(
            Integer idEmpleado, Integer idTipoAdjuntoEmpleado);
}
