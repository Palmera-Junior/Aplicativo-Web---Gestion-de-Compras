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
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Manejador global de excepciones del aplicativo.
 * Intercepta excepciones de integridad de datos, validación y errores de servidor,
 * devolviendo respuestas JSON para peticiones AJAX o redirecciones con parámetros de error para vistas MVC.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Determina si la solicitud entrante es de tipo AJAX (X-Requested-With o Accept application/json).
     */
    private boolean isAjax(HttpServletRequest request) {
        String xrw = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return (xrw != null && "XMLHttpRequest".equalsIgnoreCase(xrw))
                || (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE));
    }

    private String encode(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private String destinoError(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String rutaPolizas = request.getContextPath() + "/polizas";
        return uri != null && uri.startsWith(rutaPolizas) ? "/polizas" : "/admin";
    }

    private ModelAndView redirigirConError(HttpServletRequest request, String mensaje) {
        return new ModelAndView("redirect:" + destinoError(request) + "?error=" + encode(mensaje));
    }

    /**
     * Qué hace:
     * Maneja violaciones de integridad referencial (llaves foráneas, registros duplicados).
     * 
     * A dónde apunta:
     * - Retorna 409 Conflict JSON si es AJAX o redirige a /admin?error=...
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = "Operación no permitida: el registro tiene referencias en otras entidades.";
        if (ex.getMostSpecificCause() != null) {
            String causeMsg = ex.getMostSpecificCause().getMessage();
            if (causeMsg != null && causeMsg.length() < 200) {
                message = message + " Detalle: " + causeMsg;
            }
        }

        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", message));
        } else {
            return redirigirConError(request, message);
        }
    }

    /**
     * Qué hace:
     * Maneja errores generales de acceso a base de datos Spring Data JPA.
     * 
     * A dónde apunta:
     * - Retorna 500 Internal Server Error JSON o redirige a /admin?error=...
     */
    @ExceptionHandler(DataAccessException.class)
    @ResponseBody
    public Object handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        String message = "Error de acceso a datos.";
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", message));
        } else {
            return redirigirConError(request, message);
        }
    }

    /**
     * Qué hace:
     * Maneja excepciones de validación de argumentos ilegales en la lógica de negocio.
     * 
     * A dónde apunta:
     * - Retorna 400 Bad Request JSON o redirige con mensaje de error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String message = ex.getMessage() == null ? "Argumentos inválidos." : ex.getMessage();
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
        } else {
            return redirigirConError(request, message);
        }
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String message = ex.getMessage() == null ? "No fue posible completar la operación." : ex.getMessage();
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", message));
        }
        return redirigirConError(request, message);
    }

    @ExceptionHandler(BindException.class)
    @ResponseBody
    public Object handleBindException(BindException ex, HttpServletRequest request) {
        String message = ex.getFieldError() == null
                ? "No fue posible procesar los datos enviados."
                : "El campo " + ex.getFieldError().getField() + " no tiene un valor válido.";
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
        }
        return redirigirConError(request, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public Object handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String message = "El archivo adjunto supera el tamaño máximo permitido de 10 MB.";
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Map.of("error", message));
        }
        return redirigirConError(request, message);
    }

    /**
     * Qué hace:
     * Maneja errores de validación de anotaciones (@Valid / @NotNull / @Size).
     * 
     * A dónde apunta:
     * - Retorna 400 Bad Request JSON o redirige con mensaje de error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = "Error de validación de datos.";
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
        } else {
            return redirigirConError(request, message);
        }
    }

    /**
     * Qué hace:
     * Capturador genérico para cualquier excepción no controlada en el sistema.
     * 
     * A dónde apunta:
     * - Retorna 500 Internal Server Error JSON o redirección a /admin.
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public Object handleAll(Exception ex, HttpServletRequest request) {
        String message = "Ocurrió un error interno. Por favor contacte al administrador.";
        if (isAjax(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", message));
        } else {
            return redirigirConError(request, message);
        }
    }
}

