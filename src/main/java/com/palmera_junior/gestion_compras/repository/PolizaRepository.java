package com.palmera_junior.gestion_compras.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.palmera_junior.gestion_compras.entity.Poliza;

public interface PolizaRepository extends JpaRepository<Poliza, Integer>, JpaSpecificationExecutor<Poliza> {

    Optional<Poliza> findByNumeroContrato(String numeroContrato);

    Page<Poliza> findAllByOrderByIdPolizaDesc(Pageable pageable);

    @EntityGraph(attributePaths = { "proveedor", "sede", "usuario", "usuarioAprobacion" })
    @Query("select p from Poliza p where p.idPoliza = :idPoliza")
    Optional<Poliza> findWithRelationsByIdPoliza(@Param("idPoliza") Integer idPoliza);
}