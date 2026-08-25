package com.palmera_junior.gestion_compras.service.correo;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Procesador asíncrono para la ejecución concurrente de envíos de correo sin bloquear los hilos principales.
 */
@Service
@RequiredArgsConstructor
public class CorreoOrdenAsyncProcessor {

    private final CorreoOrdenOutboxService correoOrdenOutboxService;

    /**
     * Qué hace:
     * Ejecuta en un hilo secundario del pool `emailExecutor` el procesamiento y despacho de un correo de orden.
     * 
     * A dónde apunta:
     * - Pool de hilos: `emailExecutor` (definido en {@link com.palmera_junior.gestion_compras.config.EmailAsyncConfig})
     * - Servicio delegado: {@link CorreoOrdenOutboxService#procesar(Long)}
     * 
     * @param idAuditoria Identificador del registro en auditoria_envio_correo.
     */
    @Async("emailExecutor")
    public void procesar(Long idAuditoria) {
        correoOrdenOutboxService.procesar(idAuditoria);
    }
}

