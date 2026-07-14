package com.palmera_junior.gestion_compras.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.palmera_junior.gestion_compras.entity.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer>{
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);
    
    List<Usuario> findBySedeIdSede(Integer idSede);
    
    boolean existsByNombreUsuario(String nombreUsuario);
    
    boolean existsByCedula(String cedula);
    
}
