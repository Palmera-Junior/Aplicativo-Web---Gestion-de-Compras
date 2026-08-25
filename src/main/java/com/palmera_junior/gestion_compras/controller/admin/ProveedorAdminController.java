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

/**
 * Controlador administrativo para la gestión del catálogo de Proveedores.
 * Permite listar, paginar asíncronamente, crear, actualizar y eliminar proveedores y sus sedes asociadas.
 */
@Controller
public class ProveedorAdminController {

    private final IProveedorService proveedorService;

    /**
     * Constructor para inyección de dependencias del servicio de proveedores.
     */
    public ProveedorAdminController(IProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    /**
     * Qué hace:
     * Retorna el fragmento HTML Thymeleaf con la tabla paginada de proveedores para refresco asíncrono en cliente.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /admin/proveedores/pagina
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IProveedorService#paginar(int, int)} -> ProveedorRepository
     * - Fragmento renderizado: "admin :: proveedoresFragment"
     * 
     * @param model Modelo para inyectar la página y metadatos.
     * @param pageProveedores Índice de página.
     * @param size Cantidad de elementos por página.
     * @return Fragmento Thymeleaf `admin :: proveedoresFragment`.
     */
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

    /**
     * Qué hace:
     * Crea un nuevo proveedor o actualiza uno existente con sus datos de contacto (NIT, nombre, ciudad,
     * dirección, teléfono, correo) y el listado de sedes con las que opera comercialmente.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/proveedores
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IProveedorService#guardar} -> ProveedorRepository, SedeRepository
     * - Redirección: "redirect:/admin" con parámetros de feedback (success/error).
     * 
     * @param idProv ID del proveedor si se actualiza; null si se crea.
     * @param nit Número de Identificación Tributaria del proveedor.
     * @param nombre Razón social o nombre comercial.
     * @param ciudad Ciudad de ubicación.
     * @param direccion Dirección fiscal/física.
     * @param telefono Teléfono de contacto.
     * @param correo Correo electrónico para envío de órdenes.
     * @param sedeIds Lista de IDs de sedes asignadas al proveedor.
     * @param redirectAttributes Atributos flash para mensajes temporales.
     * @return Redirección a la vista de administración.
     */
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

    /**
     * Qué hace:
     * Elimina un proveedor por su ID, validando que no tenga órdenes de compra asociadas.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/proveedores/delete/{id}
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IProveedorService#eliminar(Integer)} -> ProveedorRepository
     * - Retorno: JSON Map con status 200 OK, 404 Not Found o 409 Conflict si está referenciado.
     * 
     * @param id Identificador numérico del proveedor.
     * @return {@link ResponseEntity} indicando el resultado.
     */
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

