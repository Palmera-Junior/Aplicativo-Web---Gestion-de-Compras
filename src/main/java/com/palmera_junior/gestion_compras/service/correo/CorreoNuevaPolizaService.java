package com.palmera_junior.gestion_compras.service.correo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.PolizaSedeAprobacion;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** Envía avisos de aprobación a los aprobadores de las sedes involucradas. */
@Service
@RequiredArgsConstructor
public class CorreoNuevaPolizaService {

    private static final Logger log = LoggerFactory.getLogger(CorreoNuevaPolizaService.class);
    private static final String ASUNTO = "PÓLIZA PENDIENTE POR APROBAR";
    private static final String ASUNTO_MODIFICACION = "PÓLIZA MODIFICADA - PENDIENTE DE NUEVA APROBACIÓN";
    private static final String PERMISO_APROBAR_POLIZAS = "POLIZAS_APROBAR";

    private final UsuarioRepository usuarioRepository;
    private final PolizaRepository polizaRepository;
    private final IEmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Async("emailExecutor")
    public void notificarAprobadores(Integer idPoliza, List<Integer> idsSedes) {
        notificarAprobadores(idPoliza, idsSedes, false);
    }

    @Async("emailExecutor")
    public void notificarAprobadoresTrasModificacion(Integer idPoliza, List<Integer> idsSedes) {
        notificarAprobadores(idPoliza, idsSedes, true);
    }

    private void notificarAprobadores(Integer idPoliza, List<Integer> idsSedes, boolean modificada) {
        if (idPoliza == null || idsSedes == null || idsSedes.isEmpty()) {
            return;
        }
        Poliza poliza = polizaRepository.findWithSedesAprobacionByIdPoliza(idPoliza)
                .orElseThrow(() -> new IllegalStateException("No existe la póliza " + idPoliza));
        if (poliza.getEstado() != EstadoPoliza.BORRADOR) {
            return;
        }

        List<Integer> idsSedesUnicos = idsSedes.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (idsSedesUnicos.isEmpty()) {
            return;
        }

        List<Usuario> destinatarios = usuarioRepository.buscarUsuariosPorSedes(idsSedesUnicos).stream()
                .filter(this::puedeAprobarPolizas)
                .filter(usuario -> idsSedesUnicos.stream().anyMatch(usuario::tieneSedeAsignada))
                .filter(usuario -> StringUtils.hasText(usuario.getEmail()))
                .filter(distintosPorCorreo())
                .toList();

        if (destinatarios.isEmpty()) {
            log.warn("No hay usuarios con permiso para aprobar la póliza {} en las sedes {}", idPoliza, idsSedesUnicos);
            return;
        }

        destinatarios.forEach(usuario -> enviarCorreoAprobacion(poliza, usuario, idsSedesUnicos, modificada));
    }

    private void enviarCorreoAprobacion(
            Poliza poliza, Usuario usuario, List<Integer> idsSedes, boolean modificada) {
        try {
            String html = modificada
                    ? emailTemplateService.generarCorreoPolizaModificadaPendiente(
                            poliza, nombreCompleto(usuario), sedesAprobacion(poliza, usuario, idsSedes))
                    : emailTemplateService.generarCorreoNuevaPolizaPendiente(
                            poliza, nombreCompleto(usuario), sedesAprobacion(poliza, usuario, idsSedes));
            emailService.enviarNotificacionHtml(
                    usuario.getEmail().trim(), modificada ? ASUNTO_MODIFICACION : ASUNTO, html);
        } catch (Exception exception) {
            log.error("No fue posible notificar la póliza al aprobador {}", usuario.getEmail(), exception);
        }
    }

    private boolean puedeAprobarPolizas(Usuario usuario) {
        return usuario.esSuperAdministrador() || usuario.getPermisos().stream()
                .anyMatch(permiso -> PERMISO_APROBAR_POLIZAS.equals(permiso.getCodigo()));
    }

    private String sedesAprobacion(Poliza poliza, Usuario usuario, List<Integer> idsSedes) {
        Set<Integer> idsSedesUsuario = usuario.getIdsSedesAsignadas();
        String sedes = poliza.getAprobacionesPorSede().stream()
                .map(PolizaSedeAprobacion::getSede)
                .filter(java.util.Objects::nonNull)
                .filter(sede -> idsSedes.contains(sede.getIdSede())
                        && idsSedesUsuario.contains(sede.getIdSede()))
                .map(sede -> sede.getNombre())
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(", "));
        if (StringUtils.hasText(sedes)) {
            return sedes;
        }
        return poliza.getSede() == null ? "N/A" : poliza.getSede().getNombre();
    }

    private java.util.function.Predicate<Usuario> distintosPorCorreo() {
        java.util.Set<String> correos = java.util.concurrent.ConcurrentHashMap.newKeySet();
        return usuario -> correos.add(usuario.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
    }

    private String nombreCompleto(Usuario usuario) {
        return (usuario.getNombre() + " " + usuario.getApellido()).trim();
    }
}
