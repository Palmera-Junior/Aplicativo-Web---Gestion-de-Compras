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
import com.palmera_junior.gestion_compras.entity.EstadoAprobacionSede;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;

public interface PolizaRepository extends JpaRepository<Poliza, Integer>, JpaSpecificationExecutor<Poliza> {

    /**
     * La tabla y el modal de pólizas muestran datos del proveedor y del creador.
     * Se cargan junto con la página para que la plantilla no intente inicializar
     * proxies perezosos una vez haya terminado la transacción del servicio.
     */
    @Override
    @EntityGraph(attributePaths = { "proveedor", "sede", "usuario", "usuarioAprobacion", "aprobacionesPorSede.sede", "aprobacionesPorSede.usuarioAprobacion" })
    Page<Poliza> findAll(Specification<Poliza> specification, Pageable pageable);

    Optional<Poliza> findByNumeroContrato(String numeroContrato);

    Page<Poliza> findAllByOrderByIdPolizaDesc(Pageable pageable);

    long countByEstadoAndSedeIdSede(EstadoPoliza estado, Integer idSede);

    /**
     * Qué hace: Cuenta las pólizas en estado borrador que aún tienen aprobaciones pendientes para una sede específica.
     * Solo considera registros cuya aprobación asociada a la sede esté en estado pendiente.
     *
     * A dónde apunta: Tabla `poliza`, relación `aprobaciones_por_sede` y entidad `sede`.
     */
    @Query("select count(distinct p) from Poliza p join p.aprobacionesPorSede aprobacion "
            + "where p.estado = :estado and aprobacion.sede.idSede = :idSede "
            + "and aprobacion.estado = :estadoAprobacion")
    long countPendientesAprobacionPorSede(@Param("estado") EstadoPoliza estado,
            @Param("idSede") Integer idSede,
            @Param("estadoAprobacion") EstadoAprobacionSede estadoAprobacion);

    @EntityGraph(attributePaths = { "proveedor", "sede", "usuario", "usuarioAprobacion", "detallesPrima" })
    @Query("select p from Poliza p where p.idPoliza = :idPoliza")
    Optional<Poliza> findWithRelationsByIdPoliza(@Param("idPoliza") Integer idPoliza);

    /**
     * Carga exclusivamente el grafo necesario para consultar aprobaciones por sede.
     * No se combina con detallesPrima porque ambas asociaciones son listas y Hibernate
     * no permite cargar dos colecciones tipo bag con fetch join en la misma consulta.
     */
    @EntityGraph(attributePaths = { "sede", "aprobacionesPorSede.sede", "aprobacionesPorSede.usuarioAprobacion" })
    @Query("select p from Poliza p where p.idPoliza = :idPoliza")
    Optional<Poliza> findWithAprobacionesPorSedeByIdPoliza(@Param("idPoliza") Integer idPoliza);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update Poliza p set p.estado = com.palmera_junior.gestion_compras.entity.EstadoPoliza.VENCIDA "
            + "where p.estado = com.palmera_junior.gestion_compras.entity.EstadoPoliza.VIGENTE "
            + "and p.fechaVencimiento < :fecha")
    int marcarVigentesVencidas(@Param("fecha") LocalDate fecha);
}
