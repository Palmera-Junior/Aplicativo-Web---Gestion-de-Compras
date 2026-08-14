package com.palmera_junior.gestion_compras.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.entity.CentroCosto;

public interface CentroCostoRepository extends JpaRepository<CentroCosto, Integer> {

    List<CentroCosto> findBySedeIdSedeOrderByNombreAsc(Integer idSede);

boolean existsByNombreIgnoreCaseAndSedeIdSede(String nombre, Integer idSede);

    boolean existsByCodigoIgnoreCase(String codigo);
}

