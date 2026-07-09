package com.palmera_junior.gestion_compras.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.repository.ProveedorRepository;

@Service
public class ProveedorService {

    private final ProveedorRepository repository;

    public ProveedorService(
        ProveedorRepository repository
    ) {
        this.repository=repository;
    }

    public List<Proveedor>Listar(){
        return repository.findAll();
    }

}


 

