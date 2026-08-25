package com.palmera_junior.gestion_compras.service.catalogo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;
import com.palmera_junior.gestion_compras.entity.Producto;

public interface IProductoService {
    List<Producto> getAllProductos();
    Page<Producto> paginar(int page, int size);
    Producto buscarPorCodigo(String codigo);
    List<Producto> buscarPorTermino(String termino);
    Producto guardarProducto(Integer idProducto, String codigoInventario, String nombre, String categoria,
            List<String> presentacionNombres, List<Integer> presentacionCantidades, List<String> presentacionUnidades,
            List<BigDecimal> presentacionPrecios);
    boolean eliminarProducto(Integer id);
    List<PresentacionProducto> obtenerPresentacionesProducto(Integer id);

    /**
     * Qué hace:
     * Realiza una búsqueda paginada aplicando filtros opcionales de término, categoría y estado.
     * 
     * A dónde apunta:
     * - Consulta tabla: producto
     * 
     * @param termino Texto de búsqueda.
     * @param categoria Categoría a filtrar.
     * @param deleted Filtro de borrado lógico.
     * @param page Índice de página.
     * @param size Tamaño de página.
     * @return Página de productos filtrados.
     */
    Page<Producto> buscarConFiltros(String termino, String categoria, Boolean deleted, int page, int size);
}
