package com.palmera_junior.gestion_compras.rrhh.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorreoEmpleadoAsyncProcessor {

    private final CorreoEmpleadoOutboxService outboxService;

    @Async("emailExecutor")
    public void procesar(Long idAuditoria) {
        outboxService.procesar(idAuditoria);
    }
}
