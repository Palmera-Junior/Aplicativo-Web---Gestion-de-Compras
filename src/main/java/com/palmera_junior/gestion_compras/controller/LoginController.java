package com.palmera_junior.gestion_compras.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Controlador de autenticación y acceso al sistema.
 * Gestiona la visualización de la pantalla de inicio de sesión y la redirección de usuarios ya autenticados.
 */
@Controller
public class LoginController {

    private final IUsuarioService usuarioService;

    @Value("${security.oauth2.microsoft.enabled:false}")
    private boolean microsoftOAuth2Enabled;

    /**
     * Constructor para inyección de dependencias del servicio de usuarios.
     */
    public LoginController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Qué hace:
     * Evalúa el estado de autenticación del usuario; si ya está autenticado, lo redirige a su módulo autorizado;
     * de lo contrario, muestra la página de inicio de sesión (/login).
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /login
     * - Servicio delegado: {@link IUsuarioService#obtenerVistaLogin(Authentication)}
     * - Vista renderizada: "login" (templates/login.html) o la ruta inicial correspondiente al usuario.
     * 
     * @param authentication Objeto de autenticación del contexto de seguridad de Spring.
     * @return Vista a renderizar o redirección.
     */
    @GetMapping("/login")
    public String login(Authentication authentication, Model model, HttpServletRequest request) {
        model.addAttribute("microsoftOAuth2Enabled", microsoftOAuth2Enabled);
        Object errorOAuth2 = request.getSession(false) == null ? null
                : request.getSession(false).getAttribute("OAUTH2_ERROR_DETAIL");
        if (errorOAuth2 != null) {
            model.addAttribute("oauth2Error", errorOAuth2);
            request.getSession(false).removeAttribute("OAUTH2_ERROR_DETAIL");
        }
        return usuarioService.obtenerVistaLogin(authentication);
    }
}
