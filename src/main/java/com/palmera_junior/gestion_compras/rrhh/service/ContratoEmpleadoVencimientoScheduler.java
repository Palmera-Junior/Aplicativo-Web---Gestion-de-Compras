package com.palmera_junior.gestion_compras.rrhh.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ContratoEmpleadoVencimientoScheduler {

    private final IContratoEmpleadoService contratoEmpleadoService;

    @Scheduled(
            cron = "${rrhh.contratos.sincronizacion-cron:0 5 0 * * *}",
            zone = "${rrhh.zona-horaria:America/Bogota}")
    public void sincronizarEstados() {
        contratoEmpleadoService.sincronizarEstados();
    }
}
