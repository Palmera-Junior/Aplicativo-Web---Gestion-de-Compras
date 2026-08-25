package com.palmera_junior.gestion_compras.controller.admin;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.palmera_junior.gestion_compras.service.organizacion.ICentroCostoService;

/**
 * Controlador administrativo para la gestión CRUD de Centros de Costo.
 * Permite crear, actualizar y eliminar centros de costo asociados a sedes organizacionales.
 */
@Controller
public class CentroCostoAdminController {

    private final ICentroCostoService centroCostoService;

    /**
     * Constructor para inyección de dependencias del servicio de centros de costo.
     */
    public CentroCostoAdminController(ICentroCostoService centroCostoService) {
        this.centroCostoService = centroCostoService;
    }

    /**
     * Qué hace:
     * Crea un nuevo centro de costo o actualiza uno existente asociándolo a una sede específica.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/centros-costo
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link ICentroCostoService#guardar(Integer, String, Integer, String, String)} -> CentroCostoRepository
     * - Redirección: "redirect:/admin" con parámetros flash de feedback (success/error).
     * 
     * @param idCentroCosto ID del centro de costo si es edición; null si es creación.
     * @param nombre Nombre descriptivo del centro de costo.
     * @param sedeId ID de la sede a la que pertenece.
     * @param codigo Código alfanumérico identificador.
     * @param direccion Dirección física o ubicación.
     * @param redirectAttributes Atributos para mensajes temporales tras la redirección.
     * @return Redirección a la vista principal de administración.
     */
    @PostMapping("/admin/centros-costo")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarCentroCosto(@RequestParam(required = false) Integer idCentroCosto,
            @RequestParam String nombre,
            @RequestParam Integer sedeId,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String direccion,
            RedirectAttributes redirectAttributes) {

        try {
            centroCostoService.guardar(idCentroCosto, nombre, sedeId, codigo, direccion);
            redirectAttributes.addAttribute("success",
                    idCentroCosto == null ? "Centro de costo creado correctamente." : "Centro de costo actualizado correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        }
    }

    /**
     * Qué hace:
     * Elimina un centro de costo por su ID, validando que no tenga restricciones de integridad referencial.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/centros-costo/delete/{id}
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link ICentroCostoService#eliminar(Integer)} -> CentroCostoRepository
     * - Retorno: JSON Map con status 200 OK, 404 Not Found o 409 Conflict si está en uso.
     * 
     * @param id Identificador numérico del centro de costo a eliminar.
     * @return {@link ResponseEntity} con mensaje JSON de éxito o error.
     */
    @PostMapping("/admin/centros-costo/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteCentro(@PathVariable Integer id) {
        try {
            if (!centroCostoService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Centro no encontrado"));
            }
            return ResponseEntity.ok(Map.of("success", "Centro eliminado"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar el centro de costo porque está asociado a órdenes de compra u otros registros."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar centro: " + dae.getMostSpecificCause()));
        }
    }
}

