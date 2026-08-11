package com.palmera_junior.gestion_compras.service;

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

@Service
public class ProductoService {

   private final ProductoRepository productoRepository;
   private final PresentacionProductoService presentacionProductoService;

   public ProductoService(ProductoRepository productoRepository,
           PresentacionProductoService presentacionProductoService) {
       this.productoRepository = productoRepository;
       this.presentacionProductoService = presentacionProductoService;
   }

   public List<Producto> getAllProductos() {
       return productoRepository.findAll();
   }

   public Page<Producto> paginar(int page, int size) {
       return productoRepository.findAll(PageRequest.of(page, size));
   }

   public Producto buscarPorCodigo(String codigo) {
       return productoRepository.findByCodigoInventario(codigo).orElse(null);
   }

   public List<Producto> buscarPorTermino(String termino) {
       if (termino == null || termino.isBlank()) {
           return java.util.Collections.emptyList();
       }
       return productoRepository
               .findByNombreContainingIgnoreCaseOrCodigoInventarioContainingIgnoreCase(termino.trim(), termino.trim());
   }

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

   @Transactional
   public boolean eliminarProducto(Integer id) {
       if (!productoRepository.existsById(id)) {
           return false;
       }
       presentacionProductoService.eliminarPorProducto(id);
       productoRepository.deleteById(id);
       return true;
   }

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
