package com.palmera_junior.gestion_compras.service.usuario;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.SedeRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;
import com.palmera_junior.gestion_compras.security.CustomOAuth2User;
import com.palmera_junior.gestion_compras.security.UsuarioPrincipal;

@Service
public class UsuarioService implements IUsuarioService {

   private final UsuarioRepository usuarioRepository;
   private final SedeRepository sedeRepository;
   private final PasswordEncoder passwordEncoder;

   public UsuarioService(UsuarioRepository usuarioRepository, SedeRepository sedeRepository,
           PasswordEncoder passwordEncoder) {
       this.usuarioRepository = usuarioRepository;
       this.sedeRepository = sedeRepository;
       this.passwordEncoder = passwordEncoder;
   }

   public List<Usuario> listarTodos() {
       return usuarioRepository.findAll();
   }

   public Page<Usuario> paginar(int page, int size) {
       return usuarioRepository.findAll(PageRequest.of(page, size));
   }

   public Optional<Usuario> buscarPorEmail(String email) {
       return usuarioRepository.findByEmailConSede(email);
   }

   public Usuario vincularCuentaGoogle(Usuario usuario, String proveedorId) {
       usuario.setProveedor("google");
       usuario.setProveedorId(proveedorId);
       return usuarioRepository.save(usuario);
   }

   public Usuario guardar(Usuario usuario) {
       return usuarioRepository.save(usuario);
   }

   public Usuario obtenerUsuarioAutenticado() {
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       return obtenerUsuarioAutenticado(authentication);
   }

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

   public String obtenerVistaLogin(Authentication authentication) {
       if (authentication != null && authentication.isAuthenticated()) {
           return "redirect:/dashboard";
       }
       return "login";
   }

   @Transactional
   public Usuario guardarUsuario(Integer idUsuario, String cedula, String nombre, String apellido, String cargo,
           String nombreUsuario, String contrasena, String email, Rol rol, Integer sedeId) {
       final String cedulaTrim = normalizar(cedula);
       final String nombreTrim = normalizar(nombre);
       final String apellidoTrim = normalizar(apellido);
       final String nombreUsuarioTrim = normalizar(nombreUsuario);
       final String emailTrim = normalizar(email);
       final String contrasenaTrim = normalizar(contrasena);

       if (!StringUtils.hasText(cedulaTrim) || !StringUtils.hasText(nombreTrim) || !StringUtils.hasText(apellidoTrim)
               || !StringUtils.hasText(nombreUsuarioTrim) || !StringUtils.hasText(emailTrim) || sedeId == null) {
           throw new IllegalArgumentException("Todos los campos del usuario son obligatorios, excepto la contraseña al editar.");
       }

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