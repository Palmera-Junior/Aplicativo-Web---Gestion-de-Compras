package com.palmera_junior.gestion_compras.rrhh.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.rrhh.entity.GrupoAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoAdjuntoEmpleado;

public interface TipoAdjuntoEmpleadoRepository extends JpaRepository<TipoAdjuntoEmpleado, Integer> {

    List<TipoAdjuntoEmpleado> findAllByActivoTrueOrderByOrdenAsc();

    List<TipoAdjuntoEmpleado> findAllByGrupoAndActivoTrueOrderByOrdenAsc(GrupoAdjuntoEmpleado grupo);

    Optional<TipoAdjuntoEmpleado> findByCodigoAndActivoTrue(String codigo);
}
