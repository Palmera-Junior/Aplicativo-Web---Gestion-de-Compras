package com.palmera_junior.gestion_compras.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.entity.CentroCosto;

public interface CentroCostoRepository extends JpaRepository<CentroCosto, Integer> {

    List<CentroCosto> findBySedeIdSedeOrderByNombreAsc(Integer idSede);

    /**
     * Qué hace: Verifica si ya existe un centro de costo con el mismo nombre en una sede dada (case-insensitive).
     * A dónde apunta: Tabla `centro_costo`.
     */
    boolean existsByNombreIgnoreCaseAndSedeIdSede(String nombre, Integer idSede);

    boolean existsByCodigoIgnoreCase(String codigo);
}

