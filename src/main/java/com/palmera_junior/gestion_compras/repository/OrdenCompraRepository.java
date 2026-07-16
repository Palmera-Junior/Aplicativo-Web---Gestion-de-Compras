package com.palmera_junior.gestion_compras.repository;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, Integer>, JpaSpecificationExecutor<OrdenCompra> {

    Page<OrdenCompra> findBySedeIdSedeOrderByFechaDesc(Integer idSede, Pageable pageable);

    Optional<OrdenCompra> findByNumeroOrden(String numeroOrden);

    Page<OrdenCompra> findBySedeIdSedeAndNumeroOrdenContainingIgnoreCase(Integer idSede, String numeroOrden, Pageable pageable);



    
}
