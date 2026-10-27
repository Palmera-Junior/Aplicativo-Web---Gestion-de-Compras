package com.palmera_junior.gestion_compras.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.palmera_junior.gestion_compras.service.usuario.PasswordResetService;
import com.palmera_junior.gestion_compras.service.usuario.PasswordResetService.ResultadoRestablecimiento;

@Controller
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);
    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/password/forgot")
    public String mostrarSolicitud() {
        return "password-forgot";
    }

    @PostMapping("/password/forgot")
    public String solicitar(@RequestParam String email, HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.solicitarRecuperacion(email, request.getRemoteAddr());
        } catch (PasswordResetService.DeliveryException ex) {
            log.warn("Se recibió una solicitud de recuperación, pero no fue posible despachar el correo.");
        }
        redirectAttributes.addFlashAttribute("solicitudEnviada", true);
        return "redirect:/password/forgot";
    }

    @GetMapping("/password/reset")
    public String mostrarRestablecimiento(@RequestParam(required = false) String token,
            HttpServletResponse response, Model model) {
        protegerPaginaDeToken(response);
        model.addAttribute("token", token);
        model.addAttribute("tokenValido", passwordResetService.tokenValido(token));
        return "password-reset";
    }

    @PostMapping("/password/reset")
    public String restablecer(@RequestParam(required = false) String token,
            @RequestParam String nuevaContrasena, @RequestParam String confirmarContrasena,
            HttpServletRequest request, HttpServletResponse response, Model model) {
        protegerPaginaDeToken(response);
        ResultadoRestablecimiento resultado = passwordResetService.restablecer(
                token, nuevaContrasena, confirmarContrasena, request.getRemoteAddr());
        if (resultado == ResultadoRestablecimiento.EXITOSO) {
            return "redirect:/login?passwordReset";
        }
        model.addAttribute("token", token);
        model.addAttribute("tokenValido", resultado != ResultadoRestablecimiento.TOKEN_INVALIDO);
        model.addAttribute("errorRestablecimiento", switch (resultado) {
            case CONFIRMACION_INVALIDA -> "Las contraseñas no coinciden.";
            case CONTRASENA_INVALIDA -> "Usa al menos 12 caracteres y máximo 72 bytes UTF-8.";
            case LIMITADO -> "Demasiados intentos. Espera 15 minutos e inténtalo nuevamente.";
            default -> "El enlace no es válido, ya fue usado o expiró. Solicita uno nuevo.";
        });
        return "password-reset";
    }

    private void protegerPaginaDeToken(HttpServletResponse response) {
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; style-src 'self'; img-src 'self'; "
                        + "form-action 'self'; base-uri 'none'; frame-ancestors 'none'");
    }
}
