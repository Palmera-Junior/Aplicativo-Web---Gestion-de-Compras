package com.palmera_junior.gestion_compras.service.usuario;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Usuario;

/**
 * Contrato de servicio para la gestión de usuarios, sesiones y seguridad de cuentas.
 */
public interface IUsuarioService {

    /**
     * Qué hace: Retorna todos los usuarios registrados.
     * A dónde apunta: Consulta tabla `usuario`.
     */
    List<Usuario> listarTodos();

    /**
     * Qué hace: Retorna los usuarios paginados.
     * A dónde apunta: Consulta tabla `usuario` con paginación.
     */
    Page<Usuario> paginar(int page, int size);

    /**
     * Qué hace: Busca un usuario por correo electrónico cargando su sede asociada.
     * A dónde apunta: Consulta tabla `usuario` (JOIN FETCH con sede).
     */
    Optional<Usuario> buscarPorEmail(String email);

    /**
     * Qué hace: Vincula una cuenta de inicio de sesión con Google OAuth2.
     * A dónde apunta: Modifica campos `proveedor` y `proveedor_id` en tabla `usuario`.
     */
    Usuario vincularCuentaGoogle(Usuario usuario, String proveedorId);

    /**
     * Qué hace: Guarda directamente una entidad Usuario.
     * A dónde apunta: Persistencia en tabla `usuario`.
     */
    Usuario guardar(Usuario usuario);

    /**
     * Qué hace: Obtiene el usuario autenticado del contexto actual de Spring Security.
     * A dónde apunta: {@link org.springframework.security.core.context.SecurityContextHolder}.
     */
    Usuario obtenerUsuarioAutenticado();

    /**
     * Qué hace: Extrae la entidad Usuario desde un objeto Authentication dado (OAuth2 o formulario nativo).
     * A dónde apunta: {@link com.palmera_junior.gestion_compras.security.UsuarioPrincipal} o {@link com.palmera_junior.gestion_compras.security.CustomOAuth2User}.
     */
    Usuario obtenerUsuarioAutenticado(Authentication authentication);

    /**
     * Qué hace: Construye un mapa JSON con los datos del usuario en sesión (nombre, email, rol) para el frontend.
     * A dónde apunta: Token de autenticación de Spring Security.
     */
    Map<String, Object> obtenerDatosUsuarioActual(Authentication authentication);

    /**
     * Qué hace: Determina si se renderiza el formulario de login o se redirige a /dashboard si ya está autenticado.
     * A dónde apunta: Vistas "login" o "redirect:/dashboard".
     */
    String obtenerVistaLogin(Authentication authentication);

    /**
     * Qué hace: Crea o actualiza un usuario con contraseña hasheada en BCrypt y validación de unicidad de cédula, email y username.
     * A dónde apunta: Tablas `usuario`, `sede`.
     */
    Usuario guardarUsuario(Integer idUsuario, String cedula, String nombre, String apellido, String cargo,
            String nombreUsuario, String contrasena, String email, Rol rol, Integer sedeId);

    /**
     * Qué hace: Elimina una cuenta de usuario por su ID si no tiene órdenes de compra asociadas.
     * A dónde apunta: Modifica tabla `usuario`.
     */
    boolean eliminar(Integer id);
}

