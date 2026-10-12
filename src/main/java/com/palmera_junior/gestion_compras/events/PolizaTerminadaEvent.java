package com.palmera_junior.gestion_compras.events;

import java.util.List;

/** Evento publicado al confirmar la terminación de una póliza. */
public record PolizaTerminadaEvent(Integer idPoliza, List<Integer> idsSedesInvolucradas) {
}
