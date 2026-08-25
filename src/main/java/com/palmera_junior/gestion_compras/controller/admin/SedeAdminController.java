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

import com.palmera_junior.gestion_compras.service.organizacion.ISedeService;

/**
 * Controlador administrativo para la gestión CRUD de Sedes organizacionales.
 * Permite registrar nuevas sedes geográficas, actualizar prefijos de ciudad y direcciones, y eliminarlas.
 */
@Controller
public class SedeAdminController {

    private final ISedeService sedeService;

    /**
     * Constructor para inyección de dependencias del servicio de sedes.
     */
    public SedeAdminController(ISedeService sedeService) {
        this.sedeService = sedeService;
    }

    /**
     * Qué hace:
     * Registra una nueva sede o actualiza una existente con su nombre, prefijo de ciudad (usado para consecutivos de órdenes)
     * y dirección principal.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/sedes
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link ISedeService#guardar(Integer, String, String, String)} -> SedeRepository
     * - Redirección: "redirect:/admin" con mensajes flash.
     * 
     * @param idSede ID de la sede si se actualiza; null si se crea.
     * @param nombre Nombre de la sede (p.ej. "Bucaramanga").
     * @param prefijoCiudad Prefijo abreviado (p.ej. "BUC").
     * @param direccion Dirección física de la sede.
     * @param redirectAttributes Atributos para mensajes temporales.
     * @return Redirección a la vista de administración.
     */
    @PostMapping("/admin/sedes")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarSede(@RequestParam(required = false) Integer idSede,
            @RequestParam String nombre,
            @RequestParam String prefijoCiudad,
            @RequestParam(required = false) String direccion,
            RedirectAttributes redirectAttributes) {

        try {
            sedeService.guardar(idSede, nombre, prefijoCiudad, direccion);
            redirectAttributes.addAttribute("success",
                    idSede == null ? "Sede creada correctamente." : "Sede actualizada correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        }
    }

    /**
     * Qué hace:
     * Elimina una sede por su ID verificando que no posea usuarios, centros de costo u órdenes vinculadas.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/sedes/delete/{id}
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link ISedeService#eliminar(Integer)} -> SedeRepository
     * - Retorno: JSON Map con status 200, 404, 409 o 500.
     * 
     * @param id Identificador numérico de la sede a eliminar.
     * @return {@link ResponseEntity} con la respuesta.
     */
    @PostMapping("/admin/sedes/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteSede(@PathVariable Integer id) {
        try {
            if (!sedeService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Sede no encontrada"));
            }
            return ResponseEntity.ok(Map.of("success", "Sede eliminada"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar la sede porque tiene centros, órdenes o recursos asociados."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar sede: " + dae.getMostSpecificCause()));
        }
    }
}

