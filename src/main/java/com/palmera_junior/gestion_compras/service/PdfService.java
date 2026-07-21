package com.palmera_junior.gestion_compras.service;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.DetalleCompra;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;

@Service
public class PdfService {

    public byte[] generarPdfOrdenCompra(OrdenCompra orden) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Fuentes profesionales
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(40, 40, 40));
        Font fontSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(80, 80, 80));
        Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(50, 50, 50));
        Font fontHeaderTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);

        // Título del reporte
        Paragraph titulo = new Paragraph("ORDEN DE COMPRA", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        Paragraph nOrden = new Paragraph("Nº de Orden: " + (orden.getNumeroOrden() != null ? orden.getNumeroOrden() : "N/A"), fontSub);
        nOrden.setAlignment(Element.ALIGN_CENTER);
        document.add(nOrden);
        document.add(new Paragraph(" ")); // Espacio en blanco

        // Tabla de información del Proveedor y Fecha (2 columnas)
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);

        PdfPCell cellProv = new PdfPCell();
        cellProv.setBorder(Rectangle.NO_BORDER);
        cellProv.addElement(new Paragraph("Proveedor: " + orden.getNombreProv(), fontTexto));
        cellProv.addElement(new Paragraph("NIT / CC: " + orden.getNitProv(), fontTexto));
        cellProv.addElement(new Paragraph("Ciudad: " + orden.getCiudadProv(), fontTexto));
        cellProv.addElement(new Paragraph("Dirección: " + orden.getDireccionProv(), fontTexto));
        infoTable.addCell(cellProv);

        PdfPCell cellFecha = new PdfPCell();
        cellFecha.setBorder(Rectangle.NO_BORDER);
        cellFecha.addElement(new Paragraph("Fecha: " + orden.getFecha(), fontTexto));
        cellFecha.addElement(new Paragraph("Teléfono: " + orden.getTelefonoProv(), fontTexto));
        cellFecha.addElement(new Paragraph("Correo: " + orden.getCorreoProv(), fontTexto));
        infoTable.addCell(cellFecha);

        document.add(infoTable);
        document.add(new Paragraph(" "));

        // Tabla de Productos / Detalles
        PdfPTable tablaDetalles = new PdfPTable(6);
        tablaDetalles.setWidthPercentage(100);
        tablaDetalles.setWidths(new float[]{1.5f, 4f, 1f, 1.5f, 1.5f, 2f});

        String[] headers = {"Código", "Descripción", "Cant", "V. Unitario", "IVA %", "Total Línea"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Paragraph(h, fontHeaderTabla));
            c.setBackgroundColor(new Color(52, 73, 94)); // Color elegante azul oscuro/gris
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(6);
            tablaDetalles.addCell(c);
        }

        DecimalFormat df = new DecimalFormat("#,##0.00");

        if (orden.getDetalles() != null) {
            for (DetalleCompra det : orden.getDetalles()) {
                tablaDetalles.addCell(new Paragraph(det.getCodigoInventario(), fontTexto));
                tablaDetalles.addCell(new Paragraph(det.getDescripcion(), fontTexto));
                
                PdfPCell cCant = new PdfPCell(new Paragraph(String.valueOf(det.getCantidad()), fontTexto));
                cCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                tablaDetalles.addCell(cCant);

                PdfPCell cUnit = new PdfPCell(new Paragraph("$ " + df.format(det.getValorUnitario()), fontTexto));
                cUnit.setHorizontalAlignment(Element.ALIGN_RIGHT);
                tablaDetalles.addCell(cUnit);

                PdfPCell cIva = new PdfPCell(new Paragraph(det.getIvaProducto() + "%", fontTexto));
                cIva.setHorizontalAlignment(Element.ALIGN_CENTER);
                tablaDetalles.addCell(cIva);

                PdfPCell cTotal = new PdfPCell(new Paragraph("$ " + df.format(det.getValorTotalLinea()), fontTexto));
                cTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                tablaDetalles.addCell(cTotal);
            }
        }

        document.add(tablaDetalles);
        document.add(new Paragraph(" "));

        // Tabla de Totales Generales
        PdfPTable totalesTable = new PdfPTable(2);
        totalesTable.setWidthPercentage(45);
        totalesTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        totalesTable.addCell(new Paragraph("Subtotal:", fontSub));
        totalesTable.addCell(new Paragraph("$ " + df.format(orden.getSubTotal()), fontTexto));

        totalesTable.addCell(new Paragraph("IVA Total:", fontSub));
        totalesTable.addCell(new Paragraph("$ " + df.format(orden.getIvaTotal()), fontTexto));

        totalesTable.addCell(new Paragraph("Descuento:", fontSub));
        totalesTable.addCell(new Paragraph("$ " + df.format(orden.getDescuento()), fontTexto));

        totalesTable.addCell(new Paragraph("TOTAL A PAGAR:", fontTitulo));
        totalesTable.addCell(new Paragraph("$ " + df.format(orden.getTotal()), fontTitulo));

        document.add(totalesTable);

        // Observaciones (Si las hay)
        if (orden.getObservaciones() != null && !orden.getObservaciones().trim().isEmpty()) {
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Observaciones:", fontSub));
            document.add(new Paragraph(orden.getObservaciones(), fontTexto));
        }

        document.close();
        return baos.toByteArray();
    }
}