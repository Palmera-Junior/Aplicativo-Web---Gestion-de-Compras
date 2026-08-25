package com.palmera_junior.gestion_compras.controller.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.palmera_junior.gestion_compras.entity.PresentacionProducto;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.service.catalogo.IProductoService;

/**
 * Controlador administrativo para la gestión del catálogo de Productos y sus Presentaciones.
 * Gestiona operaciones CRUD, paginación dinámica por fragmentos Thymeleaf y consulta AJAX de presentaciones.
 */
@Controller
public class ProductoAdminController {

    private final IProductoService productoService;

    /**
     * Constructor para inyección de dependencias del servicio de productos.
     */
    public ProductoAdminController(IProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * Qué hace:
     * Retorna el fragmento HTML Thymeleaf correspondiente a la tabla de productos paginada
     * para actualización asíncrona (AJAX) sin refrescar la página completa.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /admin/productos/pagina
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IProductoService#paginar(int, int)} -> ProductoRepository
     * - Vista Thymeleaf: Fragmento "admin :: productosFragment"
     * 
     * @param model Modelo para inyectar la página de productos y controles.
     * @param pageProductos Índice de página a consultar.
     * @param size Cantidad de registros por página.
     * @return Fragmento Thymeleaf `admin :: productosFragment`.
     */
    @GetMapping("/admin/productos/pagina")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String paginaProductos(Model model,
            @RequestParam(defaultValue = "0") int pageProductos,
            @RequestParam(defaultValue = "10") int size) {

        Page<Producto> productosPage = productoService.paginar(pageProductos, size);

        model.addAttribute("productosPage", productosPage);
        model.addAttribute("pageProductos", pageProductos);
        model.addAttribute("size", size);

        return "admin :: productosFragment";
    }

    /**
     * Qué hace:
     * Crea un nuevo producto o actualiza uno existente junto con su lista de presentaciones,
     * cantidades, unidades y precios en una sola transacción.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/productos
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IProductoService#guardarProducto} -> ProductoRepository, PresentacionProductoRepository
     * - Redirección: "redirect:/admin" con atributos de feedback (success/error).
     * 
     * @param idProducto ID del producto si es edición; null si es creación.
     * @param codigoInventario Código único del producto.
     * @param nombre Nombre del producto.
     * @param categoria Categoría del catálogo.
     * @param redirectAttributes Atributos para mensajes temporales.
     * @param presentacionNombres Lista de nombres de presentaciones (p.ej. "Caja", "Galón").
     * @param presentacionCantidades Lista de cantidades por presentación.
     * @param presentacionUnidades Lista de unidades de medida (p.ej. "ml", "g").
     * @param presentacionPrecios Lista de precios unitarios por presentación.
     * @return Redirección a la vista de administración.
     */
    @PostMapping("/admin/productos")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarProducto(@RequestParam(required = false) Integer idProducto,
            @RequestParam String codigoInventario,
            @RequestParam String nombre,
            @RequestParam(required = false) String categoria,
            RedirectAttributes redirectAttributes,
            @RequestParam(required = false) List<String> presentacionNombres,
            @RequestParam(required = false) List<Integer> presentacionCantidades,
            @RequestParam(required = false) List<String> presentacionUnidades,
            @RequestParam(required = false) List<BigDecimal> presentacionPrecios) {

        try {
            productoService.guardarProducto(idProducto, codigoInventario, nombre, categoria, presentacionNombres,
                    presentacionCantidades, presentacionUnidades, presentacionPrecios);
            redirectAttributes.addAttribute("success",
                    idProducto == null ? "Producto creado correctamente." : "Producto actualizado correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        } catch (DataAccessException e) {
            String causa = e.getMostSpecificCause() != null && e.getMostSpecificCause().getMessage() != null
                    ? e.getMostSpecificCause().getMessage()
                    : e.getMessage();
            redirectAttributes.addAttribute("error",
                    "No se pudo guardar el producto. Detalle: " + causa);
            return "redirect:/admin";
        } catch (Exception e) {
            redirectAttributes.addAttribute("error",
                    "Ocurrió un error inesperado al guardar el producto: " + e.getMessage());
            return "redirect:/admin";
        }
    }

    /**
     * Qué hace:
     * Elimina un producto del catálogo por su ID, validando que no tenga referencias activas en órdenes de compra.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/productos/delete/{id}
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IProductoService#eliminarProducto(Integer)} -> ProductoRepository
     * - Retorno: JSON Map con status HTTP correspondiente (200, 404, 409 o 500).
     * 
     * @param id Identificador numérico del producto a eliminar.
     * @return {@link ResponseEntity} indicando el resultado.
     */
    @PostMapping("/admin/productos/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteProducto(@PathVariable Integer id) {
        try {
            if (!productoService.eliminarProducto(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Producto no encontrado"));
            }
            return ResponseEntity.ok(Map.of("success", "Producto eliminado"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar el producto porque está asociado a órdenes u otros registros."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar producto: " + dae.getMostSpecificCause()));
        }
    }

    /**
     * Qué hace:
     * Obtiene la lista de presentaciones asociadas a un producto en formato JSON para precargarlas en el formulario de edición.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /admin/producto/{id}/presentaciones
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IProductoService#obtenerPresentacionesProducto(Integer)} -> PresentacionProductoRepository
     * - Retorno: Arreglo JSON de {@link PresentacionProducto}.
     * 
     * @param id Identificador del producto.
     * @return Lista JSON de presentaciones.
     */
    @GetMapping("/admin/producto/{id}/presentaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseBody
    public List<PresentacionProducto> obtenerPresentacionesProducto(@PathVariable Integer id) {
        return productoService.obtenerPresentacionesProducto(id);
    }
}

