package com.palmera_junior.gestion_compras.service.correo;

import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/** Envía a los aprobadores de las sedes involucradas el aviso de una póliza nueva. */
@Service
@RequiredArgsConstructor
public class CorreoNuevaPolizaService {

    private static final Logger log = LoggerFactory.getLogger(CorreoNuevaPolizaService.class);
    private static final String ASUNTO = "NUEVA POLIZA PENDIENTE DE APROBACION";

    private final UsuarioRepository usuarioRepository;
    private final PolizaRepository polizaRepository;
    private final IEmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Async("emailExecutor")
    public void notificarAprobadores(Integer idPoliza, List<Integer> idsSedes) {
        if (idPoliza == null || idsSedes == null || idsSedes.isEmpty()) {
            return;
        }
        Poliza poliza = polizaRepository.findWithRelationsByIdPoliza(idPoliza)
                .orElseThrow(() -> new IllegalStateException("No existe la póliza " + idPoliza));

        usuarioRepository.findByRolAndSedeIdSedeIn(Rol.APROBADOR, idsSedes).stream()
                .filter(usuario -> StringUtils.hasText(usuario.getEmail()))
                .filter(distintosPorCorreo())
                .forEach(usuario -> {
                    try {
                        String html = emailTemplateService.generarCorreoNuevaPolizaPendiente(poliza,
                                nombreCompleto(usuario), usuario.getSede() == null ? "N/A" : usuario.getSede().getNombre());
                        emailService.enviarNotificacionHtml(usuario.getEmail().trim(), ASUNTO, html);
                    } catch (Exception exception) {
                        log.error("No fue posible notificar la nueva póliza al aprobador {}", usuario.getEmail(), exception);
                    }
                });
    }

    private java.util.function.Predicate<Usuario> distintosPorCorreo() {
        java.util.Set<String> correos = java.util.concurrent.ConcurrentHashMap.newKeySet();
        return usuario -> correos.add(usuario.getEmail().trim().toLowerCase(java.util.Locale.ROOT));
    }

    private String nombreCompleto(Usuario usuario) {
        return (usuario.getNombre() + " " + usuario.getApellido()).trim();
    }
}
