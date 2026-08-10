package com.palmera_junior.gestion_compras.service;

import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

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
}