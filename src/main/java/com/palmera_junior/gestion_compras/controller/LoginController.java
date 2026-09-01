package com.palmera_junior.gestion_compras.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Controlador de autenticación y acceso al sistema.
 * Gestiona la visualización de la pantalla de inicio de sesión y la redirección de usuarios ya autenticados.
 */
@Controller
public class LoginController {

    private final IUsuarioService usuarioService;

    @Value("${security.oauth2.microsoft.enabled:true}")
    private boolean microsoftOAuth2Enabled;

    /**
     * Constructor para inyección de dependencias del servicio de usuarios.
     */
    public LoginController(IUsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Qué hace:
     * Evalúa el estado de autenticación del usuario; si ya está autenticado, lo redirige al dashboard (/dashboard);
     * de lo contrario, muestra la página de inicio de sesión (/login).
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /login
     * - Servicio delegado: {@link IUsuarioService#obtenerVistaLogin(Authentication)}
     * - Vistas renderizadas: "login" (templates/login.html) o redirección "redirect:/dashboard".
     * 
     * @param authentication Objeto de autenticación del contexto de seguridad de Spring.
     * @return Vista a renderizar o redirección.
     */
    @GetMapping("/login")
    public String login(Authentication authentication, Model model) {
        model.addAttribute("microsoftOAuth2Enabled", microsoftOAuth2Enabled);
        return usuarioService.obtenerVistaLogin(authentication);
    }
}

