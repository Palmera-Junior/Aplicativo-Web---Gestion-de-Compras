package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequeridoId;

public interface EmpleadoAdjuntoRequeridoRepository
        extends JpaRepository<EmpleadoAdjuntoRequerido, EmpleadoAdjuntoRequeridoId> {

    List<EmpleadoAdjuntoRequerido> findAllByEmpleado_IdEmpleado(Integer idEmpleado);

    void deleteAllByEmpleado_IdEmpleado(Integer idEmpleado);
}
