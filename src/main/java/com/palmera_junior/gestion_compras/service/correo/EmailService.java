package com.palmera_junior.gestion_compras.service.correo;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

        private final JavaMailSender mailSender;

        @Value("${spring.mail.username}")
        private String remitente;

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
}