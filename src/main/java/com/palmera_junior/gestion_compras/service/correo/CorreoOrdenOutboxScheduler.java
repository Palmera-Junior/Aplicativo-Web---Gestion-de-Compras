package com.palmera_junior.gestion_compras.service.correo;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CorreoOrdenOutboxScheduler {

    private final CorreoOrdenOutboxService correoOrdenOutboxService;
    private final CorreoOrdenAsyncProcessor correoOrdenAsyncProcessor;

    @Scheduled(fixedDelayString = "${correo.outbox.intervalo-ms:30000}")
    public void procesarPendientes() {
        correoOrdenOutboxService.liberarProcesamientosAtascados();
        correoOrdenOutboxService.pendientesParaProcesar()
                .forEach(auditoria -> correoOrdenAsyncProcessor.procesar(auditoria.getId()));
    }
}
