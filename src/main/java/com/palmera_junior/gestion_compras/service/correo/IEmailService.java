package com.palmera_junior.gestion_compras.service.correo;

/**
 * Contrato de servicio para el envío de notificaciones por correo electrónico SMTP.
 */
public interface IEmailService {

    /**
     * Qué hace:
     * Construye y envía un correo electrónico MIME multipart en formato HTML con el documento PDF adjunto.
     * 
     * A dónde apunta:
     * - Servidor SMTP externo (configurado en application.properties vía Spring Mail).
     * 
     * @param destinatario Dirección de correo del proveedor receptor.
     * @param html Contenido del cuerpo del mensaje en formato HTML renderizado.
     * @param pdf Arreglo de bytes con el documento PDF de la orden.
     * @throws Exception Si ocurre un fallo en la conexión SMTP o en el armado del mensaje MIME.
     */
    void enviarOrdenAprobada(
            String destinatario,
            String html,
            byte[] pdf
    ) throws Exception;

    /**
     * Qué hace:
     * Construye y envía un correo electrónico MIME de texto plano con el PDF de la orden
     * (incluye la evidencia de factura ya embebida) adjunto.
     * 
     * A dónde apunta:
     * - Servidor SMTP externo (configurado en application.properties vía Spring Mail).
     * 
     * @param destinatario Dirección de correo fija de notificación de facturación.
     * @param asunto Asunto del mensaje (incluye el número de orden).
     * @param cuerpo Cuerpo del mensaje en texto plano.
     * @param pdf Arreglo de bytes con el PDF de la orden facturada.
     * @throws Exception Si ocurre un fallo en la conexión SMTP o en el armado del mensaje MIME.
     */
    void enviarNotificacionFacturacion(
            String destinatario,
            String asunto,
            String cuerpo,
            byte[] pdf
    ) throws Exception;

}
