package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("select p from Proveedor p join p.sedes s where s.idSede = :idSede "
            + "and (lower(p.nombre) like lower(concat('%', :termino, '%')) "
            + "or cast(p.nit as string) like concat('%', :termino, '%')) order by p.nombre")
    List<Proveedor> buscarPorNombreONitEnSede(@Param("idSede") Integer idSede,
            @Param("termino") String termino, Pageable pageable);

    /**
     * Qué hace: Busca proveedores a nivel nacional por coincidencia parcial en nombre o NIT,
     * ordenando los resultados por nombre para facilitar la selección en formularios y búsquedas globales.
     * A dónde apunta: Tabla `proveedor`.
     */
    @Query("select p from Proveedor p where lower(p.nombre) like lower(concat('%', :termino, '%')) "
            + "or cast(p.nit as string) like concat('%', :termino, '%') order by p.nombre")
    List<Proveedor> buscarPorNombreONit(@Param("termino") String termino, Pageable pageable);

    /**
     * Qué hace: Verifica si un proveedor con determinado NIT ya está asociado a una sede específica.
     * A dónde apunta: Tablas `proveedor` y `proveedor_sede`.
     */
    boolean existsByNitAndSedesIdSede(Long nit, Integer idSede);

    /**
     * Qué hace: Busca un proveedor por su NIT ignorando mayúsculas/minúsculas.
     * A dónde apunta: Tabla `proveedor`.
     */
    Optional<Proveedor> findByNit(Long nit);

    /**
     * Qué hace: Verifica si existe un proveedor registrado con el NIT especificado.
     * A dónde apunta: Tabla `proveedor`.
     */
    boolean existsByNit(Long nit);
}
