package com.palmera_junior.gestion_compras.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;


import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isAjax(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return (xrw != null && "XMLHttpRequest".equalsIgnoreCase(xrw))
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Operación no permitida: el registro tiene referencias en otras entidades.";
        // If database provides more specific cause, add a short hint but avoid exposing raw SQL
        if (ex.getMostSpecificCause() != null) {
            String causeMsg = ex.getMostSpecificCause().getMessage();
            if (causeMsg != null && causeMsg.length() < 200) {
                message = message + " Detalle: " + causeMsg;
            }
        }

        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", message));
        } else {
            // Redirect back to /admin with error message as query param so flash works without extra wiring
            String target = "/admin?error=" + encode(message);
            return new ModelAndView("redirect:" + target);
        }
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseBody
    public Object handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        String message = "Error de acceso a datos.";
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", message));
        } else {
            String target = "/admin?error=" + encode(message);
            return new ModelAndView("redirect:" + target);
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String message = ex.getMessage() == null ? "Argumentos inválidos." : ex.getMessage();
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
        } else {
            String target = "/admin?error=" + encode(message);
            return new ModelAndView("redirect:" + target);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = "Error de validación de datos.";
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
        } else {
            String target = "/admin?error=" + encode(message);
            return new ModelAndView("redirect:" + target);
        }
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Object handleAll(Exception ex, HttpServletRequest request) {
        // Generic fallback - log in server logs when available
        String message = "Ocurrió un error interno. Por favor contacte al administrador.";
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", message));
        } else {
            String target = "/admin?error=" + encode(message);
            return new ModelAndView("redirect:" + target);
        }
    }
}
