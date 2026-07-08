package com.palmera_junior.gestion_compras.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.palmera_junior.gestion_compras.entity.Proveedor;

public interface ProveedorRepository
        extends JpaRepository<Proveedor, Long> {

}

 