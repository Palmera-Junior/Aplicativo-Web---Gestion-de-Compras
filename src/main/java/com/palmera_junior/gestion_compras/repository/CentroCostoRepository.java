package com.palmera_junior.gestion_compras.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.palmera_junior.gestion_compras.entity.CentroCosto;

@Repository
public interface CentroCostoRepository extends JpaRepository<CentroCosto, Integer> {

    List<CentroCosto> findBySedeIdSedeOrderByNombreAsc(Integer idSede);
}

