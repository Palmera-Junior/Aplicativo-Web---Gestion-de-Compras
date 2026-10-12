package com.palmera_junior.gestion_compras.service.correo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.PolizaRepository;

import lombok.RequiredArgsConstructor;

/** Notifica al creador cuando una póliza entra al rango de 30 días para vencerse. */
@Service
@RequiredArgsConstructor
public class CorreoPolizaVencimientoService {

    private static final Logger log = LoggerFactory.getLogger(CorreoPolizaVencimientoService.class);
    private static final String ASUNTO = "PÓLIZA PRÓXIMA A VENCER";

    private final PolizaRepository polizaRepository;
    private final IEmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @Async("emailExecutor")
    public void notificarCreador(Integer idPoliza) {
        if (idPoliza == null) {
            return;
        }
        try {
            Poliza poliza = polizaRepository.findWithRelationsByIdPoliza(idPoliza)
                    .orElseThrow(() -> new IllegalStateException("No existe la póliza " + idPoliza));
            Usuario creador = poliza.getUsuario();
            if (creador == null || !StringUtils.hasText(creador.getEmail())) {
                log.warn("La póliza {} no tiene creador con correo para notificar su vencimiento", idPoliza);
                return;
            }
            String nombreCreador = (creador.getNombre() + " " + creador.getApellido()).trim();
            String html = emailTemplateService.generarCorreoPolizaProximaAVencer(poliza, nombreCreador);
            emailService.enviarNotificacionHtml(creador.getEmail().trim(), ASUNTO, html);
        } catch (Exception exception) {
            log.error("No fue posible enviar el aviso de vencimiento de la póliza {}", idPoliza, exception);
        }
    }
}
