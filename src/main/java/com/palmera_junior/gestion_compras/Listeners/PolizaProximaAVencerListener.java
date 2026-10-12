package com.palmera_junior.gestion_compras.Listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.palmera_junior.gestion_compras.events.PolizaProximaAVencerEvent;
import com.palmera_junior.gestion_compras.service.correo.CorreoPolizaVencimientoService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PolizaProximaAVencerListener {

    private final CorreoPolizaVencimientoService correoPolizaVencimientoService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notificarCreador(PolizaProximaAVencerEvent event) {
        correoPolizaVencimientoService.notificarCreador(event.idPoliza());
    }
}
