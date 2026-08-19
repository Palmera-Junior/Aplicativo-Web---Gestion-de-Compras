package com.palmera_junior.gestion_compras.service.correo;


import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    public String generarCorreoOrdenAprobada(
            OrdenCompra orden
    ) {

        Context context = new Context();

        context.setVariable(
                "numeroOrden",
                orden.getNumeroOrden()
        );

        context.setVariable(
                "fechaAprobacion",
                orden.getFechaAprobacion()
        );

        context.setVariable(
                "direccionEntrega",
                orden.getCentroCosto().getDireccion()
        );

        context.setVariable(
                "proveedor",
                orden.getProveedor().getNombre()
        );

        return templateEngine.process(
                "plantilla-correo",
                context
        );
    }
}