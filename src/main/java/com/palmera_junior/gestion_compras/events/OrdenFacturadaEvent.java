package com.palmera_junior.gestion_compras.events;

/**
 * Evento de dominio publicado tras el registro exitoso de la facturación de una Orden de Compra.
 * Transporta el identificador del registro outbox para iniciar el envío asíncrono de la notificación.
 * 
 * @param idAuditoria Identificador del registro en la tabla `auditoria_envio_correo`.
 */
public record OrdenFacturadaEvent(
        Long idAuditoria) {
}
