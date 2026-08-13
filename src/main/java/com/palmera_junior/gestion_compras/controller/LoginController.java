package com.palmera_junior.gestion_compras.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.palmera_junior.gestion_compras.service.IUsuarioService;

@Controller
public class LoginController {

    private final IUsuarioService usuarioService;

    public LoginController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        return usuarioService.obtenerVistaLogin(authentication);
    }
}
