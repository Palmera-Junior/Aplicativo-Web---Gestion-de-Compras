package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoHistorialEstado;

public interface EmpleadoHistorialEstadoRepository
        extends JpaRepository<EmpleadoHistorialEstado, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<EmpleadoHistorialEstado> findAllByEmpleado_IdEmpleadoOrderByFechaCambioDescIdEmpleadoHistorialEstadoDesc(
            Integer idEmpleado);
}
