package com.palmera_junior.gestion_compras.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Controlador REST para consultas relacionadas con el usuario autenticado en la sesión actual.
 */
@RestController
public class UsuarioController {

    private final IUsuarioService usuarioService;

    /**
     * Constructor para inyección de dependencias del servicio de usuario.
     */
    public UsuarioController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Qué hace:
     * Retorna un mapa con los datos del usuario en sesión actual (nombre, rol, sede, nombre de usuario)
     * para inicializar variables de contexto en el frontend.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /api/usuario/actual
     * - Servicio delegado: {@link IUsuarioService#obtenerDatosUsuarioActual(Authentication)}
     * - Respuesta: JSON Map con los atributos del usuario.
     * 
     * @param authentication Información del usuario autenticado en Spring Security.
     * @return Map con los datos del perfil del usuario.
     */
    @GetMapping("/api/usuario/actual")
    public Map<String, Object> usuarioActual(Authentication authentication) {
        return usuarioService.obtenerDatosUsuarioActual(authentication);
    }
}