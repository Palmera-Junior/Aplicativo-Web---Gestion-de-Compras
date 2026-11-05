package com.palmera_junior.gestion_compras.rrhh.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;
import com.palmera_junior.gestion_compras.rrhh.entity.AuditoriaEnvioCorreoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.AuditoriaEnvioCorreoEmpleadoRepository;
import com.palmera_junior.gestion_compras.service.correo.EmailTemplateService;
import com.palmera_junior.gestion_compras.service.correo.IEmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorreoEmpleadoOutboxService {

    private static final Logger log = LoggerFactory.getLogger(CorreoEmpleadoOutboxService.class);
    private static final String EVENTO_REGISTRO_INICIAL = "REGISTRO_INICIAL_CREADO";
    private static final int MAX_INTENTOS = 4;
    private static final Set<EstadoEnvioCorreo> ESTADOS_RECLAMABLES =
            Set.of(EstadoEnvioCorreo.PENDIENTE, EstadoEnvioCorreo.REINTENTAR);

    private final AuditoriaEnvioCorreoEmpleadoRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final IEmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Transactional
    public int registrarNotificacionesRegistroInicial(Empleado empleado) {
        List<Usuario> aprobadores = usuarioRepository.buscarAprobadoresRrhhConCorreo();
        int registrados = 0;
        for (String destinatario : aprobadores.stream()
                .map(Usuario::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .distinct()
                .toList()) {
            if (auditoriaRepository.existsByEmpleado_IdEmpleadoAndDestinatarioIgnoreCaseAndClaveEvento(
                    empleado.getIdEmpleado(), destinatario, EVENTO_REGISTRO_INICIAL)) {
                continue;
            }
            AuditoriaEnvioCorreoEmpleado auditoria = new AuditoriaEnvioCorreoEmpleado();
            auditoria.setEmpleado(empleado);
            auditoria.setDestinatario(destinatario);
            auditoria.setClaveEvento(EVENTO_REGISTRO_INICIAL);
            auditoria.setEstado(EstadoEnvioCorreo.PENDIENTE);
            auditoria.setProximoIntento(ahoraUtc());
            auditoriaRepository.save(auditoria);
            registrados++;
        }
        if (registrados == 0) {
            log.warn("No hay usuarios APROBADOR_RRHH con correo para notificar el empleado {}",
                    empleado.getIdEmpleado());
        }
        return registrados;
    }

    @Transactional
    public void procesar(Long idAuditoria) {
        OffsetDateTime ahora = ahoraUtc();
        if (auditoriaRepository.reclamarParaEnvio(
                idAuditoria, ESTADOS_RECLAMABLES, ahora) == 0) {
            return;
        }
        AuditoriaEnvioCorreoEmpleado auditoria = auditoriaRepository.findById(idAuditoria)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe la auditoría de correo " + idAuditoria));
        try {
            Empleado empleado = auditoria.getEmpleado();
            String html = emailTemplateService.generarCorreoNuevoEmpleado(
                    empleado,
                    "/recursos-humanos/principal?empleadoId=" + empleado.getIdEmpleado());
            emailService.enviarNotificacionHtml(
                    auditoria.getDestinatario(), "Nuevo empleado para revisión en RR. HH.", html);
            auditoria.setEstado(EstadoEnvioCorreo.ENVIADO);
            auditoria.setEnviadoEn(ahoraUtc());
            auditoria.setBloqueadoEn(null);
            auditoria.setProximoIntento(null);
            auditoria.setUltimoError(null);
        } catch (Exception ex) {
            registrarFallo(auditoria, ex);
        }
    }

    @Transactional(readOnly = true)
    public List<AuditoriaEnvioCorreoEmpleado> pendientesParaProcesar() {
        return auditoriaRepository.findTop50ByEstadoInAndProximoIntentoLessThanEqualOrderByCreadoEnAsc(
                ESTADOS_RECLAMABLES, ahoraUtc());
    }

    @Transactional
    public int liberarProcesamientosAtascados() {
        OffsetDateTime ahora = ahoraUtc();
        return auditoriaRepository.liberarProcesamientosAtascados(
                ahora.minusMinutes(15), ahora);
    }

    private void registrarFallo(AuditoriaEnvioCorreoEmpleado auditoria, Exception ex) {
        int intentos = auditoria.getIntentos();
        String mensaje = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        auditoria.setUltimoError(mensaje.length() <= 4000 ? mensaje : mensaje.substring(0, 4000));
        auditoria.setBloqueadoEn(null);
        OffsetDateTime ahora = ahoraUtc();
        if (intentos >= MAX_INTENTOS) {
            auditoria.setEstado(EstadoEnvioCorreo.FALLIDO);
            auditoria.setProximoIntento(null);
            log.error("Notificación RR. HH. {} falló definitivamente tras {} intentos",
                    auditoria.getIdAuditoriaCorreoEmpleado(), intentos, ex);
            return;
        }
        auditoria.setEstado(EstadoEnvioCorreo.REINTENTAR);
        auditoria.setProximoIntento(ahora.plusMinutes(1L << (intentos - 1)));
        log.warn("Falló notificación RR. HH. {}; se programó reintento {}",
                auditoria.getIdAuditoriaCorreoEmpleado(), intentos, ex);
    }

    private static OffsetDateTime ahoraUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
