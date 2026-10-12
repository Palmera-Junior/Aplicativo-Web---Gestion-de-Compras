package com.palmera_junior.gestion_compras.Listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.palmera_junior.gestion_compras.events.NuevaPolizaCreadaEvent;
import com.palmera_junior.gestion_compras.service.correo.CorreoNuevaPolizaService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NuevaPolizaCreadaListener {

    private final CorreoNuevaPolizaService correoNuevaPolizaService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarAprobadores(NuevaPolizaCreadaEvent event) {
        correoNuevaPolizaService.notificarAprobadores(event.idPoliza(), event.idsSedesInvolucradas());
    }
}
