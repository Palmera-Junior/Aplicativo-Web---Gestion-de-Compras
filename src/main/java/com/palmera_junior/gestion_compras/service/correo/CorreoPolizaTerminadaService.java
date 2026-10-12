package com.palmera_junior.gestion_compras.service.correo;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** Informa la terminación de una póliza a su creador y a los aprobadores involucrados. */
@Service
@RequiredArgsConstructor
public class CorreoPolizaTerminadaService {

    private static final Logger log = LoggerFactory.getLogger(CorreoPolizaTerminadaService.class);
    private static final String ASUNTO = "PÓLIZA TERMINADA";

    private final PolizaRepository polizaRepository;
    private final UsuarioRepository usuarioRepository;
    private final IEmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Async("emailExecutor")
    public void notificar(Integer idPoliza, List<Integer> idsSedes) {
        if (idPoliza == null) {
            return;
        }
        try {
            Poliza poliza = polizaRepository.findWithRelationsByIdPoliza(idPoliza)
                    .orElseThrow(() -> new IllegalStateException("No existe la póliza " + idPoliza));
            Set<String> correos = ConcurrentHashMap.newKeySet();
            notificarUsuario(poliza.getUsuario(), poliza, correos);
            if (idsSedes != null && !idsSedes.isEmpty()) {
                usuarioRepository.findByRolAndSedeIdSedeIn(Rol.APROBADOR, idsSedes)
                        .forEach(aprobador -> notificarUsuario(aprobador, poliza, correos));
            }
        } catch (Exception exception) {
            log.error("No fue posible procesar las notificaciones de terminación para la póliza {}", idPoliza, exception);
        }
    }

    private void notificarUsuario(Usuario destinatario, Poliza poliza, Set<String> correos) {
        if (destinatario == null || !StringUtils.hasText(destinatario.getEmail())) {
            return;
        }
        String correo = destinatario.getEmail().trim();
        if (!correos.add(correo.toLowerCase(Locale.ROOT))) {
            return;
        }
        try {
            String html = emailTemplateService.generarCorreoPolizaTerminada(poliza, nombreCompleto(destinatario));
            emailService.enviarNotificacionHtml(correo, ASUNTO, html);
        } catch (Exception exception) {
            log.error("No fue posible notificar la póliza terminada al usuario {}", correo, exception);
        }
    }

    private String nombreCompleto(Usuario usuario) {
        return (usuario.getNombre() + " " + usuario.getApellido()).trim();
    }
}
