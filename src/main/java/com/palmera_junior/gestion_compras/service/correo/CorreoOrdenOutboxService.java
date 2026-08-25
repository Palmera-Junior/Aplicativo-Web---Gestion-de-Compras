package com.palmera_junior.gestion_compras.service.correo;

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
import com.palmera_junior.gestion_compras.service.orden.PdfService;

import lombok.RequiredArgsConstructor;

/**
 * Servicio transaccional que implementa el patrón Transactional Outbox para el envío confiable de correos electrónicos.
 * Garantiza que ante caídas del servidor SMTP o fallos de red, los correos pendientes se reintenten automáticamente
 * con retroceso exponencial (exponential backoff) y queden auditados en la base de datos.
 */
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
    private final IEmailService emailService;
    private final EmailTemplateService emailTemplateService;

    /**
     * Qué hace:
     * Registra un nuevo registro en la tabla de auditoría en estado PENDIENTE dentro de la misma transacción
     * en que la orden de compra fue aprobada. Si el proveedor no tiene correo, lo marca como FALLIDO.
     * 
     * A dónde apunta:
     * - Repositorio: {@link AuditoriaEnvioCorreoRepository#save(Object)}
     * - Tabla: auditoria_envio_correo
     * 
     * @param orden Orden de compra aprobada.
     * @return ID del registro de auditoría creado.
     */
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

    /**
     * Qué hace:
     * Reclama de forma exclusiva y atómica un registro outbox (bloqueo optimista/pesimista),
     * genera el PDF de la orden, procesa la plantilla HTML y realiza el despacho vía SMTP.
     * Si tiene éxito, transiciona el estado a ENVIADO; si falla, programa un reintento con backoff.
     * 
     * A dónde apunta:
     * - Repositorios: {@link AuditoriaEnvioCorreoRepository}, {@link OrdenCompraRepository}
     * - Servicios: {@link PdfService#generarPdfOrdenCompra}, {@link EmailTemplateService#generarCorreoOrdenAprobada}, {@link IEmailService#enviarOrdenAprobada}
     * 
     * @param idAuditoria Identificador del registro en auditoria_envio_correo.
     */
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

    /**
     * Qué hace:
     * Consulta hasta 50 registros outbox que estén listos para procesar o reintentar (proximoIntento <= ahora).
     * 
     * A dónde apunta:
     * - Repositorio: {@link AuditoriaEnvioCorreoRepository#findTop50ByEstadoInAndProximoIntentoLessThanEqualOrderByCreadoEnAsc}
     * 
     * @return Lista de auditorías pendientes.
     */
    @Transactional(readOnly = true)
    public List<AuditoriaEnvioCorreo> pendientesParaProcesar() {
        return auditoriaRepository.findTop50ByEstadoInAndProximoIntentoLessThanEqualOrderByCreadoEnAsc(
                ESTADOS_RECLAMABLES, LocalDateTime.now());
    }

    /**
     * Qué hace:
     * Devuelve el último estado de envío de correo para un conjunto de IDs de órdenes,
     * permitiendo mostrar el ícono de estado (PENDIENTE, ENVIADO, FALLIDO) en la tabla del dashboard.
     * 
     * A dónde apunta:
     * - Repositorio: {@link AuditoriaEnvioCorreoRepository#findByOrdenCompra_IdOrdenInOrderByIdDesc}
     * 
     * @param idsOrdenes Colección de identificadores de órdenes visibles en la página actual.
     * @return Mapa de [idOrden -> EstadoEnvioCorreo].
     */
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

    /**
     * Qué hace:
     * Libera bloqueos de registros outbox que hayan quedado trabados por más de 15 minutos por caídas intempestivas del servidor.
     * 
     * A dónde apunta:
     * - Repositorio: {@link AuditoriaEnvioCorreoRepository#liberarProcesamientosAtascados}
     * 
     * @return Número de registros liberados.
     */
    @Transactional
    public int liberarProcesamientosAtascados() {
        LocalDateTime ahora = LocalDateTime.now();
        return auditoriaRepository.liberarProcesamientosAtascados(ahora.minusMinutes(15), ahora);
    }

    /**
     * Qué hace:
     * Permite al usuario/administrador confirmar que despachó el correo de la orden por un canal manual
     * externo tras un fallo definitivo (FALLIDO), registrando la justificación y cambiando el estado a ENVIADO.
     * 
     * A dónde apunta:
     * - Repositorio: {@link AuditoriaEnvioCorreoRepository#findFirstByOrdenCompra_IdOrdenOrderByIdDesc} y {@link AuditoriaEnvioCorreoRepository#save}
     * 
     * @param idOrden ID de la orden.
     * @param descripcionFallo Justificación o detalle del envío manual.
     * @return AuditoriaEnvioCorreo actualizada.
     */
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
