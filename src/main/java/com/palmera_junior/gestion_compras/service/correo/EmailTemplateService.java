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

    /**
     * Qué hace:
     * Construye el asunto del correo de notificación de facturación, incluyendo el número de orden.
     * 
     * @param orden Entidad {@link OrdenCompra} recién facturada.
     * @return Asunto del correo.
     */
    public String generarAsuntoFacturacion(OrdenCompra orden) {
        return "Orden de Compra Facturada - N° " + orden.getNumeroOrden();
    }

    /**
     * Qué hace:
     * Construye el cuerpo en texto plano del correo de notificación de facturación.
     * 
     * @param orden Entidad {@link OrdenCompra} recién facturada.
     * @return Cuerpo del mensaje en texto plano.
     */
    public String generarCuerpoFacturacion(OrdenCompra orden) {
        String proveedor = orden.getProveedor() != null ? orden.getProveedor().getNombre() : "N/A";
        String numeroFactura = orden.getNumeroFactura() != null ? orden.getNumeroFactura() : "N/A";

        return "Se registró correctamente la facturación de la Orden de Compra N° " + orden.getNumeroOrden() + ".\n\n"
                + "Proveedor: " + proveedor + "\n"
                + "N° Factura proveedor: " + numeroFactura + "\n\n"
                + "Se adjunta la evidencia digital del soporte de factura del proveedor.\n\n"
                + "Este es un mensaje automático, por favor no responder.";
    }
}
