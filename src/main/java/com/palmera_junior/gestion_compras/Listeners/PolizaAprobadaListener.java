package com.palmera_junior.gestion_compras.Listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.palmera_junior.gestion_compras.events.PolizaAprobadaEvent;
import com.palmera_junior.gestion_compras.service.correo.CorreoPolizaAsyncProcessor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PolizaAprobadaListener {

    private final CorreoPolizaAsyncProcessor correoPolizaAsyncProcessor;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void procesarPolizaAprobada(PolizaAprobadaEvent event) {
        correoPolizaAsyncProcessor.procesar(event.idAuditoria());
    }
}