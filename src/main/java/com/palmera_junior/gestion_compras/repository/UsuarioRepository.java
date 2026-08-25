package com.palmera_junior.gestion_compras.repository;

import java.util.*;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.palmera_junior.gestion_compras.entity.Usuario;

/**
 * Repositorio JPA para la entidad {@link Usuario}.
 * Proporciona métodos de autenticación, carga eager de sedes y validaciones de unicidad.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    /**
     * Qué hace: Busca un usuario por su nombre de usuario de inicio de sesión.
     * A dónde apunta: Tabla `usuario`.
     */
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    /**
     * Qué hace: Carga un usuario por su correo electrónico uniendo tempranamente (JOIN FETCH) su sede.
     * A dónde apunta: Tablas `usuario` y `sede`.
     */
    @Query("""
                SELECT u
                FROM Usuario u
                LEFT JOIN FETCH u.sede
                WHERE u.email = :email
            """)
    Optional<Usuario> findByEmailConSede(@Param("email") String email);

    /**
     * Qué hace: Lista los usuarios asociados a una sede específica.
     * A dónde apunta: Tabla `usuario` por `id_sede`.
     */
    List<Usuario> findBySedeIdSede(Integer idSede);

    /**
     * Qué hace: Busca un usuario por su correo electrónico exacto.
     * A dónde apunta: Tabla `usuario`.
     */
    Optional<Usuario> findByEmail(String email);

    /**
     * Qué hace: Verifica si ya existe un usuario con el nombre de usuario dado.
     * A dónde apunta: Tabla `usuario`.
     */
    boolean existsByNombreUsuario(String nombreUsuario);

    /**
     * Qué hace: Verifica si ya existe un usuario con la cédula dada.
     * A dónde apunta: Tabla `usuario`.
     */
    boolean existsByCedula(String cedula);

    /**
     * Qué hace: Busca un usuario por su número de cédula.
     * A dónde apunta: Tabla `usuario`.
     */
    Optional<Usuario> findByCedula(String cedula);
}

