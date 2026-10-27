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
import java.util.List;

import com.palmera_junior.gestion_compras.entity.Permiso;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PermisoRepository;
import com.palmera_junior.gestion_compras.repository.RolRepository;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Controlador administrativo para la gestión de Usuarios del sistema.
 * Permite listar con paginación asíncrona, crear nuevos usuarios con roles y sedes,
 * actualizar credenciales y eliminar cuentas.
 */
@Controller
public class UsuarioAdminController {

    private final IUsuarioService usuarioService;
    private final PermisoRepository permisoRepository;
    private final RolRepository rolRepository;

    /**
     * Constructor para inyección de dependencias del servicio de usuarios.
     */
    public UsuarioAdminController(IUsuarioService usuarioService, PermisoRepository permisoRepository,
            RolRepository rolRepository) {
        this.usuarioService = usuarioService;
        this.permisoRepository = permisoRepository;
        this.rolRepository = rolRepository;
    }

    @GetMapping("/admin/catalogos/permisos")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public String administrarPermisos(Model model,
            @RequestParam(required = false) Integer usuarioId,
            @RequestParam(required = false) Integer rolId,
            @RequestParam(defaultValue = "usuarios") String vista,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String error) {
        List<Usuario> usuarios = usuarioService.listarTodos().stream()
                .sorted(java.util.Comparator.comparing(Usuario::getNombreUsuario,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        Usuario usuarioSeleccionado;
        if (usuarioId == null && !usuarios.isEmpty()) {
            usuarioSeleccionado = usuarios.get(0);
        } else {
            usuarioSeleccionado = usuarios.stream()
                    .filter(usuario -> usuarioId != null && usuarioId.equals(usuario.getIdUsuario()))
                    .findFirst()
                    .orElse(null);
        }
        List<com.palmera_junior.gestion_compras.entity.Rol> roles = rolRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(com.palmera_junior.gestion_compras.entity.Rol::getCodigo,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        var rolSeleccionado = rolId == null
                ? null
                : roles.stream().filter(rol -> rolId.equals(rol.getIdRol())).findFirst().orElse(null);

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("permisos", permisoRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Permiso::getNombre,
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList());
        model.addAttribute("usuarioSeleccionado", usuarioSeleccionado);
        model.addAttribute("usuarioId", usuarioSeleccionado == null ? null : usuarioSeleccionado.getIdUsuario());
        model.addAttribute("roles", roles);
        model.addAttribute("rolSeleccionado", rolSeleccionado);
        model.addAttribute("rolId", rolSeleccionado == null ? null : rolSeleccionado.getIdRol());
        model.addAttribute("vistaPermisos", "roles".equals(vista) ? "roles" : "usuarios");
        model.addAttribute("catalogoActivo", "permisos");
        model.addAttribute("successMessage", success);
        model.addAttribute("errorMessage", error != null
                ? error
                : usuarioId != null && usuarioSeleccionado == null ? "El usuario seleccionado no existe."
                : rolId != null && rolSeleccionado == null ? "El rol seleccionado no existe." : null);
        return "permisos";
    }

    @PostMapping("/admin/catalogos/permisos")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public String guardarPermisos(@RequestParam Integer usuarioId,
            @RequestParam(required = false) List<Integer> permisoIds,
            RedirectAttributes redirectAttributes) {
        try {
            usuarioService.actualizarPermisos(usuarioId, permisoIds);
            redirectAttributes.addAttribute("usuarioId", usuarioId);
            redirectAttributes.addAttribute("vista", "usuarios");
            redirectAttributes.addAttribute("success", "Permisos actualizados correctamente.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("usuarioId", usuarioId);
            redirectAttributes.addAttribute("vista", "usuarios");
            redirectAttributes.addAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirectAttributes.addAttribute("usuarioId", usuarioId);
            redirectAttributes.addAttribute("vista", "usuarios");
            redirectAttributes.addAttribute("error", "No se pudieron actualizar los permisos del usuario.");
        }
        return "redirect:/admin/catalogos/permisos";
    }

    @PostMapping("/admin/catalogos/permisos/roles")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public String guardarRol(@RequestParam(required = false) Integer rolId,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) List<Integer> permisoIds,
            RedirectAttributes redirectAttributes) {
        try {
            var rol = usuarioService.guardarRol(rolId, codigo, permisoIds);
            redirectAttributes.addAttribute("rolId", rol.getIdRol());
            redirectAttributes.addAttribute("success", rolId == null
                    ? "Rol creado correctamente."
                    : "Permisos del rol actualizados correctamente.");
        } catch (IllegalArgumentException ex) {
            if (rolId != null) {
                redirectAttributes.addAttribute("rolId", rolId);
            }
            redirectAttributes.addAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            if (rolId != null) {
                redirectAttributes.addAttribute("rolId", rolId);
            }
            redirectAttributes.addAttribute("error", "No se pudo guardar el rol.");
        }
        redirectAttributes.addAttribute("vista", "roles");
        return "redirect:/admin/catalogos/permisos";
    }

    @PostMapping("/admin/catalogos/permisos/roles/delete/{rolId}")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public String eliminarRol(@PathVariable Integer rolId, RedirectAttributes redirectAttributes) {
        try {
            usuarioService.eliminarRol(rolId);
            redirectAttributes.addAttribute("success", "Rol eliminado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addAttribute("rolId", rolId);
            redirectAttributes.addAttribute("error", ex.getMessage());
        } catch (DataAccessException ex) {
            redirectAttributes.addAttribute("rolId", rolId);
            redirectAttributes.addAttribute("error", "No se pudo eliminar el rol.");
        }
        redirectAttributes.addAttribute("vista", "roles");
        return "redirect:/admin/catalogos/permisos";
    }

    /**
     * Qué hace:
     * Retorna el fragmento HTML Thymeleaf correspondiente a la tabla paginada de usuarios para refresco AJAX.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /admin/usuarios/pagina
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicio delegado: {@link IUsuarioService#paginar(int, int)} -> UsuarioRepository
    * - Fragmento: "catalogos :: usuariosFragment"
     * 
     * @param model Modelo para inyectar la página y metadatos.
     * @param pageUsuarios Índice de página.
     * @param size Cantidad de usuarios por página.
    * @return Fragmento Thymeleaf `catalogos :: usuariosFragment`.
     */
    @GetMapping("/admin/usuarios/pagina")
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public String paginaUsuarios(Model model,
            @RequestParam(defaultValue = "0") int pageUsuarios,
            @RequestParam(defaultValue = "10") int size) {
        Page<Usuario> usuariosPage = usuarioService.paginar(pageUsuarios, size);
        model.addAttribute("usuariosPage", usuariosPage);
        model.addAttribute("pageUsuarios", pageUsuarios);
        model.addAttribute("size", size);
        return "catalogos :: usuariosFragment";
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
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
    public String guardarUsuario(@RequestParam(required = false) Integer idUsuario,
            @RequestParam String cedula,
            @RequestParam String nombre,
            @RequestParam String apellido,
            @RequestParam(required = false) String cargo,
            @RequestParam String nombreUsuario,
            @RequestParam(required = false) String contrasena,
            @RequestParam String email,
            @RequestParam Integer rolId,
            @RequestParam Integer sedeId,
            RedirectAttributes redirectAttributes) {

        try {
            usuarioService.guardarUsuario(idUsuario, cedula, nombre, apellido, cargo, nombreUsuario, contrasena,
                    email, rolId, sedeId);
            redirectAttributes.addAttribute("success",
                    idUsuario == null ? "Usuario creado correctamente." : "Usuario actualizado correctamente.");
            return "redirect:/admin/catalogos/usuarios";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
            return "redirect:/admin/catalogos/usuarios";
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
    @PreAuthorize("hasRole('SUPERADMINISTRADOR')")
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
