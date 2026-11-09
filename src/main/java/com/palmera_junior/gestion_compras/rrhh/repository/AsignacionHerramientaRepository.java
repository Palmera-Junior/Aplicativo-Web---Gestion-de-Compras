package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.AsignacionHerramienta;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoAsignacionHerramienta;

public interface AsignacionHerramientaRepository
        extends JpaRepository<AsignacionHerramienta, Long> {

    @EntityGraph(attributePaths = "herramienta")
    List<AsignacionHerramienta> findAllByEmpleado_IdEmpleadoOrderByEntregadaEnDesc(
            Integer idEmpleado);

    @EntityGraph(attributePaths = "herramienta")
    List<AsignacionHerramienta> findAllByEmpleado_IdEmpleadoAndEstadoOrderByHerramienta_NombreAsc(
            Integer idEmpleado, EstadoAsignacionHerramienta estado);

    Optional<AsignacionHerramienta> findByHerramienta_IdHerramientaAndEstado(
            Integer idHerramienta, EstadoAsignacionHerramienta estado);

    Optional<AsignacionHerramienta> findByEmpleado_IdEmpleadoAndHerramienta_IdHerramientaAndEstado(
            Integer idEmpleado,
            Integer idHerramienta,
            EstadoAsignacionHerramienta estado);
}
