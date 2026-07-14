package com.palmera_junior.gestion_compras.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.palmera_junior.gestion_compras.entity.Proveedor;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Integer> {
        
                List<Proveedor> findBySedeIdSedeOrderByNombreAsc(Integer idSede);

                List<Proveedor> findBySedeIdSedeAndNombreContainingIgnoreCase(Integer idSede, String nombre);

                boolean existsByNitAndSedeIdSede(String nit, Integer idSede);


                }


                
    



 