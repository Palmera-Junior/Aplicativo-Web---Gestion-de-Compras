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

/**
 * Controlador administrativo para la gestión de Usuarios del sistema.
 * Permite listar con paginación asíncrona, crear nuevos usuarios con roles y sedes,
 * actualizar credenciales y eliminar cuentas.
 */
@Controller
public class UsuarioAdminController {

    private final IUsuarioService usuarioService;

    /**
     * Constructor para inyección de dependencias del servicio de usuarios.
     */
    public UsuarioAdminController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Qué hace:
     * Retorna el fragmento HTML Thymeleaf correspondiente a la tabla paginada de usuarios para refresco AJAX.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /admin/usuarios/pagina
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IUsuarioService#paginar(int, int)} -> UsuarioRepository
     * - Fragmento: "admin :: usuariosFragment"
     * 
     * @param model Modelo para inyectar la página y metadatos.
     * @param pageUsuarios Índice de página.
     * @param size Cantidad de usuarios por página.
     * @return Fragmento Thymeleaf `admin :: usuariosFragment`.
     */
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

    /**
     * Qué hace:
     * Crea un nuevo usuario o actualiza sus datos de perfil (cédula, nombre, apellido, cargo, nombreUsuario,
     * email, rol, sede y contraseña encriptada con BCrypt).
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/usuarios
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IUsuarioService#guardarUsuario} -> UsuarioRepository, PasswordEncoder
     * - Redirección: "redirect:/admin" con mensajes flash de éxito o error.
     * 
     * @param idUsuario ID del usuario si es actualización; null si es creación.
     * @param cedula Número de identificación / cédula.
     * @param nombre Nombres del usuario.
     * @param apellido Apellidos del usuario.
     * @param cargo Cargo o función en la empresa.
     * @param nombreUsuario Nombre de usuario único para login.
     * @param contrasena Contraseña en texto plano (se encripta si se proporciona).
     * @param email Correo electrónico institucional.
     * @param rol Rol del usuario (ADMINISTRADOR, COMPRADOR, etc.).
     * @param sedeId ID de la sede a la que pertenece el usuario.
     * @param redirectAttributes Atributos flash para mensajes temporales.
     * @return Redirección a la vista de administración.
     */
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

    /**
     * Qué hace:
     * Elimina una cuenta de usuario por su ID, validando que no tenga órdenes registradas o aprobadas.
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /admin/usuarios/delete/{id}
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IUsuarioService#eliminar(Integer)} -> UsuarioRepository
     * - Retorno: JSON Map con status 200, 404, 409 o 500.
     * 
     * @param id Identificador numérico del usuario a eliminar.
     * @return {@link ResponseEntity} indicando el resultado.
     */
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

