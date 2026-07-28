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

    // ==========================================
    // PALETA DE COLORES (misma línea visual que
    // el dashboard web: verde institucional,
    // grises sutiles en vez de negro/rojo puro)
    // ==========================================
    private static final Color COLOR_PRIMARY = new Color(25, 135, 84);      // #198754
    private static final Color COLOR_TEXTO = new Color(33, 37, 41);        // #212529
    private static final Color COLOR_TEXTO_MUTED = new Color(108, 117, 125); // #6c757d
    private static final Color COLOR_BORDE = new Color(206, 212, 218);      // #ced4da

    // ==========================================
    // DATOS DE CONTROL DEL DOCUMENTO (encabezado)
    // ==========================================
    private static final String CODIGO_DOCUMENTO = "CA-F-01";
    private static final String VERSION_DOCUMENTO = "09";
    private static final String FECHA_VERSION_DOCUMENTO = "03/08/2026";

    /**
     * Construye el encabezado tipo "formato controlado": dos celdas para
     * logos a la izquierda, el título centrado en medio, y el bloque de
     * Código/Versión/Fecha a la derecha (igual a la plantilla de Excel).
     */
    private PdfPTable construirEncabezado(Font fontTitulo, Font fontInfo) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(3);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{21f, 50f, 29f});

        // ---- Columna izquierda: 2 celdas para logos ----
        PdfPTable logosTable = new PdfPTable(1);
        logosTable.setWidthPercentage(100);

        PdfPCell logoCell1 = new PdfPCell();
        logoCell1.setFixedHeight(35f);
        logoCell1.setBorderColor(Color.BLACK);
        // TODO: reemplazar por el logo real, por ejemplo:
        // Image logo1 = Image.getInstance("ruta/al/logo1.png");
        // logo1.scaleToFit(80f, 30f);
        // logoCell1.addElement(logo1);
        logosTable.addCell(logoCell1);

        PdfPCell logoCell2 = new PdfPCell();
        logoCell2.setFixedHeight(35f);
        logoCell2.setBorderColor(Color.BLACK);
        // TODO: reemplazar por el segundo logo real de la misma forma
        logosTable.addCell(logoCell2);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setFixedHeight(70f); // 35 + 35, mismo total que la columna derecha
        leftCell.setPadding(0);
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(logosTable);
        headerTable.addCell(leftCell);

        // ---- Columna central: título ----
        PdfPCell centerCell = new PdfPCell(new Phrase("ORDEN DE COMPRA", fontTitulo));
        centerCell.setFixedHeight(70f); // mismo alto total que logos e info
        centerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        centerCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        centerCell.setBorderColor(Color.BLACK);
        headerTable.addCell(centerCell);

        // ---- Columna derecha: Código / Versión / Fecha ----
        PdfPTable infoTable = new PdfPTable(1);
        infoTable.setWidthPercentage(100);

        PdfPCell codigoCell = new PdfPCell(new Phrase("Código: " + CODIGO_DOCUMENTO, fontInfo));
        codigoCell.setFixedHeight(23.33f); // 70 / 3, para repartir el mismo total
        codigoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        codigoCell.setBorderColor(Color.BLACK);
        infoTable.addCell(codigoCell);

        PdfPCell versionCell = new PdfPCell(new Phrase("Versión:" + VERSION_DOCUMENTO, fontInfo));
        versionCell.setFixedHeight(23.33f);
        versionCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        versionCell.setBorderColor(Color.BLACK);
        infoTable.addCell(versionCell);

        PdfPCell fechaCell = new PdfPCell(new Phrase("Fecha:" + FECHA_VERSION_DOCUMENTO, fontInfo));
        fechaCell.setFixedHeight(23.34f); // 23.33+23.33+23.34 = 70 exacto
        fechaCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        fechaCell.setBorderColor(Color.BLACK);
        infoTable.addCell(fechaCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setFixedHeight(70f); // mismo total que leftCell y centerCell
        rightCell.setPadding(0);
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.addElement(infoTable);
        headerTable.addCell(rightCell);

        return headerTable;
    }

    private void agregarSeparador(Document document) throws DocumentException {
    LineSeparator ls = new LineSeparator();
    ls.setLineWidth(0.75f);
    ls.setPercentage(100);
    ls.setLineColor(COLOR_BORDE);

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

        // Fuentes profesionales (paleta alineada al dashboard: verde
        // institucional para acentos, gris institucional para texto,
        // gris muted itálico para notas en vez de rojo de alerta)
        Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_TEXTO);
        Font fontSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY);
        Font fontTexto = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_TEXTO);
        Font fontalert = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, COLOR_TEXTO_MUTED);
        Font fontHeaderTabla = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

        // Encabezado tipo formato controlado (logos + título + código/versión/fecha)
        Font fontInfoEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_TEXTO);
        document.add(construirEncabezado(fontTitulo, fontInfoEncabezado));
        document.add(new Paragraph(" "));
        agregarSeparador(document);

        // Sección: Datos Generales
        Paragraph seccionGenerales = new Paragraph("DATOS GENERALES", fontSub);
        seccionGenerales.setSpacingBefore(4f);
        seccionGenerales.setSpacingAfter(4f);
        document.add(seccionGenerales);

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
        solicitante.addElement(new Paragraph("CENTRO COSTO:   " + (orden.getCentroCosto() != null ? orden.getCentroCosto().getCodigo() + " - " + orden.getCentroCosto().getNombre() : "N/A"), fontTexto));
        fechaTable.addCell(solicitante);
        

        document.add(fechaTable);
        agregarSeparador(document);

        // Sección: Datos del Proveedor
        Paragraph seccionProveedor = new Paragraph("DATOS DEL PROVEEDOR", fontSub);
        seccionProveedor.setSpacingBefore(4f);
        seccionProveedor.setSpacingAfter(4f);
        document.add(seccionProveedor);


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
            c.setBackgroundColor(COLOR_PRIMARY); // Verde institucional, igual al encabezado de tabla del dashboard
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
        notaCell.setBorderColor(COLOR_BORDE);
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
        // Auditoría: Aprobación y Recepción
        PdfPTable auditoriaTable = new PdfPTable(2);
        auditoriaTable.setWidthPercentage(100);

        String aproboText = orden.getUsuarioAprobacion() != null ?
            orden.getUsuarioAprobacion().getNombre() + " " + orden.getUsuarioAprobacion().getApellido() : "N/A";

        PdfPCell aprobo = new PdfPCell();
        aprobo.setBorder(Rectangle.NO_BORDER);
        aprobo.addElement(new Paragraph("APROBÓ: " + aproboText, fontTexto));
        auditoriaTable.addCell(aprobo);

        String recibioText = orden.getRecibidoPor() != null ? orden.getRecibidoPor() : "N/A";
        PdfPCell recibio = new PdfPCell();
        recibio.setBorder(Rectangle.NO_BORDER);
        recibio.addElement(new Paragraph("RECIBIÓ: " + recibioText, fontTexto));
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

        // Sección: Datos de Envío
        Paragraph seccionEnvio = new Paragraph("DATOS DE ENVÍO", fontSub);
        seccionEnvio.setSpacingBefore(4f);
        seccionEnvio.setSpacingAfter(4f);
        datos.addElement(seccionEnvio);

        String fechaRecibido = orden.getFechaRecepcion() != null ? orden.getFechaRecepcion().toString() : "N/A";
        Paragraph p4 = new Paragraph("FECHA RECIBIDO: " + fechaRecibido, fontTexto);
        p4.setSpacingAfter(6f);

        String numeroFactura = orden.getNumeroFactura() != null ? orden.getNumeroFactura() : "N/A";
        Paragraph pFactura = new Paragraph("NÚM. FACTURA PROVEEDOR: " + numeroFactura, fontTexto);
        pFactura.setSpacingAfter(6f);

        String destino = "N/A";
        if (orden.getCentroCosto() != null) {
            String dir = orden.getCentroCosto().getDireccion() != null ? orden.getCentroCosto().getDireccion() : "";
            String nombreCentro = orden.getCentroCosto().getNombre() != null ? orden.getCentroCosto().getNombre() : "";
            destino = (dir + " , " + nombreCentro).trim();
        }
        Paragraph p5 = new Paragraph("DESTINO: " + destino, fontTexto);

        datos.addElement(p4);
        datos.addElement(pFactura);
        datos.addElement(p5);
        
        envioTable.addCell(datos);
        
        document.add(envioTable);
        document.add(new Paragraph(" "));



        document.close();
        return baos.toByteArray();
    }
}