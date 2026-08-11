package com.palmera_junior.gestion_compras.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;
import com.palmera_junior.gestion_compras.repository.PresentacionProductoRepository;

@Service
public class PresentacionProductoService {

    private final PresentacionProductoRepository presentacionProductoRepository;

    public PresentacionProductoService(PresentacionProductoRepository presentacionProductoRepository) {
        this.presentacionProductoRepository = presentacionProductoRepository;
    }

    public List<PresentacionProducto> listarPorProducto(Integer idProducto) {
        return presentacionProductoRepository.findByProductoIdProducto(idProducto);
    }

    @Transactional
    public void eliminarPorProducto(Integer idProducto) {
        presentacionProductoRepository.deleteByProductoIdProducto(idProducto);
    }
}
