package com.palmera_junior.gestion_compras.service.poliza;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PolizaVencimientoScheduler {

    private final IPolizaService polizaService;

    @Scheduled(cron = "${polizas.vencimiento.cron:0 5 0 * * *}")
    public void marcarPolizasVencidas() {
        polizaService.marcarVencidas();
    }
}