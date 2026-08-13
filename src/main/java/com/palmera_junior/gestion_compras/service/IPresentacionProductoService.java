package com.palmera_junior.gestion_compras.service;

import java.util.List;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;

public interface IPresentacionProductoService {
    List<PresentacionProducto> listarPorProducto(Integer idProducto);
    void eliminarPorProducto(Integer idProducto);
}
