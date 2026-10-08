package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.entity.PolizaRenovacion;

public interface PolizaRenovacionRepository extends JpaRepository<PolizaRenovacion, Integer> {

    @EntityGraph(attributePaths = { "usuario", "adjuntos" })
    List<PolizaRenovacion> findByPolizaIdPolizaOrderByFechaInicioAsc(Integer idPoliza);

    @EntityGraph(attributePaths = { "usuario", "adjuntos" })
    Optional<PolizaRenovacion> findByIdRenovacionAndPolizaIdPoliza(Integer idRenovacion, Integer idPoliza);
}