package com.palmera_junior.gestion_compras.service.correo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.Poliza;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Servicio encargado del procesamiento y renderizado dinámico de plantillas HTML de correo electrónico mediante Thymeleaf.
 */
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    @Value("${correo.plataforma-url}")
    private String plataformaUrl;

    /** Renderiza el aviso HTML de una póliza en borrador para un aprobador específico. */
    public String generarCorreoNuevaPolizaPendiente(Poliza poliza, String nombreAprobador, String sedeAprobador) {
        Context context = new Context(new Locale("es", "CO"));
        String numeroPoliza = poliza.getNumeroPoliza();
        if (numeroPoliza == null || numeroPoliza.isBlank()) {
            numeroPoliza = "Contrato " + poliza.getNumeroContrato();
        }
        String creador = poliza.getUsuario() == null ? "Un usuario"
                : (poliza.getUsuario().getNombre() + " " + poliza.getUsuario().getApellido()).trim();

        context.setVariable("nombreAprobador", nombreAprobador);
        context.setVariable("numeroPoliza", numeroPoliza);
        context.setVariable("numeroContrato", poliza.getNumeroContrato());
        context.setVariable("tituloResumen", "Nueva Póliza");
        context.setVariable("cliente", poliza.getCliente());
        context.setVariable("proveedor", poliza.getProveedor() == null ? "N/A" : poliza.getProveedor().getNombre());
        context.setVariable("sede", sedeAprobador);
        context.setVariable("fechaInicio", poliza.getFechaCreacion());
        context.setVariable("fechaFin", poliza.getFechaVencimiento());
        context.setVariable("creador", creador);
        context.setVariable("fechaCreacion", poliza.getFechaCreacion());
        context.setVariable("urlRevision", plataformaUrl.replaceAll("/$", "") + "/polizas");
        context.setVariable("encabezado", "SISTEMA DE GESTIÓN · PÓLIZAS");
        context.setVariable("etiqueta", "◷  PENDIENTE DE TU APROBACIÓN");
        context.setVariable("mensaje", "Se ha creado una nueva póliza que requiere tu revisión y aprobación antes de continuar el proceso. A continuación encontrarás el resumen:");
        context.setVariable("textoBoton", "Revisar y aprobar póliza");
        context.setVariable("advertencia", "Esta póliza permanecerá en estado Borrador hasta que sea aprobada. Si no se gestiona a tiempo, puede afectar el inicio de la vigencia del contrato.");
        return templateEngine.process("poliza-pendiente-aprobacion", context);
    }

    /** Reutiliza la plantilla de pólizas para advertir el próximo vencimiento al creador. */
    public String generarCorreoPolizaProximaAVencer(Poliza poliza, String nombreCreador) {
        Context context = new Context(new Locale("es", "CO"));
        long diasRestantes = ChronoUnit.DAYS.between(java.time.LocalDate.now(), poliza.getFechaVencimiento());
        context.setVariable("nombreAprobador", nombreCreador);
        context.setVariable("numeroPoliza", poliza.getNumeroPoliza() == null || poliza.getNumeroPoliza().isBlank()
                ? "Contrato " + poliza.getNumeroContrato() : poliza.getNumeroPoliza());
        context.setVariable("numeroContrato", poliza.getNumeroContrato());
        context.setVariable("tituloResumen", "Póliza próxima a vencer");
        context.setVariable("cliente", poliza.getCliente());
        context.setVariable("proveedor", poliza.getProveedor() == null ? "N/A" : poliza.getProveedor().getNombre());
        context.setVariable("sede", poliza.getSede() == null ? "N/A" : poliza.getSede().getNombre());
        context.setVariable("fechaInicio", poliza.getFechaCreacion());
        context.setVariable("fechaFin", poliza.getFechaVencimiento());
        context.setVariable("creador", nombreCreador);
        context.setVariable("fechaCreacion", poliza.getFechaCreacion());
        context.setVariable("urlRevision", plataformaUrl.replaceAll("/$", "") + "/polizas");
        context.setVariable("encabezado", "SISTEMA DE GESTIÓN · PÓLIZAS");
        context.setVariable("etiqueta", "⚠  PRÓXIMA A VENCER");
        context.setVariable("mensaje", "La póliza está próxima a vencer. Faltan " + diasRestantes
                + " día(s) para su vencimiento. Revise la información y gestione su renovación oportunamente.");
        context.setVariable("textoBoton", "Revisar póliza");
        context.setVariable("advertencia", "Esta es una alerta automática de vencimiento. Si no se gestiona a tiempo, la vigencia del contrato puede verse afectada.");
        return templateEngine.process("poliza-pendiente-aprobacion", context);
    }

    /** Reutiliza la plantilla HTML para informar la terminación y las primas pendientes de gestionar. */
    public String generarCorreoPolizaTerminada(Poliza poliza, String nombreDestinatario) {
        Context context = new Context(new Locale("es", "CO"));
        String creador = poliza.getUsuario() == null ? "N/A"
                : (poliza.getUsuario().getNombre() + " " + poliza.getUsuario().getApellido()).trim();
        List<String> primas = poliza.getDetallesPrima().stream()
                .map(prima -> prima.getDescripcion() + ": " + formatearPesos(prima.getValor()))
                .toList();
        context.setVariable("nombreAprobador", nombreDestinatario);
        context.setVariable("numeroPoliza", poliza.getNumeroPoliza() == null || poliza.getNumeroPoliza().isBlank()
                ? "Contrato " + poliza.getNumeroContrato() : poliza.getNumeroPoliza());
        context.setVariable("numeroContrato", poliza.getNumeroContrato());
        context.setVariable("tituloResumen", "Póliza terminada");
        context.setVariable("cliente", poliza.getCliente());
        context.setVariable("proveedor", poliza.getProveedor() == null ? "N/A" : poliza.getProveedor().getNombre());
        context.setVariable("sede", poliza.getSede() == null ? "N/A" : poliza.getSede().getNombre());
        context.setVariable("fechaInicio", poliza.getFechaCreacion());
        context.setVariable("fechaFin", poliza.getFechaVencimiento());
        context.setVariable("creador", creador);
        context.setVariable("fechaCreacion", poliza.getFechaTerminacion());
        context.setVariable("urlRevision", plataformaUrl.replaceAll("/$", "") + "/polizas");
        context.setVariable("encabezado", "SISTEMA DE GESTIÓN · PÓLIZAS");
        context.setVariable("etiqueta", "■  PÓLIZA TERMINADA");
        context.setVariable("mensaje", "La póliza ha sido terminada. Cada sede involucrada debe solicitar el valor restante de las primas relacionadas a continuación.");
        context.setVariable("textoBoton", "Revisar póliza terminada");
        context.setVariable("advertencia", "Motivo de terminación: " + (poliza.getMotivoTerminacion() == null ? "No registrado" : poliza.getMotivoTerminacion()));
        context.setVariable("primasTerminacion", primas);
        return templateEngine.process("poliza-pendiente-aprobacion", context);
    }

    /** Reutiliza la plantilla HTML para informar a Contabilidad que una póliza fue aprobada. */
    public String generarCorreoPolizaAprobadaContabilidad(Poliza poliza) {
        Context context = new Context(new Locale("es", "CO"));
        String creador = poliza.getUsuario() == null ? "N/A"
                : (poliza.getUsuario().getNombre() + " " + poliza.getUsuario().getApellido()).trim();
        String aprobador = poliza.getUsuarioAprobacion() == null ? "N/A"
                : (poliza.getUsuarioAprobacion().getNombre() + " " + poliza.getUsuarioAprobacion().getApellido()).trim();
        context.setVariable("nombreAprobador", "Departamento de Contabilidad");
        context.setVariable("numeroPoliza", poliza.getNumeroPoliza() == null || poliza.getNumeroPoliza().isBlank()
                ? "Contrato " + poliza.getNumeroContrato() : poliza.getNumeroPoliza());
        context.setVariable("numeroContrato", poliza.getNumeroContrato());
        context.setVariable("tituloResumen", "Póliza aprobada");
        context.setVariable("cliente", poliza.getCliente());
        context.setVariable("proveedor", poliza.getProveedor() == null ? "N/A" : poliza.getProveedor().getNombre());
        context.setVariable("sede", poliza.getSede() == null ? "N/A" : poliza.getSede().getNombre());
        context.setVariable("fechaInicio", poliza.getFechaCreacion());
        context.setVariable("fechaFin", poliza.getFechaVencimiento());
        context.setVariable("creador", creador);
        context.setVariable("fechaCreacion", poliza.getFechaAprobacion());
        context.setVariable("urlRevision", plataformaUrl.replaceAll("/$", "") + "/polizas");
        context.setVariable("encabezado", "SISTEMA DE GESTIÓN · PÓLIZAS");
        context.setVariable("etiqueta", "✓  PÓLIZA APROBADA");
        context.setVariable("mensaje", "La póliza fue aprobada por " + aprobador
                + ". Se adjunta el contrato en PDF para su gestión contable.");
        context.setVariable("textoBoton", "Revisar póliza aprobada");
        context.setVariable("advertencia", "Por favor valide el contrato adjunto y realice el proceso contable correspondiente.");
        context.setVariable("mostrarSaludo", false);
        context.setVariable("mostrarEnlace", false);
        return templateEngine.process("poliza-pendiente-aprobacion", context);
    }

    /**
     * Qué hace:
     * Construye las variables de contexto (número de orden, fecha de aprobación, dirección de entrega, nombre del proveedor)
     * y procesa la plantilla Thymeleaf `templates/plantilla-correo.html` para generar el cuerpo HTML final del mensaje.
     * 
     * A dónde apunta:
     * - Plantilla Thymeleaf: `templates/plantilla-correo.html`
     * 
     * @param orden Entidad {@link OrdenCompra} aprobada.
     * @return Cadena con el contenido HTML formateado del correo.
     */
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
                orden.getCentroCosto() != null ? orden.getCentroCosto().getDireccion() : ""
        );

        context.setVariable(
                "proveedor",
                orden.getProveedor() != null ? orden.getProveedor().getNombre() : ""
        );

        return templateEngine.process(
                "plantilla-correo",
                context
        );
    }

    /**
     * Qué hace:
     * Construye el asunto del correo de notificación de facturación, incluyendo el número de orden.
     * 
     * @param orden Entidad {@link OrdenCompra} recién facturada.
     * @return Asunto del correo.
     */
    public String generarAsuntoFacturacion(OrdenCompra orden) {
        String centroCosto = orden.getCentroCosto() == null ? "N/A" : orden.getCentroCosto().getNombre();
        return "Orden de Compra Facturada para " + centroCosto + " - " + orden.getNumeroOrden();
    }

    /**
     * Qué hace:
     * Construye el cuerpo HTML de notificación de facturación reutilizando la plantilla verde de pólizas.
     * 
     * @param orden Entidad {@link OrdenCompra} recién facturada.
     * @return Cuerpo HTML del mensaje.
     */
    public String generarCorreoFacturacion(OrdenCompra orden) {
        Context context = new Context(new Locale("es", "CO"));
        String numeroOrden = orden.getNumeroOrden() == null ? "N/A" : orden.getNumeroOrden();
        String proveedor = orden.getProveedor() == null ? "N/A" : orden.getProveedor().getNombre();
        String sede = orden.getSede() == null ? "N/A" : orden.getSede().getNombre();
        String centroCosto = orden.getCentroCosto() == null ? "N/A" : orden.getCentroCosto().getNombre();
        String fechaOrden = orden.getFecha() == null ? "N/A"
                : orden.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", new Locale("es", "CO")));

        context.setVariable("numeroContrato", numeroOrden);
        context.setVariable("etiquetaNumero", "Número de orden");
        context.setVariable("numeroFactura", orden.getNumeroFactura() == null ? "N/A" : orden.getNumeroFactura());
        context.setVariable("tituloResumen", "Orden de compra facturada");
        context.setVariable("proveedor", proveedor);
        context.setVariable("sede", sede);
        context.setVariable("etiquetaSede", "Sede");
        context.setVariable("centroCosto", centroCosto);
        context.setVariable("totalOrden", formatearPesos(orden.getTotal()));
        context.setVariable("fechaOrden", fechaOrden);
        context.setVariable("mostrarVigencia", false);
        context.setVariable("mostrarCreacion", false);
        context.setVariable("mostrarSaludo", false);
        context.setVariable("mostrarEnlace", false);
        context.setVariable("encabezado", "SISTEMA DE GESTIÓN · COMPRAS");
        context.setVariable("etiqueta", "✓  FACTURADA · GESTIÓN CONTABLE");
        context.setVariable("mensaje", "La orden de compra fue registrada como facturada. Encontrará el resumen y el soporte disponible para continuar con la gestión contable.");
        context.setVariable("advertencia", "Se adjunta el PDF de la orden de compra con la evidencia de factura disponible.");
        context.setVariable("tipoAviso", "info");
        context.setVariable("iconoAviso", "ℹ");
        return templateEngine.process("poliza-pendiente-aprobacion", context);
    }

        public String generarAsuntoPolizaAprobada(Poliza poliza) {
                return "Póliza aprobada para contabilidad - " + poliza.getNumeroContrato();
        }

        public String generarCuerpoPolizaAprobada(Poliza poliza) {
                String proveedor = poliza.getProveedor() == null ? "N/A" : poliza.getProveedor().getNombre();
                String sede = poliza.getSede() == null ? "N/A" : poliza.getSede().getNombre();
                String aprobador = poliza.getUsuarioAprobacion() == null ? "N/A"
                                : (poliza.getUsuarioAprobacion().getNombre() + " " + poliza.getUsuarioAprobacion().getApellido()).trim();
                return "Se aprobó la póliza para registro contable.\n\n"
                                + "Número de contrato: " + poliza.getNumeroContrato() + "\n"
                                + "Proveedor: " + proveedor + "\n"
                                + "Cliente: " + poliza.getCliente() + "\n"
                                + "Valor de la prima: " + formatearPesos(poliza.getValorPrima()) + "\n"
                                + "Valor del contrato: " + formatearPesos(poliza.getValorContrato()) + "\n"
                                + "Sede: " + sede + "\n"
                                + "Fecha de aprobación: " + poliza.getFechaAprobacion() + "\n"
                                + "Aprobada por: " + aprobador + "\n\n"
                                + "Se adjunta el contrato en formato PDF.";
        }

        private String formatearPesos(BigDecimal valor) {
                if (valor == null) {
                        return "N/A";
                }
                NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
                formato.setMinimumFractionDigits(0);
                formato.setMaximumFractionDigits(0);
                return formato.format(valor.setScale(0, RoundingMode.HALF_UP));
        }
}
