package com.palmera_junior.gestion_compras.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.palmera_junior.gestion_compras.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    @Query("""
                SELECT u
                FROM Usuario u
                LEFT JOIN FETCH u.sede
                WHERE u.email = :email
            """)
    Optional<Usuario> findByEmailConSede(
            @Param("email") String email);

    List<Usuario> findBySedeIdSede(Integer idSede);

    Optional<Usuario> findByEmail(String email);

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByCedula(String cedula);

    Optional<Usuario> findByCedula(String cedula);

}
