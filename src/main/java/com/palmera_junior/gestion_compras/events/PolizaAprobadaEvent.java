package com.palmera_junior.gestion_compras.events;

/** Evento publicado después de aprobar una póliza y registrar su correo pendiente. */
public record PolizaAprobadaEvent(Long idAuditoria) {
}