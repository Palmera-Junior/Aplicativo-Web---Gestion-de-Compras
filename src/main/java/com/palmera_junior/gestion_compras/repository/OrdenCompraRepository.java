package com.palmera_junior.gestion_compras.repository;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import com.palmera_junior.gestion_compras.entity.OrdenCompra;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Integer>, JpaSpecificationExecutor<OrdenCompra> {

    Page<OrdenCompra> findBySedeIdSedeOrderByFechaDesc(Integer idSede, Pageable pageable);

    Optional<OrdenCompra> findByNumeroOrden(String numeroOrden);

    Page<OrdenCompra> findBySedeIdSedeAndNumeroOrdenContainingIgnoreCase(Integer idSede, String numeroOrden, Pageable pageable);

    Optional<OrdenCompra> findById(Integer idOrden);

    /**
     * Qué hace: Carga una orden de compra con todas sus asociaciones eagerly (proveedor, sede, centro, detalles, producto) para la generación de PDF y correo.
     * A dónde apunta: Tablas `orden_compra`, `proveedor`, `sede`, `centro_costo`, `usuario`, `detalle_compra`, `producto`.
     */
    @EntityGraph(attributePaths = {
            "proveedor", "sede", "centroCosto", "usuario", "usuarioAprobacion", "detalles", "detalles.producto"
    })
    @Query("select o from OrdenCompra o where o.idOrden = :idOrden")
    Optional<OrdenCompra> buscarParaEnvioCorreo(@Param("idOrden") Integer idOrden);

    /**
     * Qué hace: Retorna todas las órdenes paginadas ordenadas descendentemente por ID.
     * A dónde apunta: Tabla `orden_compra`.
     */
    Page<OrdenCompra> findAllByOrderByIdOrdenDesc(Pageable pageable);
}

