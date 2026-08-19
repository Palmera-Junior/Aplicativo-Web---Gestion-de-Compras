package com.palmera_junior.gestion_compras.controller.admin;

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

import java.util.Map;

import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

// Único responsable del CRUD y la paginación de Usuarios dentro del panel de administración.
@Controller
public class UsuarioAdminController {

    private final IUsuarioService usuarioService;

    public UsuarioAdminController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Endpoint AJAX para paginar la tabla de Usuarios sin recargar la página
    @GetMapping("/admin/usuarios/pagina")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String paginaUsuarios(Model model,
            @RequestParam(defaultValue = "0") int pageUsuarios,
            @RequestParam(defaultValue = "10") int size) {
        Page<Usuario> usuariosPage = usuarioService.paginar(pageUsuarios, size);
        model.addAttribute("usuariosPage", usuariosPage);
        model.addAttribute("pageUsuarios", pageUsuarios);
        model.addAttribute("size", size);
        return "admin :: usuariosFragment";
    }

    @PostMapping("/admin/usuarios")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public String guardarUsuario(@RequestParam(required = false) Integer idUsuario,
            @RequestParam String cedula,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam(required = false) String cargo,
            @RequestParam String nombreUsuario,
            @RequestParam(required = false) String contrasena,
            @RequestParam String email,
            @RequestParam Rol rol,
            @RequestParam Integer sedeId,
            RedirectAttributes redirectAttributes) {

        try {
            usuarioService.guardarUsuario(idUsuario, cedula, nombre, apellido, cargo, nombreUsuario, contrasena,
                    email, rol, sedeId);
            redirectAttributes.addAttribute("success",
                    idUsuario == null ? "Usuario creado correctamente." : "Usuario actualizado correctamente.");
            return "redirect:/admin";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin";
        }
    }

    @PostMapping("/admin/usuarios/delete/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> deleteUsuario(@PathVariable Integer id) {
        try {
            if (!usuarioService.eliminar(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Usuario no encontrado"));
            }
            return ResponseEntity.ok(Map.of("success", "Usuario eliminado"));
        } catch (DataIntegrityViolationException dive) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "No se puede eliminar el usuario porque está asociado a órdenes de compra u otros registros."));
        } catch (DataAccessException dae) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Error al eliminar usuario: " + dae.getMostSpecificCause()));
        }
    }
}
