package com.palmera_junior.gestion_compras.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.AuditoriaEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.repository.AuditoriaEnvioCorreoRepository;
import com.palmera_junior.gestion_compras.repository.OrdenCompraRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorreoOrdenOutboxService {

    private static final Logger log = LoggerFactory.getLogger(CorreoOrdenOutboxService.class);
    private static final int MAX_INTENTOS = 4;
    private static final Set<EstadoEnvioCorreo> ESTADOS_RECLAMABLES = Set.of(
            EstadoEnvioCorreo.PENDIENTE, EstadoEnvioCorreo.REINTENTAR);

    private final AuditoriaEnvioCorreoRepository auditoriaRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Transactional
    public Long registrarPendiente(OrdenCompra orden) {
        AuditoriaEnvioCorreo auditoria = new AuditoriaEnvioCorreo();
        auditoria.setOrdenCompra(orden);
        String destinatario = orden.getProveedor() == null ? null : orden.getProveedor().getCorreo();

        if (!StringUtils.hasText(destinatario)) {
            auditoria.setDestinatario("sin-correo@invalid.local");
            auditoria.setEstado(EstadoEnvioCorreo.FALLIDO);
            auditoria.setUltimoError("La orden aprobada no tiene un correo de proveedor válido");
            auditoria.setProximoIntento(null);
        } else {
            auditoria.setDestinatario(destinatario.trim());
            auditoria.setEstado(EstadoEnvioCorreo.PENDIENTE);
        }
        return auditoriaRepository.save(auditoria).getId();
    }

    @Transactional
    public void procesar(Long idAuditoria) {
        LocalDateTime ahora = LocalDateTime.now();
        if (auditoriaRepository.reclamarParaEnvio(idAuditoria, ESTADOS_RECLAMABLES, ahora) == 0) {
            return;
        }

        AuditoriaEnvioCorreo auditoria = auditoriaRepository.findById(idAuditoria).orElseThrow();
        try {
            OrdenCompra orden = ordenCompraRepository.buscarParaEnvioCorreo(auditoria.getOrdenCompra().getIdOrden())
                    .orElseThrow(() -> new IllegalStateException("No existe la orden " + auditoria.getOrdenCompra().getIdOrden()));

            byte[] pdf = pdfService.generarPdfOrdenCompra(orden);
            String html = emailTemplateService.generarCorreoOrdenAprobada(orden);
            emailService.enviarOrdenAprobada(auditoria.getDestinatario(), html, pdf);

            auditoria.setEstado(EstadoEnvioCorreo.ENVIADO);
            auditoria.setEnviadoEn(LocalDateTime.now());
            auditoria.setBloqueadoEn(null);
            auditoria.setProximoIntento(null);
            auditoria.setUltimoError(null);
            auditoria.setActualizadoEn(LocalDateTime.now());
            log.info("Correo de orden {} enviado a {} (auditoría {})", orden.getIdOrden(), auditoria.getDestinatario(), idAuditoria);
        } catch (Exception ex) {
            registrarFallo(auditoria, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEnvioCorreo> pendientesParaProcesar() {
        return auditoriaRepository.findTop50ByEstadoInAndProximoIntentoLessThanEqualOrderByCreadoEnAsc(
                ESTADOS_RECLAMABLES, LocalDateTime.now());
    }

    // Devuelve el estado del último intento de envío por cada orden, para pintar
    // el ícono de estado de correo en la columna Acciones del dashboard
    @Transactional(readOnly = true)
    public Map<Integer, EstadoEnvioCorreo> obtenerEstadosPorOrdenes(java.util.Collection<Integer> idsOrdenes) {
        if (idsOrdenes == null || idsOrdenes.isEmpty()) {
            return Map.of();
        }
        Map<Integer, EstadoEnvioCorreo> estados = new LinkedHashMap<>();
        auditoriaRepository.findByOrdenCompra_IdOrdenInOrderByIdDesc(idsOrdenes)
                .forEach(auditoria -> estados.putIfAbsent(auditoria.getOrdenCompra().getIdOrden(), auditoria.getEstado()));
        return estados;
    }

    @Transactional
    public int liberarProcesamientosAtascados() {
        LocalDateTime ahora = LocalDateTime.now();
        return auditoriaRepository.liberarProcesamientosAtascados(ahora.minusMinutes(15), ahora);
    }

    // Permite al usuario confirmar que envió el correo manualmente (fuera del sistema)
    // tras un fallo definitivo, dejando registrada la justificación en 'ultimoError'.
    @PreAuthorize("hasAnyRole('SOLICITANTE', 'APROBADOR')")
    @Transactional
    public AuditoriaEnvioCorreo marcarEnviadoManualmente(Integer idOrden, String descripcionFallo) {
        if (!StringUtils.hasText(descripcionFallo)) {
            throw new IllegalArgumentException("Debe ingresar una descripción del fallo.");
        }

        AuditoriaEnvioCorreo auditoria = auditoriaRepository.findFirstByOrdenCompra_IdOrdenOrderByIdDesc(idOrden)
                .orElseThrow(() -> new IllegalStateException("No existe un envío de correo registrado para esta orden."));

        if (auditoria.getEstado() != EstadoEnvioCorreo.FALLIDO) {
            throw new IllegalStateException(
                    "Solo se puede marcar como enviado manualmente un correo en estado FALLIDO.");
        }

        String descripcion = descripcionFallo.trim();
        auditoria.setUltimoError(descripcion.length() <= 2000 ? descripcion : descripcion.substring(0, 2000));
        auditoria.setEstado(EstadoEnvioCorreo.ENVIADO);
        auditoria.setEnviadoEn(LocalDateTime.now());
        auditoria.setBloqueadoEn(null);
        auditoria.setProximoIntento(null);
        auditoria.setActualizadoEn(LocalDateTime.now());

        log.info("Correo de orden {} marcado como ENVIADO manualmente (auditoría {})", idOrden, auditoria.getId());
        return auditoriaRepository.save(auditoria);
    }

    private void registrarFallo(AuditoriaEnvioCorreo auditoria, Exception ex) {
        int intentos = auditoria.getIntentos();
        auditoria.setBloqueadoEn(null);
        auditoria.setUltimoError(resumenError(ex));
        auditoria.setActualizadoEn(LocalDateTime.now());

        if (intentos >= MAX_INTENTOS) {
            auditoria.setEstado(EstadoEnvioCorreo.FALLIDO);
            auditoria.setProximoIntento(null);
            log.error("Correo de auditoría {} falló definitivamente tras {} intentos", auditoria.getId(), intentos, ex);
            return;
        }

        auditoria.setEstado(EstadoEnvioCorreo.REINTENTAR);
        auditoria.setProximoIntento(LocalDateTime.now().plusMinutes(1L << (intentos - 1)));
        log.warn("Correo de auditoría {} falló; se reintentará por intento {}", auditoria.getId(), intentos, ex);
    }

    private String resumenError(Exception ex) {
        String mensaje = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        return mensaje.length() <= 2000 ? mensaje : mensaje.substring(0, 2000);
    }
}
