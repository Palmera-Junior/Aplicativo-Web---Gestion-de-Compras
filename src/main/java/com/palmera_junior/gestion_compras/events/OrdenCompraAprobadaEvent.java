package com.palmera_junior.gestion_compras.events;

/**
 * Evento de dominio publicado tras la aprobación exitosa de una Orden de Compra.
 * Transporta el identificador del registro outbox para iniciar el envío asíncrono de correo y PDF.
 * 
 * @param idAuditoria Identificador del registro en la tabla `auditoria_envio_correo`.
 */
public record OrdenCompraAprobadaEvent(
        Long idAuditoria) {
}
