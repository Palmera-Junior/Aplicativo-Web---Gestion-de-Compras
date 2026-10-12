package com.palmera_junior.gestion_compras.service.poliza;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Ejecuta diariamente el procesamiento de vencimientos de pólizas.
 *
 * <p>Por defecto corre a las 00:05. El horario se puede cambiar con la propiedad
 * {@code polizas.vencimiento.cron}. La tarea delega la lógica a {@link IPolizaService}:
 * {@code PolizaService} actualiza las pólizas vencidas en el repositorio y publica
 * eventos para avisos de próximo vencimiento. Estos eventos llegan a
 * {@code PolizaProximaAVencerListener}, que solicita el correo al creador.</p>
 */
@Component
@RequiredArgsConstructor
public class PolizaVencimientoScheduler {

    private final IPolizaService polizaService;

    /** Inicia el procesamiento automático según la expresión cron configurada. */
    @Scheduled(cron = "${polizas.vencimiento.cron:0 5 0 * * *}")
    public void procesarVencimientosPolizas() {
        polizaService.marcarVencidas();
    }
}
