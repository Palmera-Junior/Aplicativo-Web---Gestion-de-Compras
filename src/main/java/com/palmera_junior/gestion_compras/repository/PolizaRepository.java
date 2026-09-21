package com.palmera_junior.gestion_compras.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Poliza;

public interface PolizaRepository extends JpaRepository<Poliza, Integer>, JpaSpecificationExecutor<Poliza> {

    /**
     * La tabla y el modal de pólizas muestran datos del proveedor y del creador.
     * Se cargan junto con la página para que la plantilla no intente inicializar
     * proxies perezosos una vez haya terminado la transacción del servicio.
     */
    @Override
    @EntityGraph(attributePaths = { "proveedor", "sede", "usuario", "usuarioAprobacion" })
    Page<Poliza> findAll(Specification<Poliza> specification, Pageable pageable);

    Optional<Poliza> findByNumeroContrato(String numeroContrato);

    Page<Poliza> findAllByOrderByIdPolizaDesc(Pageable pageable);

    @EntityGraph(attributePaths = { "proveedor", "sede", "usuario", "usuarioAprobacion" })
    @Query("select p from Poliza p where p.idPoliza = :idPoliza")
    Optional<Poliza> findWithRelationsByIdPoliza(@Param("idPoliza") Integer idPoliza);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update Poliza p set p.estado = com.palmera_junior.gestion_compras.entity.EstadoPoliza.VENCIDA "
            + "where p.estado = com.palmera_junior.gestion_compras.entity.EstadoPoliza.VIGENTE "
            + "and p.fechaVencimiento < :fecha")
    int marcarVigentesVencidas(@Param("fecha") LocalDate fecha);
}
