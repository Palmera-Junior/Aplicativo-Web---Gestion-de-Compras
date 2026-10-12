package com.palmera_junior.gestion_compras.events;

/** Evento publicado una vez cuando una póliza ingresa al rango de 30 días para vencer. */
public record PolizaProximaAVencerEvent(Integer idPoliza) {
}
