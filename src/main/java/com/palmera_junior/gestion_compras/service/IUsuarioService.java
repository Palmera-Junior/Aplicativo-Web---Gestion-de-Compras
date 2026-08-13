package com.palmera_junior.gestion_compras.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;

import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Usuario;

public interface IUsuarioService {
    List<Usuario> listarTodos();
    Page<Usuario> paginar(int page, int size);
    Optional<Usuario> buscarPorEmail(String email);
    Usuario vincularCuentaGoogle(Usuario usuario, String proveedorId);
    Usuario guardar(Usuario usuario);
    Usuario obtenerUsuarioAutenticado();
    Usuario obtenerUsuarioAutenticado(Authentication authentication);
    Map<String, Object> obtenerDatosUsuarioActual(Authentication authentication);
    String obtenerVistaLogin(Authentication authentication);
    Usuario guardarUsuario(Integer idUsuario, String cedula, String nombre, String apellido, String cargo,
            String nombreUsuario, String contrasena, String email, Rol rol, Integer sedeId);
    boolean eliminar(Integer id);
}
