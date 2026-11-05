package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequeridoId;
import com.palmera_junior.gestion_compras.rrhh.entity.EtapaAdjuntoEmpleado;

public interface EmpleadoAdjuntoRequeridoRepository
        extends JpaRepository<EmpleadoAdjuntoRequerido, EmpleadoAdjuntoRequeridoId> {

    @EntityGraph(attributePaths = "tipoAdjunto")
    List<EmpleadoAdjuntoRequerido> findAllByEmpleado_IdEmpleado(Integer idEmpleado);

    List<EmpleadoAdjuntoRequerido> findAllByEmpleado_IdEmpleadoAndEtapaRequerida(
            Integer idEmpleado, EtapaAdjuntoEmpleado etapaRequerida);

    void deleteAllByEmpleado_IdEmpleado(Integer idEmpleado);

    void deleteAllByEmpleado_IdEmpleadoAndEtapaRequerida(
            Integer idEmpleado, EtapaAdjuntoEmpleado etapaRequerida);
}
