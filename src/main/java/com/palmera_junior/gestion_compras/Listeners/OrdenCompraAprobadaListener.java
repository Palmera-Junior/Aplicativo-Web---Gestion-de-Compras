package com.palmera_junior.gestion_compras.Listeners;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.events.OrdenCompraAprobadaEvent;
import com.palmera_junior.gestion_compras.repository.OrdenCompraRepository;
import com.palmera_junior.gestion_compras.service.EmailService;
import com.palmera_junior.gestion_compras.service.EmailTemplateService;
import com.palmera_junior.gestion_compras.service.PdfService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class OrdenCompraAprobadaListener {

    private final OrdenCompraRepository ordenCompraRepository;
    private final PdfService pdfService;
    private final EmailService emailService;
    private final EmailTemplateService emailTemplateService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void procesarOrdenAprobada(
            OrdenCompraAprobadaEvent event
    ) {

        try {

            OrdenCompra orden =
                    ordenCompraRepository
                            .findById(event.idOrden())
                            .orElseThrow();

            byte[] pdf =
                    pdfService.generarPdfOrdenCompra(
                            orden
                    );

            String html =
                    emailTemplateService
                            .generarCorreoOrdenAprobada(
                                    orden
                            );

            emailService.enviarOrdenAprobada(
                    orden.getProveedor().getCorreo(),
                    html,
                    pdf
            );

            System.out.println(
                    "Correo enviado al proveedor: "
                            + orden.getProveedor().getCorreo()
            );

        } catch (Exception e) {

            e.printStackTrace();

        }
    }
}