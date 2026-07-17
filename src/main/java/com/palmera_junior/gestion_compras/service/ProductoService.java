package com.palmera_junior.gestion_compras.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.repository.ProductoRepository;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    public List<Producto> getAllProductos() {
        return productoRepository.findAll();
    
    }

    public Producto buscarPorCodigo(String codigo) {
        return productoRepository.findByCodigoInventario(codigo);
    }

}
