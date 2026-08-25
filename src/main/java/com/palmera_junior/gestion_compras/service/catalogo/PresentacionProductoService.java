package com.palmera_junior.gestion_compras.service.catalogo;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;
import com.palmera_junior.gestion_compras.repository.PresentacionProductoRepository;

/**
 * Implementación del servicio de gestión de presentaciones comerciales de productos.
 */
@Service
public class PresentacionProductoService implements IPresentacionProductoService {

    private final PresentacionProductoRepository presentacionProductoRepository;

    /**
     * Constructor para inyección del repositorio de presentaciones.
     */
    public PresentacionProductoService(PresentacionProductoRepository presentacionProductoRepository) {
        this.presentacionProductoRepository = presentacionProductoRepository;
    }

    /**
     * Qué hace:
     * Consulta y retorna todas las presentaciones registradas para un producto determinado.
     * 
     * A dónde apunta:
     * - Repositorio: {@link PresentacionProductoRepository#findByProductoIdProducto(Integer)}
     * - Tabla JPA: presentacion_producto
     * 
     * @param idProducto Identificador del producto.
     * @return Lista de entidades PresentacionProducto.
     */
    @Override
    public List<PresentacionProducto> listarPorProducto(Integer idProducto) {
        return presentacionProductoRepository.findByProductoIdProducto(idProducto);
    }

    /**
     * Qué hace:
     * Elimina en una transacción todas las presentaciones de un producto antes de reinsertar las actualizadas.
     * 
     * A dónde apunta:
     * - Repositorio: {@link PresentacionProductoRepository#deleteByProductoIdProducto(Integer)}
     * - Tabla JPA: presentacion_producto
     * 
     * @param idProducto Identificador del producto.
     */
    @Override
    @Transactional
    public void eliminarPorProducto(Integer idProducto) {
        presentacionProductoRepository.deleteByProductoIdProducto(idProducto);
    }
}

