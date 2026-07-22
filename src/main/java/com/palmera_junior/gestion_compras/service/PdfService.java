package com.palmera_junior.gestion_compras.service;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.DetalleCompra;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.draw.LineSeparator;

import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;

@Service


public class PdfService {

    private void agregarSeparador(Document document) throws DocumentException {
    LineSeparator ls = new LineSeparator();
    ls.setLineWidth(1f);
    ls.setPercentage(100);

    document.add(new Chunk(ls));
    document.add(new Paragraph(" "));
    
}

    private PdfPCell crearCelda(String texto, Font font) {
    PdfPCell cell = new PdfPCell(new Phrase(texto, font));
    cell.setBorder(Rectangle.NO_BORDER);
    cell.setVerticalAlignment(Element.ALIGN_RIGHT);
    return cell;

}



    public byte[] generarPdfOrdenCompra(OrdenCompra orden) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.LETTER, 26, 26, 26, 26);
        PdfWriter.getInstance(document, baos);

        document.open();

        // Fuentes profesionales
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(40, 40, 40));
        Font fontSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(80, 80, 80));
        Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(0, 0, 0));
        Font fontalert = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.red);
        Font fontHeaderTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

        // Título del reporte
        Paragraph titulo = new Paragraph("ORDEN DE COMPRA", fontTitulo);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);
        document.add(new Paragraph(" "));
        agregarSeparador(document);

        // Tabla fecha y solicitate 

        PdfPTable fechaTable = new PdfPTable(2);
        fechaTable.setWidthPercentage(100);     

        PdfPCell fecha = new PdfPCell();
        fecha.setBorder(Rectangle.NO_BORDER);
        fecha.addElement(new Paragraph("FECHA:    " + orden.getFecha(), fontTexto));
        fecha.addElement(new Paragraph("FACTURA No.   " + orden.getNumeroOrden(), fontTexto));
        fechaTable.addCell(fecha);

        PdfPCell solicitante = new PdfPCell();
        solicitante.setBorder(Rectangle.NO_BORDER);
        solicitante.addElement(new Paragraph("SOLICITANTE:     " + orden.getUsuario().getNombre()+" "+orden.getUsuario().getApellido() +" - "+orden.getSede().getNombre(), fontTexto));
        fechaTable.addCell(solicitante);
        

        document.add(fechaTable);
        agregarSeparador(document);


        // Tabla de información del Proveedor (2 columnas)
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);

        PdfPCell cellProv = new PdfPCell();
        cellProv.setBorder(Rectangle.NO_BORDER);
        cellProv.addElement(new Paragraph("NOMBRE: " + orden.getNombreProv(), fontTexto));
        cellProv.addElement(new Paragraph("NIT: " + orden.getNitProv(), fontTexto));
        cellProv.addElement(new Paragraph("DIRECCIÓN: " + orden.getDireccionProv(), fontTexto));
        infoTable.addCell(cellProv);

        PdfPCell cellProv2 = new PdfPCell();
        cellProv2.setBorder(Rectangle.NO_BORDER);
        cellProv2.addElement(new Paragraph("CIUDAD: " + orden.getCiudadProv(), fontTexto));
        cellProv2.addElement(new Paragraph("TELEFONO: " + orden.getTelefonoProv(), fontTexto));
        cellProv2.addElement(new Paragraph("EMAIL: " + orden.getCorreoProv(), fontTexto));
        infoTable.addCell(cellProv2);

        document.add(infoTable);
        document.add(new Paragraph(" "));

        // Tabla de Productos / Detalles
        PdfPTable tablaDetalles = new PdfPTable(8);
        tablaDetalles.setWidthPercentage(100);
        tablaDetalles.setWidths(new float[]{0.8f ,1.1f, 3f, 2f, 1.3f, 1f, 1.3f, 1.6f});

        PdfPCell defaulCell=tablaDetalles.getDefaultCell();
        defaulCell.setMinimumHeight(25f);
        defaulCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        String[] headers = {"CTD", "CODIGO", "DESCRIPCIÓN", "PRESENTACION", "VALOR U.", "IVA", "VALOR IVA", "TOTAL"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Paragraph(h, fontHeaderTabla));
            c.setBackgroundColor(new Color(10, 130, 64)); // Color elegante azul oscuro/gris
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(8);
            tablaDetalles.addCell(c);
        }

        DecimalFormat df = new DecimalFormat("¤#,##0");

        if (orden.getDetalles() != null) {
            for (DetalleCompra det : orden.getDetalles()) {

                PdfPCell cCant = new PdfPCell(new Paragraph(String.valueOf(det.getCantidad()), fontTexto));
                cCant.setHorizontalAlignment(Element.ALIGN_CENTER);
                tablaDetalles.addCell(cCant);

                PdfPCell cCod = new PdfPCell(new Paragraph(String.valueOf(det.getCodigoInventario()), fontTexto));
                cCod.setHorizontalAlignment(Element.ALIGN_CENTER);
                tablaDetalles.addCell(cCod);


                tablaDetalles.addCell(new Paragraph(det.getDescripcion(), fontTexto));
                tablaDetalles.addCell(new Paragraph(det.getPresentacion(), fontTexto ));                

                PdfPCell cUnit = new PdfPCell(new Paragraph(df.format(det.getValorUnitario()), fontTexto));
                cUnit.setHorizontalAlignment(Element.ALIGN_RIGHT);
                tablaDetalles.addCell(cUnit);

                PdfPCell cIva = new PdfPCell(new Paragraph(det.getIvaProducto() + "%", fontTexto));
                cIva.setHorizontalAlignment(Element.ALIGN_CENTER);
                tablaDetalles.addCell(cIva);

                PdfPCell cVIva = new PdfPCell(new Paragraph( df.format(det.getValorIva()), fontTexto));
                cVIva.setHorizontalAlignment(Element.ALIGN_RIGHT);
                tablaDetalles.addCell(cVIva);

                PdfPCell cTotal = new PdfPCell(new Paragraph( df.format(det.getValorTotalLinea()), fontTexto));
                cTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                tablaDetalles.addCell(cTotal);
            }
        }

        document.add(tablaDetalles);
        document.add(new Paragraph(" "));

        // Tabla de Totales Generales
        PdfPTable totalesTable = new PdfPTable(2);
        totalesTable.setWidthPercentage(100);
        totalesTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
        
        totalesTable.addCell(crearCelda("Subtotal:", fontSub));
        totalesTable.addCell(crearCelda(df.format(orden.getSubTotal()), fontTexto));

        totalesTable.addCell(crearCelda("IVA Total:", fontSub));
        totalesTable.addCell(crearCelda(df.format(orden.getIvaTotal()), fontTexto));

        totalesTable.addCell(crearCelda("Descuento:", fontSub));
        totalesTable.addCell(crearCelda(df.format(orden.getDescuento()), fontTexto));

        totalesTable.addCell(crearCelda("TOTAL:", fontSub));
        totalesTable.addCell(crearCelda(df.format(orden.getTotal()), fontSub));

                // Tabla contenedora
        PdfPTable contenedor = new PdfPTable(2);
        contenedor.setWidthPercentage(100);
        contenedor.setWidths(new float[]{65, 35});
            
        // ===== TABLA INTERNA DE OBSERVACIONES =====
        PdfPTable obsTable = new PdfPTable(1);
        obsTable.setWidthPercentage(100);
            
        // Parte superior (observaciones)
        PdfPCell obsContenido = new PdfPCell();
        obsContenido.setBorder(Rectangle.NO_BORDER);
        obsContenido.setFixedHeight(80f); // Ajusta según necesites
            
        obsContenido.addElement(new Paragraph("OBSERVACIONES:", fontSub));
            
        obsContenido.addElement(
            new Paragraph(
                orden.getObservaciones() != null ? orden.getObservaciones() : "",
                fontTexto
            )
        );
        
        obsTable.addCell(obsContenido);
        
        // Parte inferior (mensaje fijo)
        PdfPCell notaCell = new PdfPCell(
            new Phrase(
                "Toda factura debe llegar al correo recepcionfacturas@palmerajunior.com de lo contrario no será aceptada",
                fontalert
            )
        );
        
        notaCell.setBorder(Rectangle.TOP);
        notaCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        notaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        
        obsTable.addCell(notaCell);
        
        // Celda contenedora de observaciones
        PdfPCell observacionesCell = new PdfPCell();
        observacionesCell.setBorder(Rectangle.NO_BORDER);
        observacionesCell.addElement(obsTable);
        
        contenedor.addCell(observacionesCell);
        
        // ===== TOTALES =====
        PdfPCell totalesCell = new PdfPCell();
        totalesCell.setBorder(Rectangle.NO_BORDER);
        totalesCell.addElement(totalesTable);
        
        contenedor.addCell(totalesCell);
        
        // Agregar al documento
        document.add(contenedor);

        agregarSeparador(document);
                
        // Aprovador y quien recibe

        PdfPTable auditoriaTable = new PdfPTable(2);
        auditoriaTable.setWidthPercentage(100);  
        
                
        PdfPCell aprobo = new PdfPCell();
        aprobo.setBorder(Rectangle.NO_BORDER);
        aprobo.addElement(new Paragraph("APROBÓ  : " + "_______________________________", fontTexto));
        auditoriaTable.addCell(aprobo);

        PdfPCell recibio = new PdfPCell();
        recibio.setBorder(Rectangle.NO_BORDER);
        recibio.addElement(new Paragraph("RECIBIÓ  : " + "_______________________________", fontTexto));
        auditoriaTable.addCell(recibio);

        document.add(auditoriaTable);
        agregarSeparador(document);
        // Datos de envío
        PdfPTable envioTable = new PdfPTable(2);
        envioTable.setWidthPercentage(100);
            
        PdfPCell palmera = new PdfPCell();
        palmera.setBorder(Rectangle.NO_BORDER);
            
        Paragraph p1 = new Paragraph("FACTURAR A NOMBRE DE: PALMERA JUNIOR S.A.S.", fontTexto);
        p1.setSpacingAfter(8f);        
        Paragraph p2 = new Paragraph("CORREO DE FACTURACION ELECTRONICA:", fontTexto );  

        Paragraph p3 = new Paragraph("recepcionfacturas@palmerajunior.com",fontalert );
        
        palmera.addElement(p1);
        palmera.addElement(p2);
        palmera.addElement(p3);
        
        envioTable.addCell(palmera);        
        
        PdfPCell datos = new PdfPCell();
        datos.setBorder(Rectangle.NO_BORDER);
        
        Paragraph p4 = new Paragraph("FECHA RECIBIDO: __________________________", fontTexto);
        p4.setSpacingAfter(8f);
        
        Paragraph p5 = new Paragraph("DESTINO: " + orden.getSede().getDireccion() + " , " + orden.getSede().getNombre(),  fontTexto  );
        
        datos.addElement(p4);
        datos.addElement(p5);
        
        envioTable.addCell(datos);
        
        document.add(envioTable);
        document.add(new Paragraph(" "));



        document.close();
        return baos.toByteArray();
    }
}


