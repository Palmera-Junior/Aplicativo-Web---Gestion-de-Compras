package com.palmera_junior.gestion_compras.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.palmera_junior.gestion_compras.entity.Sede;

/**
 * Repositorio JPA para la entidad {@link Sede}.
 */
public interface SedeRepository extends JpaRepository<Sede, Integer> {

    /**
     * Qué hace: Busca una sede por nombre ignorando mayúsculas y minúsculas.
     * Se utiliza para localizar la sede central o una sede específica sin depender del formato exacto del texto.
     * A dónde apunta: Tabla `sede`.
     */
    Optional<Sede> findByNombreIgnoreCase(String nombre);

    /**
     * Qué hace: Verifica si existe una sede con el mismo nombre (case-insensitive).
     * A dónde apunta: Tabla `sede`.
     */
    boolean existsByNombreIgnoreCase(String nombre);
    boolean existsByPrefijoCiudadIgnoreCase(String prefijoCiudad);
}
