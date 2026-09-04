package com.palmera_junior.gestion_compras.service.correo;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.RawMessage;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;

/**
 * Servicio encargado del envío de correos electrónicos mediante Amazon SES API v2.
 * Construye el mensaje MIME y adjunta el PDF de la orden de compra.
 */
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

        private final SesV2Client sesClient;

        @Value("${correo.remitente}")
        private String remitente;

        @Value("${correo.remitente-nombre}")
        private String nombreRemitente;

        /**
         * Qué hace:
         * Genera un mensaje MIME UTF-8 con cuerpo HTML y el archivo PDF adjunto ("Orden_Compra.pdf"),
         * y lo despacha a través del servidor SMTP configurado.
         *
         * A dónde apunta:
         * - Amazon SES API v2 mediante un mensaje MIME Raw.
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

                MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

                MimeMessageHelper helper = new MimeMessageHelper(
                                message,
                                true,
                                "UTF-8");

                helper.setFrom(remitente, nombreRemitente);

                helper.setTo(destinatario);

                helper.setSubject(
                                "Nueva Orden de Compra Aprobada");

                helper.setText(
                                html,
                                true);

                helper.addAttachment(
                                "Orden_Compra.pdf",
                                new ByteArrayResource(pdf));

                enviarComoRaw(message, destinatario);
        }

        /**
         * Qué hace:
         * Genera un mensaje MIME UTF-8 de texto plano con el PDF de la orden (incluye la evidencia
         * de factura ya embebida) adjunto y lo despacha a través del servidor SMTP configurado.
         *
         * A dónde apunta:
         * - Amazon SES API v2 mediante un mensaje MIME Raw.
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

                MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));

                MimeMessageHelper helper = new MimeMessageHelper(
                                message,
                                true,
                                "UTF-8");

                helper.setFrom(remitente, nombreRemitente);

                helper.setTo(destinatario);

                helper.setSubject(asunto);

                helper.setText(
                                cuerpo,
                                false);

                helper.addAttachment(
                                "Orden_Compra.pdf",
                                new ByteArrayResource(pdf));

                enviarComoRaw(message, destinatario);
        }

        private void enviarComoRaw(MimeMessage message, String destinatario) throws Exception {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                message.writeTo(output);

                RawMessage rawMessage = RawMessage.builder()
                                .data(SdkBytes.fromByteArray(output.toByteArray()))
                                .build();

                SendEmailRequest request = SendEmailRequest.builder()
                                .destination(Destination.builder().toAddresses(destinatario).build())
                                .content(EmailContent.builder().raw(rawMessage).build())
                                .build();

                sesClient.sendEmail(request);
        }
}
