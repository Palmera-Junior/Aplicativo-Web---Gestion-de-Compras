package com.palmera_junior.gestion_compras.events;

import java.util.List;

/** Evento publicado después de confirmar la creación de una póliza en borrador. */
public record NuevaPolizaCreadaEvent(Integer idPoliza, List<Integer> idsSedesInvolucradas) {
}
