package com.palmera_junior.gestion_compras.security;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.palmera_junior.gestion_compras.entity.Usuario;

/**
 * Adaptador {@link OAuth2User} que vincula el usuario federado con la entidad {@link Usuario} interna.
 */
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2User oauth2User;
    private final Usuario usuario;

    /**
     * Constructor para envolver el principal OAuth2 y la entidad de usuario.
     */
    public CustomOAuth2User(
            OAuth2User oauth2User,
            Usuario usuario) {

        this.oauth2User = oauth2User;
        this.usuario = usuario;
    }

    /**
     * Retorna la entidad Usuario local.
     */
    public Usuario getUsuario() {
        return usuario;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return oauth2User.getAuthorities();
    }

    @Override
    public String getName() {
        return oauth2User.getName();
    }
}