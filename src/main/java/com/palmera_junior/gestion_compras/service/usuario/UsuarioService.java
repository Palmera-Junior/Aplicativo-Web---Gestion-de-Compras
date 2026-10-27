package com.palmera_junior.gestion_compras.service.usuario;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Permiso;
import com.palmera_junior.gestion_compras.entity.PermisoUsuario;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.SedeRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;
import com.palmera_junior.gestion_compras.repository.RolRepository;
import com.palmera_junior.gestion_compras.repository.PermisoRepository;
import com.palmera_junior.gestion_compras.security.CustomOAuth2User;
import com.palmera_junior.gestion_compras.security.RutaInicialUsuario;
import com.palmera_junior.gestion_compras.security.UsuarioPrincipal;

/**
 * Implementación del servicio de gestión de usuarios y autenticación.
 * Gestiona el ciclo de vida de usuarios, validaciones de seguridad, cifrado de claves con BCrypt
 * y mapeo de identidades OAuth2 / formularios nativos.
 */
@Service
public class UsuarioService implements IUsuarioService {

   private final UsuarioRepository usuarioRepository;
   private final SedeRepository sedeRepository;
   private final PasswordEncoder passwordEncoder;
   private final RolRepository rolRepository;
   private final PermisoRepository permisoRepository;

   /**
    * Constructor con inyección de repositorios y codificador de contraseñas.
    */
   public UsuarioService(UsuarioRepository usuarioRepository, SedeRepository sedeRepository,
           PasswordEncoder passwordEncoder, RolRepository rolRepository, PermisoRepository permisoRepository) {
       this.usuarioRepository = usuarioRepository;
       this.sedeRepository = sedeRepository;
       this.passwordEncoder = passwordEncoder;
       this.rolRepository = rolRepository;
       this.permisoRepository = permisoRepository;
   }

   /**
    * Qué hace: Retorna la lista total de usuarios en base de datos.
    * A dónde apunta: {@link UsuarioRepository#findAll()} -> tabla usuario
    */
   @Override
   public List<Usuario> listarTodos() {
       return usuarioRepository.findAll();
   }

   /**
    * Qué hace: Retorna una página de usuarios.
    * A dónde apunta: {@link UsuarioRepository#findAll(org.springframework.data.domain.Pageable)} -> tabla usuario
    */
   @Override
   public Page<Usuario> paginar(int page, int size) {
       return usuarioRepository.findAll(PageRequest.of(page, size));
   }

   /**
    * Qué hace: Busca un usuario por correo electrónico cargando con JOIN FETCH su sede.
    * A dónde apunta: {@link UsuarioRepository#findByEmailConSede(String)} -> tabla usuario
    */
   @Override
   public Optional<Usuario> buscarPorEmail(String email) {
       return usuarioRepository.findByEmailConSede(email);
   }

   /**
    * Qué hace: Asigna el proveedor OAuth2 (Google) y el ID de proveedor a la cuenta.
    * A dónde apunta: {@link UsuarioRepository#save(Object)} -> tabla usuario
    */
   @Override
   public Usuario vincularCuentaGoogle(Usuario usuario, String proveedorId) {
       usuario.setProveedor("google");
       usuario.setProveedorId(proveedorId);
       return usuarioRepository.save(usuario);
   }

   /**
    * Qué hace: Persiste una entidad Usuario.
    * A dónde apunta: {@link UsuarioRepository#save(Object)} -> tabla usuario
    */
   @Override
   public Usuario guardar(Usuario usuario) {
       return usuarioRepository.save(usuario);
   }

   /**
    * Qué hace: Obtiene el usuario autenticado desde el contexto de Spring Security.
    * A dónde apunta: {@link SecurityContextHolder#getContext()}
    */
   @Override
   public Usuario obtenerUsuarioAutenticado() {
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       return obtenerUsuarioAutenticado(authentication);
   }

   /**
    * Qué hace: Resuelve la entidad {@link Usuario} subyacente según el tipo de principal de seguridad.
    * A dónde apunta: Instancia de {@link UsuarioPrincipal} o {@link CustomOAuth2User}.
    */
   @Override
   public Usuario obtenerUsuarioAutenticado(Authentication authentication) {
       if (authentication == null || !authentication.isAuthenticated()) {
           throw new IllegalStateException("No hay un usuario autenticado.");
       }

       Object principal = authentication.getPrincipal();
       if (principal instanceof UsuarioPrincipal usuarioPrincipal) {
           return usuarioPrincipal.getUsuario();
       }
       if (principal instanceof CustomOAuth2User oauthUser) {
           return oauthUser.getUsuario();
       }

       throw new IllegalStateException("Tipo de autenticación no soportado.");
   }

   /**
    * Qué hace: Genera un mapa de atributos del usuario actual (nombre, correo, rol) para respuestas JSON.
    * A dónde apunta: Sesión activa del usuario.
    */
   @Override
   public Map<String, Object> obtenerDatosUsuarioActual(Authentication authentication) {
       if (authentication == null || !authentication.isAuthenticated()) {
           return Map.of("autenticado", false);
       }

       String nombre;
       String email = "";

       Object principal = authentication.getPrincipal();
       if (principal instanceof CustomOAuth2User oauthUser) {
           Usuario usuario = oauthUser.getUsuario();
           nombre = usuario.getNombre() + " " + usuario.getApellido();
           email = usuario.getEmail();
       } else {
           nombre = authentication.getName();
       }

       String rol = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
               .findFirst()
               .orElse("");

       return Map.of(
               "autenticado", true,
               "nombre", nombre,
               "email", email,
               "rol", rol);
   }

   /**
    * Qué hace: Retorna la vista "login" o redirige al dashboard si la sesión ya está activa.
    * A dónde apunta: Vistas Spring MVC.
    */
   @Override
   public String obtenerVistaLogin(Authentication authentication) {
       if (authentication != null && authentication.isAuthenticated()
               && !(authentication instanceof AnonymousAuthenticationToken)) {
           return "redirect:" + RutaInicialUsuario.resolver(authentication).orElse("/acceso-denegado");
       }
       return "login";
   }

   /**
    * Qué hace: Crea o actualiza un usuario con cifrado BCrypt y chequeos de unicidad para cédula, usuario y correo.
    * A dónde apunta: {@link UsuarioRepository}, {@link SedeRepository}, {@link PasswordEncoder}.
    */
   @Override
   @Transactional
   public Usuario guardarUsuario(Integer idUsuario, String cedula, String nombre, String apellido, String cargo,
           String nombreUsuario, String contrasena, String email, Integer rolId, Integer sedeId) {
       final String cedulaTrim = normalizar(cedula);
       final String nombreTrim = normalizar(nombre);
       final String apellidoTrim = normalizar(apellido);
       final String nombreUsuarioTrim = normalizar(nombreUsuario);
       final String emailTrim = normalizar(email);
       final String contrasenaTrim = normalizar(contrasena);

       if (!StringUtils.hasText(cedulaTrim) || !StringUtils.hasText(nombreTrim) || !StringUtils.hasText(apellidoTrim)
               || !StringUtils.hasText(nombreUsuarioTrim) || !StringUtils.hasText(emailTrim) || sedeId == null
               || rolId == null) {
           throw new IllegalArgumentException("Todos los campos del usuario son obligatorios, excepto la contraseña al editar.");
       }
       Rol rol = rolRepository.findById(rolId)
               .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no es válido."));

       if (idUsuario == null) {
           if (usuarioRepository.existsByCedula(cedulaTrim)) {
               throw new IllegalArgumentException("Ya existe un usuario registrado con esa cédula.");
           }
           if (usuarioRepository.existsByNombreUsuario(nombreUsuarioTrim)) {
               throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
           }
           if (!StringUtils.hasText(contrasenaTrim)) {
               throw new IllegalArgumentException("La contraseña es obligatoria para crear un usuario.");
           }
           if (usuarioRepository.findByEmail(emailTrim).isPresent()) {
               throw new IllegalArgumentException("El correo electrónico ya está en uso.");
           }
           Sede sede = sedeRepository.findById(sedeId)
                   .orElseThrow(() -> new IllegalArgumentException("La sede seleccionada no es válida."));
           Usuario usuario = new Usuario();
           usuario.setCedula(cedulaTrim);
           usuario.setNombre(nombreTrim);
           usuario.setApellido(apellidoTrim);
           usuario.setCargo(cargo);
           usuario.setNombreUsuario(nombreUsuarioTrim);
           usuario.setEmail(emailTrim);
           usuario.setContraseña(passwordEncoder.encode(contrasenaTrim));
           usuario.setRol(rol);
           usuario.setSede(sede);
           return usuarioRepository.save(usuario);
       }

       Usuario usuario = usuarioRepository.findById(idUsuario)
               .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado para actualizar."));
       if (usuarioRepository.findByCedula(cedulaTrim).filter(u -> !u.getIdUsuario().equals(idUsuario)).isPresent()) {
           throw new IllegalArgumentException("Ya existe un usuario registrado con esa cédula.");
       }
       if (usuarioRepository.findByNombreUsuario(nombreUsuarioTrim).filter(u -> !u.getIdUsuario().equals(idUsuario)).isPresent()) {
           throw new IllegalArgumentException("El nombre de usuario ya está en uso.");
       }
       if (usuarioRepository.findByEmail(emailTrim).filter(u -> !u.getIdUsuario().equals(idUsuario)).isPresent()) {
           throw new IllegalArgumentException("El correo electrónico ya está en uso.");
       }
       Sede sede = sedeRepository.findById(sedeId)
               .orElseThrow(() -> new IllegalArgumentException("La sede seleccionada no es válida."));
       usuario.setCedula(cedulaTrim);
       usuario.setNombre(nombreTrim);
       usuario.setApellido(apellidoTrim);
       usuario.setCargo(cargo);
       usuario.setEmail(emailTrim);
       usuario.setNombreUsuario(nombreUsuarioTrim);
       if (StringUtils.hasText(contrasenaTrim)) {
           usuario.setContraseña(passwordEncoder.encode(contrasenaTrim));
       }
       usuario.setRol(rol);
       usuario.setSede(sede);
       return usuarioRepository.save(usuario);
   }

   @Override
   @Transactional
   public Usuario actualizarPermisos(Integer idUsuario, List<Integer> permisoIds) {
       if (idUsuario == null) {
           throw new IllegalArgumentException("Debe seleccionar un usuario.");
       }

       Usuario usuario = usuarioRepository.findById(idUsuario)
               .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));
       java.util.Set<Integer> idsUnicos = permisoIds == null
               ? java.util.Set.of()
               : new java.util.HashSet<>(permisoIds);
       List<Permiso> permisosDisponibles = permisoRepository.findAll();
       java.util.Map<Integer, Permiso> permisosPorId = permisosDisponibles.stream()
               .collect(java.util.stream.Collectors.toMap(Permiso::getIdPermiso, permiso -> permiso));
       if (!permisosPorId.keySet().containsAll(idsUnicos)) {
           throw new IllegalArgumentException("La lista de permisos contiene opciones no válidas.");
       }

       java.util.Set<Integer> permisosDelRol = usuario.getRol().getPermisos().stream()
               .map(Permiso::getIdPermiso)
               .collect(java.util.stream.Collectors.toSet());
       java.util.Map<Integer, PermisoUsuario> overridesExistentes = usuario.getPermisosDirectos().stream()
               .collect(java.util.stream.Collectors.toMap(
                       override -> override.getPermiso().getIdPermiso(), override -> override));
       for (Permiso permiso : permisosDisponibles) {
           Integer permisoId = permiso.getIdPermiso();
           boolean concedidoPorUsuario = idsUnicos.contains(permisoId);
           boolean concedidoPorRol = permisosDelRol.contains(permisoId);
           PermisoUsuario existente = overridesExistentes.remove(permisoId);
           if (concedidoPorUsuario == concedidoPorRol) {
               if (existente != null) {
                   usuario.getPermisosDirectos().remove(existente);
               }
               continue;
           }
           if (existente == null) {
               existente = new PermisoUsuario();
               existente.setUsuario(usuario);
               existente.setPermiso(permiso);
               usuario.getPermisosDirectos().add(existente);
           }
           existente.setConcedido(concedidoPorUsuario);
       }
       usuario.getPermisosDirectos().removeAll(overridesExistentes.values());
       return usuarioRepository.save(usuario);
   }

   @Override
   @Transactional
   public Rol guardarRol(Integer rolId, String codigo, List<Integer> permisoIds) {
       Rol rol;
       if (rolId == null) {
           String codigoNormalizado = normalizar(codigo);
           if (!StringUtils.hasText(codigoNormalizado)) {
               throw new IllegalArgumentException("El nombre del rol es obligatorio.");
           }
           codigoNormalizado = codigoNormalizado.toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", "_");
           if (!codigoNormalizado.matches("[A-Z0-9_]{2,30}")) {
               throw new IllegalArgumentException("Usa de 2 a 30 letras, números o guiones bajos para el código del rol.");
           }
           if (rolRepository.existsByCodigoIgnoreCase(codigoNormalizado)) {
               throw new IllegalArgumentException("Ya existe un rol con ese nombre.");
           }
           rol = new Rol();
           rol.setCodigo(codigoNormalizado);
       } else {
           rol = rolRepository.findById(rolId)
                   .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));
       }

       java.util.Set<Integer> idsUnicos = permisoIds == null
               ? java.util.Set.of()
               : new java.util.HashSet<>(permisoIds);
       List<Permiso> permisos = idsUnicos.isEmpty() ? List.of() : permisoRepository.findAllById(idsUnicos);
       if (permisos.size() != idsUnicos.size()) {
           throw new IllegalArgumentException("La lista de permisos contiene opciones no válidas.");
       }
       rol.setPermisos(new java.util.HashSet<>(permisos));
       return rolRepository.save(rol);
   }

   @Override
   @Transactional
   public void eliminarRol(Integer rolId) {
       if (rolId == null) {
           throw new IllegalArgumentException("Debe seleccionar un rol.");
       }
       Rol rol = rolRepository.findById(rolId)
               .orElseThrow(() -> new IllegalArgumentException("El rol seleccionado no existe."));
       if (usuarioRepository.existsByRolIdRol(rolId)) {
           throw new IllegalStateException("No se puede eliminar el rol porque está asignado a uno o más usuarios.");
       }
       rolRepository.delete(rol);
   }

   /**
    * Qué hace: Elimina un usuario por su ID si no tiene registros dependientes en órdenes.
    * A dónde apunta: {@link UsuarioRepository#deleteById(Object)} -> tabla usuario
    */
   @Override
   @Transactional
   public boolean eliminar(Integer id) {
       if (!usuarioRepository.existsById(id)) {
           return false;
       }
       usuarioRepository.deleteById(id);
       return true;
   }


   private String normalizar(String value) {
       return value == null ? null : value.trim();
   }
}