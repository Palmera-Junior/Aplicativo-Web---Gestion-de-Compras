package com.palmera_junior.gestion_compras.Listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.palmera_junior.gestion_compras.events.PolizaTerminadaEvent;
import com.palmera_junior.gestion_compras.service.correo.CorreoPolizaTerminadaService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PolizaTerminadaListener {

    private final CorreoPolizaTerminadaService correoPolizaTerminadaService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarTerminacion(PolizaTerminadaEvent event) {
        correoPolizaTerminadaService.notificar(event.idPoliza(), event.idsSedesInvolucradas());
    }
}
