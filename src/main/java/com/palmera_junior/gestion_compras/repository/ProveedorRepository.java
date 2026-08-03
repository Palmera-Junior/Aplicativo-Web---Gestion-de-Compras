package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.palmera_junior.gestion_compras.entity.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {

    List<Proveedor> findBySedesIdSedeOrderByNombreAsc(Integer idSede);

    List<Proveedor> findBySedesIdSedeAndNombreContainingIgnoreCase(Integer idSede, String nombre);

    boolean existsByNitAndSedesIdSede(String nit, Integer idSede);

    Optional<Proveedor> findByNitIgnoreCase(String nit);

    boolean existsByNitIgnoreCase(String nit);

}


                
    



 