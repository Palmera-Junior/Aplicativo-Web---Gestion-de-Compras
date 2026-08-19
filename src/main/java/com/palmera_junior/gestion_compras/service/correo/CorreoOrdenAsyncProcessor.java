package com.palmera_junior.gestion_compras.service.correo;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorreoOrdenAsyncProcessor {

    private final CorreoOrdenOutboxService correoOrdenOutboxService;

    @Async("emailExecutor")
    public void procesar(Long idAuditoria) {
        correoOrdenOutboxService.procesar(idAuditoria);
    }
}
