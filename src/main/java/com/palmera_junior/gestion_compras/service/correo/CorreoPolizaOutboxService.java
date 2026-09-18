package com.palmera_junior.gestion_compras.service.correo;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.AuditoriaEnvioCorreoPoliza;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.repository.AuditoriaEnvioCorreoPolizaRepository;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;
import com.palmera_junior.gestion_compras.service.poliza.IPolizaArchivoStorage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorreoPolizaOutboxService {

    private static final int MAX_INTENTOS = 4;
    private static final Set<EstadoEnvioCorreo> ESTADOS_RECLAMABLES = Set.of(
            EstadoEnvioCorreo.PENDIENTE, EstadoEnvioCorreo.REINTENTAR);

    private final AuditoriaEnvioCorreoPolizaRepository auditoriaRepository;
    private final PolizaRepository polizaRepository;
    private final IPolizaArchivoStorage archivoStorage;
    private final IEmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Value("${correo.notificacion-contabilidad}")
    private String correoContabilidad;

    @Transactional
    public Long registrarPendiente(Poliza poliza) {
        AuditoriaEnvioCorreoPoliza auditoria = new AuditoriaEnvioCorreoPoliza();
        auditoria.setPoliza(poliza);
        if (StringUtils.hasText(correoContabilidad)) {
            auditoria.setDestinatario(correoContabilidad.trim());
            auditoria.setEstado(EstadoEnvioCorreo.PENDIENTE);
        } else {
            auditoria.setDestinatario("sin-correo@invalid.local");
            auditoria.setEstado(EstadoEnvioCorreo.FALLIDO);
            auditoria.setUltimoError("No está configurado el correo de contabilidad");
            auditoria.setProximoIntento(null);
        }
        return auditoriaRepository.save(auditoria).getId();
    }

    @Transactional
    public void procesar(Long idAuditoria) {
        LocalDateTime ahora = LocalDateTime.now();
        if (auditoriaRepository.reclamarParaEnvio(idAuditoria, ESTADOS_RECLAMABLES, ahora) == 0) {
            return;
        }

        AuditoriaEnvioCorreoPoliza auditoria = auditoriaRepository.findById(idAuditoria).orElseThrow();
        try {
            Poliza poliza = polizaRepository.findWithRelationsByIdPoliza(auditoria.getPoliza().getIdPoliza())
                    .orElseThrow(() -> new IllegalStateException("No existe la póliza " + auditoria.getPoliza().getIdPoliza()));
            byte[] pdf = archivoStorage.leer(poliza.getContratoAdjunto());
            String asunto = emailTemplateService.generarAsuntoPolizaAprobada(poliza);
            String cuerpo = emailTemplateService.generarCuerpoPolizaAprobada(poliza);
            emailService.enviarNotificacionPoliza(auditoria.getDestinatario(), asunto, cuerpo, pdf,
                    "Contrato_Poliza_" + poliza.getNumeroContrato() + ".pdf");

            auditoria.setEstado(EstadoEnvioCorreo.ENVIADO);
            auditoria.setEnviadoEn(LocalDateTime.now());
            auditoria.setBloqueadoEn(null);
            auditoria.setProximoIntento(null);
            auditoria.setUltimoError(null);
            auditoria.setActualizadoEn(LocalDateTime.now());
        } catch (Exception exception) {
            registrarFallo(auditoria, exception);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEnvioCorreoPoliza> pendientesParaProcesar() {
        return auditoriaRepository.findTop50ByEstadoInAndProximoIntentoLessThanEqualOrderByCreadoEnAsc(
                ESTADOS_RECLAMABLES, LocalDateTime.now());
    }

    @Transactional
    public int liberarProcesamientosAtascados() {
        LocalDateTime ahora = LocalDateTime.now();
        return auditoriaRepository.liberarProcesamientosAtascados(ahora.minusMinutes(15), ahora);
    }

    @PreAuthorize("hasAnyRole('APROBADOR', 'ADMINISTRADOR', 'SUPERADMINISTRADOR')")
    @Transactional
    public AuditoriaEnvioCorreoPoliza marcarEnviadoManualmente(Long idAuditoria, String descripcion) {
        if (!StringUtils.hasText(descripcion)) {
            throw new IllegalArgumentException("Debe ingresar una descripción del fallo.");
        }
        AuditoriaEnvioCorreoPoliza auditoria = auditoriaRepository.findById(idAuditoria)
                .orElseThrow(() -> new IllegalStateException("No existe el envío de correo de la póliza."));
        if (auditoria.getEstado() != EstadoEnvioCorreo.FALLIDO) {
            throw new IllegalStateException("Solo se puede marcar como enviado un correo en estado FALLIDO.");
        }
        auditoria.setEstado(EstadoEnvioCorreo.ENVIADO);
        auditoria.setEnviadoEn(LocalDateTime.now());
        auditoria.setUltimoError(descripcion.trim());
        auditoria.setActualizadoEn(LocalDateTime.now());
        return auditoriaRepository.save(auditoria);
    }

    private void registrarFallo(AuditoriaEnvioCorreoPoliza auditoria, Exception exception) {
        int intentos = auditoria.getIntentos();
        auditoria.setBloqueadoEn(null);
        String mensaje = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        auditoria.setUltimoError(mensaje.length() <= 2000 ? mensaje : mensaje.substring(0, 2000));
        auditoria.setActualizadoEn(LocalDateTime.now());
        if (intentos >= MAX_INTENTOS) {
            auditoria.setEstado(EstadoEnvioCorreo.FALLIDO);
            auditoria.setProximoIntento(null);
        } else {
            auditoria.setEstado(EstadoEnvioCorreo.REINTENTAR);
            auditoria.setProximoIntento(LocalDateTime.now().plusMinutes(1L << (intentos - 1)));
        }
    }
}