package com.palmera_junior.gestion_compras.service.correo;




public interface IEmailService {

    void enviarOrdenAprobada(
            String destinatario,
            String html,
            byte[] pdf
    ) throws Exception;

}