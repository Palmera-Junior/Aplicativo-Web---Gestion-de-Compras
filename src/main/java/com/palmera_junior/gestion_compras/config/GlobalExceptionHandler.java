package com.palmera_junior.gestion_compras.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Manejador global de excepciones del aplicativo.
 * Intercepta excepciones de integridad de datos, validación y errores de servidor,
 * devolviendo respuestas JSON para peticiones AJAX o redirecciones seguras para vistas MVC.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
        String rutaRecursosHumanos = request.getContextPath() + "/recursos-humanos";
        if (uri != null && uri.startsWith(rutaPolizas)) {
            return "/polizas";
        }
        return uri != null && uri.startsWith(rutaRecursosHumanos)
                ? "/recursos-humanos/principal" : "/admin";
    }

    private ModelAndView redirigirConError(HttpServletRequest request, String mensaje) {
        String uri = request.getRequestURI();
        String rutaEmpleado = request.getContextPath() + "/recursos-humanos/empleados/";
        if (uri != null && uri.startsWith(rutaEmpleado)) {
            Matcher matcher = Pattern.compile(
                    "^" + Pattern.quote(rutaEmpleado) + "(\\d+)/(completar|contratacion)$")
                    .matcher(uri);
            if (matcher.matches()) {
                return new ModelAndView("redirect:/recursos-humanos/principal?editarEmpleadoId="
                        + matcher.group(1) + "&error=" + encode(mensaje) + "#registro-empleado");
            }
        }
        return new ModelAndView("redirect:" + destinoError(request) + "?error=" + encode(mensaje));
    }

    private String mensajeIntegridad(DataIntegrityViolationException ex) {
        Throwable causa = ex.getMostSpecificCause();
        String detalle = causa == null ? null : causa.getMessage();
        if (detalle != null && detalle.contains("Solo se permite un adjunto vigente")) {
            return "Ya existe un archivo vigente para uno de los conceptos seleccionados. "
                    + "Reemplázalo desde el formulario y vuelve a intentar.";
        }
        if (detalle != null && (detalle.contains("duplicate key")
                || detalle.contains("unique constraint") || detalle.contains("23505"))) {
            return "Ya existe un registro con uno de los datos ingresados. "
                    + "Verifica la información e inténtalo nuevamente.";
        }
        if (detalle != null && (detalle.contains("foreign key")
                || detalle.contains("violates foreign key") || detalle.contains("23503"))) {
            return "Uno de los datos seleccionados ya no está disponible. "
                    + "Actualiza la página y vuelve a intentarlo.";
        }
        return "No fue posible guardar la información por una restricción de datos. "
                + "Verifica los datos e inténtalo nuevamente.";
    }

    private Object respuestaError(
            HttpServletRequest request, HttpStatus status, String message) {
        if (isAjax(request)) {
            return ResponseEntity.status(status).body(Map.of("error", message));
        }
        return redirigirConError(request, message);
    }

    /**
     * Qué hace:
     * Maneja violaciones de integridad referencial (llaves foráneas, registros duplicados).
     * 
     * A dónde apunta:
     * - Retorna un mensaje seguro en JSON para AJAX o redirige a la vista funcional.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public Object handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = mensajeIntegridad(ex);
        log.warn("Violación de integridad de datos en {}", request.getRequestURI(), ex);
        return respuestaError(request, HttpStatus.CONFLICT, message);
    }

    /**
     * Qué hace:
     * Maneja errores generales de acceso a base de datos Spring Data JPA.
     * 
     * A dónde apunta:
     * - Retorna un mensaje seguro en JSON para AJAX o redirige a la vista funcional.
     */
    @ExceptionHandler(DataAccessException.class)
    public Object handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        String message = "No fue posible guardar la información en este momento. Inténtalo nuevamente.";
        log.error("Error de acceso a datos en {}", request.getRequestURI(), ex);
        return respuestaError(request, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    /**
     * Qué hace:
     * Maneja excepciones de validación de argumentos ilegales en la lógica de negocio.
     * 
     * A dónde apunta:
     * - Retorna 400 Bad Request JSON o redirige con mensaje de error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        String message = ex.getMessage() == null ? "Argumentos inválidos." : ex.getMessage();
        return respuestaError(request, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Object handleIllegalState(IllegalStateException ex, HttpServletRequest request) {
        String message = ex.getMessage() == null ? "No fue posible completar la operación." : ex.getMessage();
        return respuestaError(request, HttpStatus.CONFLICT, message);
    }

    @ExceptionHandler(SecurityException.class)
    public Object handleSecurity(SecurityException ex, HttpServletRequest request) {
        String message = ex.getMessage() == null ? "No tiene permisos para realizar esta operación." : ex.getMessage();
        return respuestaError(request, HttpStatus.FORBIDDEN, message);
    }

    @ExceptionHandler(BindException.class)
    public Object handleBindException(BindException ex, HttpServletRequest request) {
        String message = ex.getFieldError() == null
                ? "No fue posible procesar los datos enviados."
                : "El campo " + ex.getFieldError().getField() + " no tiene un valor válido.";
        return respuestaError(request, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String message = "La carga supera el límite permitido. Cada PDF puede pesar hasta 10 MB y el total de archivos adjuntos no puede superar 50 MB.";
        return respuestaError(request, HttpStatus.PAYLOAD_TOO_LARGE, message);
    }

    /**
     * Qué hace:
     * Maneja errores de validación de anotaciones (@Valid / @NotNull / @Size).
     * 
     * A dónde apunta:
     * - Retorna 400 Bad Request JSON o redirige con mensaje de error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = "Error de validación de datos.";
        return respuestaError(request, HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Qué hace:
     * Capturador genérico para cualquier excepción no controlada en el sistema.
     * 
     * A dónde apunta:
     * - Retorna JSON para AJAX o redirige a la vista funcional sin mostrar detalles internos.
     */
    @ExceptionHandler(Exception.class)
    public Object handleAll(Exception ex, HttpServletRequest request) {
        String message = "Ocurrió un error interno. Por favor contacte al administrador.";
        log.error("Error no controlado en {}", request.getRequestURI(), ex);
        return respuestaError(request, HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
