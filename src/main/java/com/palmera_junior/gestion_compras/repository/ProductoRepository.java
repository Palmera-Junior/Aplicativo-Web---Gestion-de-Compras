package com.palmera_junior.gestion_compras.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.palmera_junior.gestion_compras.entity.Producto;

@Repository

public interface ProductoRepository extends JpaRepository<Producto, Long>{

    Producto findByCodigoInventario(String codigoInventario);

    List<Producto> findByNombreContainingIgnoreCaseOrCodigoInventarioContainingIgnoreCase(String nombre, String codigoInventario);

    
}
