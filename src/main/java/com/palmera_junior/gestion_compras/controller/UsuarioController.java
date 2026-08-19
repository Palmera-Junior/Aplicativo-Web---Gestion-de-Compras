package com.palmera_junior.gestion_compras.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

@RestController
public class UsuarioController {

    private final IUsuarioService usuarioService;

    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/api/usuario/actual")
    public Map<String, Object> usuarioActual(Authentication authentication) {
        return usuarioService.obtenerDatosUsuarioActual(authentication);
    }
}