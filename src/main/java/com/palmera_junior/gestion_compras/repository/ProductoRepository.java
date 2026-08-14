package com.palmera_junior.gestion_compras.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import com.palmera_junior.gestion_compras.entity.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer>{

    Optional<Producto> findByCodigoInventario(String codigoInventario);

    List<Producto> findByNombreContainingIgnoreCaseOrCodigoInventarioContainingIgnoreCase(String nombre, String codigoInventario);

    
}
