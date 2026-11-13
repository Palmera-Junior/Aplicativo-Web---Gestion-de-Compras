package com.palmera_junior.gestion_compras.events;

import java.util.List;

/** Evento publicado después de modificar una póliza y reiniciar sus aprobaciones por sede. */
public record PolizaModificadaEvent(Integer idPoliza, List<Integer> idsSedesInvolucradas) {
}
