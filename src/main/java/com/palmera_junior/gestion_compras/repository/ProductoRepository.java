package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.palmera_junior.gestion_compras.entity.Producto;

/**
 * Repositorio JPA para el catálogo de entidades {@link Producto}.
 */
public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    Optional<Producto> findByCodigoInventario(String codigoInventario);

    List<Producto> findByNombreContainingIgnoreCaseOrCodigoInventarioContainingIgnoreCase(String nombre, String codigoInventario);

}