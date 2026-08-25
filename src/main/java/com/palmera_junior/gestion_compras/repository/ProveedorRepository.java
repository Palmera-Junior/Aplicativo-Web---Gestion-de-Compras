package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.palmera_junior.gestion_compras.entity.Proveedor;

/**
 * Repositorio JPA para la administración de entidades {@link Proveedor} y sus sedes asociadas.
 */
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    /**
     * Qué hace: Lista los proveedores habilitados para una sede ordenados alfabéticamente.
     * A dónde apunta: Tabla `proveedor` cruzada con `proveedor_sede` por `id_sede`.
     */
    List<Proveedor> findBySedesIdSedeOrderByNombreAsc(Integer idSede);

    /**
     * Qué hace: Búsqueda predictiva de proveedores filtrados por sede y coincidencia parcial de nombre.
     * A dónde apunta: Tabla `proveedor` y relación con sede.
     */
    List<Proveedor> findBySedesIdSedeAndNombreContainingIgnoreCase(Integer idSede, String nombre);

    /**
     * Qué hace: Verifica si un proveedor con determinado NIT ya está asociado a una sede específica.
     * A dónde apunta: Tablas `proveedor` y `proveedor_sede`.
     */
    boolean existsByNitAndSedesIdSede(String nit, Integer idSede);

    /**
     * Qué hace: Busca un proveedor por su NIT ignorando mayúsculas/minúsculas.
     * A dónde apunta: Tabla `proveedor`.
     */
    Optional<Proveedor> findByNitIgnoreCase(String nit);

    /**
     * Qué hace: Verifica si existe un proveedor registrado con el NIT especificado.
     * A dónde apunta: Tabla `proveedor`.
     */
    boolean existsByNitIgnoreCase(String nit);
}