package com.palmera_junior.gestion_compras.service.correo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Tarea programada en segundo plano que sondea y despacha periódicamente los correos pendientes o fallidos.
 */
@Component
@RequiredArgsConstructor
public class CorreoOrdenOutboxScheduler {

    private final CorreoOrdenOutboxService correoOrdenOutboxService;
    private final CorreoOrdenAsyncProcessor correoOrdenAsyncProcessor;

    /**
     * Qué hace:
     * Ejecuta a intervalos regulares (por defecto cada 30 segundos) la liberación de bloqueos atascados
     * y el despacho asíncrono de todos los correos en estado PENDIENTE o REINTENTAR listos para envío.
     * 
     * A dónde apunta:
     * - Configuración: propiedad `correo.outbox.intervalo-ms` (default 30000 ms)
     * - Servicios: {@link CorreoOrdenOutboxService#liberarProcesamientosAtascados()}, {@link CorreoOrdenOutboxService#pendientesParaProcesar()} y {@link CorreoOrdenAsyncProcessor#procesar(Long)}
     */
    @Scheduled(fixedDelayString = "${correo.outbox.intervalo-ms:30000}")
    public void procesarPendientes() {
        correoOrdenOutboxService.liberarProcesamientosAtascados();
        correoOrdenOutboxService.pendientesParaProcesar()
                .forEach(auditoria -> correoOrdenAsyncProcessor.procesar(auditoria.getId()));
    }
}

