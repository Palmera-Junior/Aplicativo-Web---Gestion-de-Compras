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

// Único responsable del CRUD de Sedes dentro del panel de administración.
@Controller
public class SedeAdminController {

    private final ISedeService sedeService;

    public SedeAdminController(ISedeService sedeService) {
        this.sedeService = sedeService;
    }

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
