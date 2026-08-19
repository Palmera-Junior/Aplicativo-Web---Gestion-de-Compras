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

// Único responsable del CRUD de Centros de Costo dentro del panel de administración.
@Controller
public class CentroCostoAdminController {

    private final ICentroCostoService centroCostoService;

    public CentroCostoAdminController(ICentroCostoService centroCostoService) {
        this.centroCostoService = centroCostoService;
    }

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
