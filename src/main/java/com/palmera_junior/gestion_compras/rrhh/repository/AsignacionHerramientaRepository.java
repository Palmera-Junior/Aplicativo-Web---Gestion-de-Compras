package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.AsignacionHerramienta;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoAsignacionHerramienta;

public interface AsignacionHerramientaRepository
        extends JpaRepository<AsignacionHerramienta, Long> {

    List<AsignacionHerramienta> findAllByEmpleado_IdEmpleadoOrderByEntregadaEnDesc(
            Integer idEmpleado);

    Optional<AsignacionHerramienta> findByHerramienta_IdHerramientaAndEstado(
            Integer idHerramienta, EstadoAsignacionHerramienta estado);
}
