package com.palmera_junior.gestion_compras.service.catalogo;

import java.util.List;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;

/**
 * Contrato de servicio para la gestión de presentaciones comerciales de productos.
 */
public interface IPresentacionProductoService {

    /**
     * Qué hace:
     * Obtiene la lista de presentaciones asociadas a un producto específico.
     * 
     * A dónde apunta:
     * - Consulta tabla: presentacion_producto (mediante PresentacionProductoRepository)
     * 
     * @param idProducto Identificador del producto.
     * @return Lista de presentaciones asociadas.
     */
    List<PresentacionProducto> listarPorProducto(Integer idProducto);

    /**
     * Qué hace:
     * Elimina todas las presentaciones comerciales vinculadas a un producto específico.
     * 
     * A dónde apunta:
     * - Modifica tabla: presentacion_producto (mediante PresentacionProductoRepository)
     * 
     * @param idProducto Identificador del producto.
     */
    void eliminarPorProducto(Integer idProducto);
}

