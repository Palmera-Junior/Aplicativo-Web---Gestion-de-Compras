package com.palmera_junior.gestion_compras.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.palmera_junior.gestion_compras.entity.Sede;

/**
 * Repositorio JPA para la entidad {@link Sede}.
 */
public interface SedeRepository extends JpaRepository<Sede, Integer> {

    /**
     * Qué hace: Verifica si existe una sede con el mismo nombre (case-insensitive).
     * A dónde apunta: Tabla `sede`.
     */
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByPrefijoCiudadIgnoreCase(String prefijoCiudad);
}
