package com.palmera_junior.gestion_compras.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PermisoRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PermisoRepository permisoRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository, PermisoRepository permisoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.permisoRepository = permisoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el username: " + username));
       
        if (usuario.getSede() != null){
            usuario.getSede().getNombre();
        }
        if (usuario.esSuperAdministrador()) {
            usuario.setPermisosSuperAdministrador(new java.util.LinkedHashSet<>(permisoRepository.findAll()));
        }
        return new UsuarioPrincipal(usuario);
    }

    
}
