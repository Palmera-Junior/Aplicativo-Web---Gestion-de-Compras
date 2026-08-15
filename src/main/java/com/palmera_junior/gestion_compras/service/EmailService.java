package com.palmera_junior.gestion_compras.service;




public interface EmailService {

    void enviarOrdenAprobada(
            String destinatario,
            String html,
            byte[] pdf
    ) throws Exception;

}