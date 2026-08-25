package com.palmera_junior.gestion_compras.service.catalogo;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.repository.ProductoRepository;
import com.palmera_junior.gestion_compras.entity.Categoria;

/**
 * Implementación del servicio de catálogo de Productos y Presentaciones.
 * Coordina la persistencia en {@link ProductoRepository} y delega operaciones de presentaciones
 * a {@link PresentacionProductoService}.
 */
@Service
public class ProductoService implements IProductoService {

   private final ProductoRepository productoRepository;
   private final PresentacionProductoService presentacionProductoService;

   /**
    * Constructor con inyección de repositorios y servicios de catálogo.
    */
   public ProductoService(ProductoRepository productoRepository,
           PresentacionProductoService presentacionProductoService) {
       this.productoRepository = productoRepository;
       this.presentacionProductoService = presentacionProductoService;
   }

   /**
    * Qué hace: Retorna la lista total de productos registrados en base de datos.
    * A dónde apunta: {@link ProductoRepository#findAll()} -> tabla producto
    */
   @Override
   public List<Producto> getAllProductos() {
       return productoRepository.findAll();
   }

   /**
    * Qué hace: Retorna los productos paginados según página y tamaño solicitados.
    * A dónde apunta: {@link ProductoRepository#findAll(org.springframework.data.domain.Pageable)} -> tabla producto
    */
   @Override
   public Page<Producto> paginar(int page, int size) {
       return productoRepository.findAll(PageRequest.of(page, size));
   }

   /**
    * Qué hace: Busca un producto por su código único de inventario.
    * A dónde apunta: {@link ProductoRepository#findByCodigoInventario(String)} -> tabla producto
    */
   @Override
   public Producto buscarPorCodigo(String codigo) {
       return productoRepository.findByCodigoInventario(codigo).orElse(null);
   }

   /**
    * Qué hace: Búsqueda predictiva insensible a mayúsculas/minúsculas por coincidencia en nombre o código.
    * A dónde apunta: {@link ProductoRepository#findByNombreContainingIgnoreCaseOrCodigoInventarioContainingIgnoreCase(String, String)}
    */
   @Override
   public List<Producto> buscarPorTermino(String termino) {
       if (termino == null || termino.isBlank()) {
           return java.util.Collections.emptyList();
       }
       return productoRepository
               .findByNombreContainingIgnoreCaseOrCodigoInventarioContainingIgnoreCase(termino.trim(), termino.trim());
   }

   /**
    * Qué hace: Búsqueda paginada aplicando filtros de coincidencia en término.
    * A dónde apunta: {@link ProductoRepository}
    */
   @Override
   public Page<Producto> buscarConFiltros(String termino, String categoria, Boolean deleted, int page, int size) {
       // Implementación conservadora: si viene un término, buscar por nombre o código; si no, devolver paginado
       if (termino != null && !termino.isBlank()) {
           List<Producto> resultados = productoRepository
                   .findByNombreContainingIgnoreCaseOrCodigoInventarioContainingIgnoreCase(termino.trim(), termino.trim());
           // Convertir a Page de forma simple
           int start = Math.min(page * size, resultados.size());
           int end = Math.min(start + size, resultados.size());
           return new org.springframework.data.domain.PageImpl<>(resultados.subList(start, end), PageRequest.of(page, size), resultados.size());
       }
       return productoRepository.findAll(PageRequest.of(page, size));
   }

   /**
    * Qué hace: Crea o actualiza un producto y sus presentaciones comerciales asociadas.
    * A dónde apunta: {@link ProductoRepository#save(Object)} y {@link PresentacionProducto} en cascada.
    */
   @Override
   @Transactional
   public Producto guardarProducto(Integer idProducto, String codigoInventario, String nombre, String categoria,
           List<String> presentacionNombres, List<Integer> presentacionCantidades, List<String> presentacionUnidades,
           List<BigDecimal> presentacionPrecios) {
       codigoInventario = normalizar(codigoInventario);
       nombre = normalizar(nombre);
       if (!StringUtils.hasText(codigoInventario) || !StringUtils.hasText(nombre)) {
           throw new IllegalArgumentException("El código de inventario y el nombre del producto son obligatorios.");
       }

       if (idProducto == null) {
           if (productoRepository.findByCodigoInventario(codigoInventario).isPresent()) {
               throw new IllegalArgumentException("Ya existe un producto con ese código de inventario.");
           }
           Producto producto = new Producto();
           producto.setCodigoInventario(codigoInventario);
           producto.setNombre(nombre);
           producto.setCategoria(categoria != null ? Categoria.valueOf(categoria) : null);
           productoRepository.save(producto);
           aplicarPresentaciones(producto, presentacionNombres, presentacionCantidades, presentacionUnidades,
                   presentacionPrecios);
           return productoRepository.save(producto);
       }

       Producto producto = productoRepository.findById(idProducto)
               .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado para actualizar."));
       if (productoRepository.findByCodigoInventario(codigoInventario)
               .filter(p -> !p.getIdProducto().equals(idProducto)).isPresent()) {
           throw new IllegalArgumentException("Hay otro producto con el mismo código de inventario.");
       }
       producto.setCodigoInventario(codigoInventario);
       producto.setNombre(nombre);
       producto.setCategoria(categoria != null ? Categoria.valueOf(categoria) : null);
       producto.getPresentaciones().clear();
       aplicarPresentaciones(producto, presentacionNombres, presentacionCantidades, presentacionUnidades,
               presentacionPrecios);
       return productoRepository.save(producto);
   }

   /**
    * Qué hace: Elimina un producto y sus presentaciones vinculadas por ID.
    * A dónde apunta: {@link PresentacionProductoService#eliminarPorProducto(Integer)} y {@link ProductoRepository#deleteById(Object)}
    */
   @Override
   @Transactional
   public boolean eliminarProducto(Integer id) {
       if (!productoRepository.existsById(id)) {
           return false;
       }
       presentacionProductoService.eliminarPorProducto(id);
       productoRepository.deleteById(id);
       return true;
   }

   /**
    * Qué hace: Consulta las presentaciones registradas para un producto.
    * A dónde apunta: {@link PresentacionProductoService#listarPorProducto(Integer)}
    */
   @Override
   public List<PresentacionProducto> obtenerPresentacionesProducto(Integer id) {
       return presentacionProductoService.listarPorProducto(id);
   }


   private void aplicarPresentaciones(Producto producto, List<String> nombres, List<Integer> cantidades,
           List<String> unidades, List<BigDecimal> precios) {
       int maxFilas = 0;
       if (nombres != null) {
           maxFilas = Math.max(maxFilas, nombres.size());
       }
       if (cantidades != null) {
           maxFilas = Math.max(maxFilas, cantidades.size());
       }
       if (unidades != null) {
           maxFilas = Math.max(maxFilas, unidades.size());
       }
       if (precios != null) {
           maxFilas = Math.max(maxFilas, precios.size());
       }

       boolean hayAlgunaFilaConDatos = false;

       for (int i = 0; i < maxFilas; i++) {
           String nombrePres = nombres != null && i < nombres.size() && nombres.get(i) != null
                   ? nombres.get(i).trim()
                   : null;
           String unidadPres = unidades != null && i < unidades.size() && unidades.get(i) != null
                   ? unidades.get(i).trim()
                   : null;
           Integer cantidadPres = cantidades != null && i < cantidades.size() && cantidades.get(i) != null
                   ? cantidades.get(i)
                   : null;
           BigDecimal precioPres = precios != null && i < precios.size() && precios.get(i) != null
                   ? precios.get(i)
                   : null;

           boolean tieneDatos = (nombrePres != null && !nombrePres.isBlank())
                   || (unidadPres != null && !unidadPres.isBlank())
                   || cantidadPres != null
                   || precioPres != null;
           if (!tieneDatos) {
               continue;
           }
           hayAlgunaFilaConDatos = true;

           String nombreFinal = nombrePres != null && !nombrePres.isBlank() ? nombrePres : "";
           PresentacionProducto pres = new PresentacionProducto();
           pres.setPresentacion(nombreFinal);
           pres.setCantidad(cantidadPres != null && cantidadPres != 1 ? cantidadPres : null);
           pres.setUnidad(unidadPres != null && !unidadPres.isBlank() ? unidadPres : "Und");
           pres.setPrecio(precioPres != null ? precioPres : BigDecimal.ZERO);
           producto.addPresentacion(pres);
       }

       if (!hayAlgunaFilaConDatos) {
           PresentacionProducto pres = new PresentacionProducto();
           pres.setPresentacion("");
           pres.setCantidad(null);
           pres.setUnidad("Und");
           pres.setPrecio(BigDecimal.ZERO);
           producto.addPresentacion(pres);
       }
   }

   private String normalizar(String value) {
       return value == null ? null : value.trim();
   }
}
