package com.palmera_junior.gestion_compras.service.correo;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Servicio encargado del procesamiento y renderizado dinámico de plantillas HTML de correo electrónico mediante Thymeleaf.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Qué hace:
     * Construye las variables de contexto (número de orden, fecha de aprobación, dirección de entrega, nombre del proveedor)
     * y procesa la plantilla Thymeleaf `templates/plantilla-correo.html` para generar el cuerpo HTML final del mensaje.
     * 
     * A dónde apunta:
     * - Plantilla Thymeleaf: `templates/plantilla-correo.html`
     * 
     * @param orden Entidad {@link OrdenCompra} aprobada.
     * @return Cadena con el contenido HTML formateado del correo.
     */
    public String generarCorreoOrdenAprobada(
            OrdenCompra orden
    ) {

        Context context = new Context();

        context.setVariable(
                "numeroOrden",
                orden.getNumeroOrden()
        );

        context.setVariable(
                "fechaAprobacion",
                orden.getFechaAprobacion()
        );

        context.setVariable(
                "direccionEntrega",
                orden.getCentroCosto() != null ? orden.getCentroCosto().getDireccion() : ""
        );

        context.setVariable(
                "proveedor",
                orden.getProveedor() != null ? orden.getProveedor().getNombre() : ""
        );

        return templateEngine.process(
                "plantilla-correo",
                context
        );
    }
}