package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoContratoEmpleado;

public interface ContratoEmpleadoRepository extends JpaRepository<ContratoEmpleado, Integer> {

    List<ContratoEmpleado> findAllByEmpleado_IdEmpleadoOrderByFechaIngresoDesc(Integer idEmpleado);

    Optional<ContratoEmpleado> findByEmpleado_IdEmpleadoAndEstado(
            Integer idEmpleado, EstadoContratoEmpleado estado);

    List<ContratoEmpleado> findAllByEstadoIn(List<EstadoContratoEmpleado> estados);
}
