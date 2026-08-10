package com.palmera_junior.gestion_compras.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.security.CustomOAuth2User;

import java.util.Map;

@RestController
public class UsuarioController {

    @GetMapping("/api/usuario/actual")
    public Map<String, Object> usuarioActual(Authentication authentication) {
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
            nombre = authentication.getName(); // login por formulario
        }

        String rol = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        return Map.of(
                "autenticado", true,
                "nombre", nombre,
                "email", email,
                "rol", rol);
    }
}