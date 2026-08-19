package com.palmera_junior.gestion_compras.controller.admin;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.service.catalogo.IProveedorService;

// Único responsable del CRUD y la paginación de Proveedores dentro del panel de administración.
@Controller
public class ProveedorAdminController {

    private final IProveedorService proveedorService;

    public ProveedorAdminController(IProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    // Endpoint AJAX para paginar la tabla de Proveedores sin recargar la página
    @GetMapping("/admin/proveedores/pagina")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String paginaProveedores(Model model,
            @RequestParam(defaultValue = "0") int pageProveedores,
            @RequestParam(defaultValue = "10") int size) {
        Page<Proveedor> proveedoresPage = proveedorService.paginar(pageProveedores, size);
        model.addAttribute("proveedoresPage", proveedoresPage);
        model.addAttribute("pageProveedores", pageProveedores);
        model.addAttribute("size", size);
        return "admin :: proveedoresFragment";
    }

    @PostMapping("/admin/proveedores")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarProveedor(@RequestParam(required = false) Integer idProv,
            @RequestParam String nit,
            @RequestParam String nombre,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String direccion,
            @RequestParam(required = false) String telefono,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) List<Integer> sedeIds,
            RedirectAttributes redirectAttributes) {

        try {
            proveedorService.guardar(idProv, nit, nombre, ciudad, direccion, telefono, correo, sedeIds);
            redirectAttributes.addAttribute("success",
                    idProv == null ? "Proveedor creado correctamente." : "Proveedor actualizado correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        }
    }

    @PostMapping("/admin/proveedores/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteProveedor(@PathVariable Integer id) {
        try {
            if (!proveedorService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Proveedor no encontrado"));
            }
            return ResponseEntity.ok(Map.of("success", "Proveedor eliminado"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar el proveedor porque está asociado a órdenes de compra u otros registros."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar proveedor: " + dae.getMostSpecificCause()));
        }
    }
}
