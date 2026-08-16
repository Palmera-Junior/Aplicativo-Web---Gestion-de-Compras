package com.palmera_junior.gestion_compras.Listeners;

import com.palmera_junior.gestion_compras.events.OrdenCompraAprobadaEvent;
import com.palmera_junior.gestion_compras.service.CorreoOrdenAsyncProcessor;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrdenCompraAprobadaListener {

    private final CorreoOrdenAsyncProcessor correoOrdenAsyncProcessor;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void procesarOrdenAprobada(
            OrdenCompraAprobadaEvent event
    ) {

        correoOrdenAsyncProcessor.procesar(event.idAuditoria());
    }
}
