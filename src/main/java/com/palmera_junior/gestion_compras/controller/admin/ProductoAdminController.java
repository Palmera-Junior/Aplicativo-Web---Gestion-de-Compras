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

// Único responsable del CRUD y la paginación de Productos dentro del panel de administración.
@Controller
public class ProductoAdminController {

    private final IProductoService productoService;

    public ProductoAdminController(IProductoService productoService) {
        this.productoService = productoService;
    }

    // Endpoint AJAX para paginar la tabla de productos sin recargar la página
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

    // Endpoint para cargar las presentaciones de un producto al editar en admin
    @GetMapping("/admin/producto/{id}/presentaciones")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseBody
    public List<PresentacionProducto> obtenerPresentacionesProducto(@PathVariable Integer id) {
        return productoService.obtenerPresentacionesProducto(id);
    }
}
