package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;

/**
 * Repositorio JPA para la persistencia de las presentaciones comerciales de los productos.
 */
public interface PresentacionProductoRepository extends JpaRepository<PresentacionProducto, Integer> {

    /**
     * Qué hace: Consulta las presentaciones asociadas a un producto.
     * A dónde apunta: Tabla `presentacion_producto` por `id_producto`.
     */
    List<PresentacionProducto> findByProductoIdProducto(Integer idProducto);

    /**
     * Qué hace: Busca una presentación por su ID único.
     * A dónde apunta: Tabla `presentacion_producto`.
     */
    Optional<PresentacionProducto> findByIdPresentacion(Integer idPresentacion);

    /**
     * Qué hace: Elimina todas las presentaciones vinculadas a un producto.
     * A dónde apunta: Sentencia DELETE en tabla `presentacion_producto`.
     */
    void deleteByProductoIdProducto(Integer idProducto);
}

