package com.palmera_junior.gestion_compras.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.repository.OrdenCompraRepository;

@Service
public class OrdenCompraService {

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    public Page<OrdenCompra> ordenesDeCompraPaginadas(Pageable pageable) {
        return ordenCompraRepository.findAll(pageable);
    }

 
}
