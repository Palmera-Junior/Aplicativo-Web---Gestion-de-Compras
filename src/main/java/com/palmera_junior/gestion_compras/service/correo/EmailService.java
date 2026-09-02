package com.palmera_junior.gestion_compras.service.correo;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio encargado del envío de correos electrónicos mediante {@link JavaMailSender}.
 * Inyecta el remitente institucional configurado y adjunta el PDF de la orden de compra.
 */
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

        private final JavaMailSender mailSender;

        @Value("${correo.remitente}")
        private String remitente;

        /**
         * Qué hace:
         * Genera un mensaje MIME UTF-8 con cuerpo HTML y el archivo PDF adjunto ("Orden_Compra.pdf"),
         * y lo despacha a través del servidor SMTP configurado.
         *
         * A dónde apunta:
         * - Protocolo SMTP mediante {@link JavaMailSender#send(MimeMessage)}
         * - Cuenta remitente: propiedad `correo.remitente`
         *
         * @param destinatario Dirección de correo del destinatario.
         * @param html Plantilla HTML procesada.
         * @param pdf Arreglo de bytes del PDF generado.
         * @throws Exception Si falla el transporte SMTP o la creación del mensaje.
         */
        @Override
        public void enviarOrdenAprobada(
                        String destinatario,
                        String html,
                        byte[] pdf) throws Exception {

                MimeMessage message = mailSender.createMimeMessage();

                MimeMessageHelper helper = new MimeMessageHelper(
                                message,
                                true,
                                "UTF-8");

                helper.setFrom(remitente);

                helper.setTo(destinatario);

                helper.setSubject(
                                "Nueva Orden de Compra Aprobada");

                helper.setText(
                                html,
                                true);

                helper.addAttachment(
                                "Orden_Compra.pdf",
                                new ByteArrayResource(pdf));

                mailSender.send(message);
        }

        /**
         * Qué hace:
         * Genera un mensaje MIME UTF-8 de texto plano con el PDF de la orden (incluye la evidencia
         * de factura ya embebida) adjunto y lo despacha a través del servidor SMTP configurado.
         *
         * A dónde apunta:
         * - Protocolo SMTP mediante {@link JavaMailSender#send(MimeMessage)}
         * - Cuenta remitente: propiedad `correo.remitente`
         *
         * @param destinatario Dirección de correo fija de notificación de facturación.
         * @param asunto Asunto del mensaje.
         * @param cuerpo Cuerpo del mensaje en texto plano.
         * @param pdf Arreglo de bytes del PDF de la orden facturada.
         * @throws Exception Si falla el transporte SMTP o la creación del mensaje.
         */
        @Override
        public void enviarNotificacionFacturacion(
                        String destinatario,
                        String asunto,
                        String cuerpo,
                        byte[] pdf) throws Exception {

                MimeMessage message = mailSender.createMimeMessage();

                MimeMessageHelper helper = new MimeMessageHelper(
                                message,
                                true,
                                "UTF-8");

                helper.setFrom(remitente);

                helper.setTo(destinatario);

                helper.setSubject(asunto);

                helper.setText(
                                cuerpo,
                                false);

                helper.addAttachment(
                                "Orden_Compra.pdf",
                                new ByteArrayResource(pdf));

                mailSender.send(message);
        }
}
