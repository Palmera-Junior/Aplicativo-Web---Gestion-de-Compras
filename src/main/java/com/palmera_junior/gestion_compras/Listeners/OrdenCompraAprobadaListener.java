package com.palmera_junior.gestion_compras.Listeners;

import com.palmera_junior.gestion_compras.events.OrdenCompraAprobadaEvent;
import com.palmera_junior.gestion_compras.service.correo.CorreoOrdenAsyncProcessor;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener transaccional de eventos de dominio para órdenes aprobadas.
 * Garantiza que el intento inmediato de envío de correo ocurra únicamente después del COMMIT exitoso de la transacción.
 */
@Component
@RequiredArgsConstructor
public class OrdenCompraAprobadaListener {

    private final CorreoOrdenAsyncProcessor correoOrdenAsyncProcessor;

    /**
     * Qué hace:
     * Escucha el evento {@link OrdenCompraAprobadaEvent} disparado tras la aprobación de una orden,
     * y solicita su procesamiento asíncrono en segundo plano sólo después de que la orden y la auditoría
     * hayan sido confirmadas en la base de datos (TransactionPhase.AFTER_COMMIT).
     * 
     * A dónde apunta:
     * - Servicio procesador: {@link CorreoOrdenAsyncProcessor#procesar(Long)}
     * 
     * @param event Evento con el ID del registro de auditoría outbox.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void procesarOrdenAprobada(
            OrdenCompraAprobadaEvent event
    ) {

        correoOrdenAsyncProcessor.procesar(event.idAuditoria());
    }
}

