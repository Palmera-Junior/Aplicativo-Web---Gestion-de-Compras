package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.entity.PolizaSedeAprobacion;

public interface PolizaSedeAprobacionRepository extends JpaRepository<PolizaSedeAprobacion, Integer> {

    @EntityGraph(attributePaths = { "sede", "usuarioAprobacion" })
    List<PolizaSedeAprobacion> findByPolizaIdPoliza(Integer idPoliza);

    @EntityGraph(attributePaths = { "sede", "usuarioAprobacion" })
    Optional<PolizaSedeAprobacion> findByPolizaIdPolizaAndSedeIdSede(Integer idPoliza, Integer idSede);
}
