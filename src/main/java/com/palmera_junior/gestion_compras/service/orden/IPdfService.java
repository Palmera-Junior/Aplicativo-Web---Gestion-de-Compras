package com.palmera_junior.gestion_compras.service.orden;

import com.palmera_junior.gestion_compras.entity.OrdenCompra;

/**
 * Contrato de servicio para la generación dinámica de documentos PDF de Órdenes de Compra.
 */
public interface IPdfService {

    /**
     * Qué hace:
     * Genera el documento binario en formato PDF (OpenPDF / iText) de una orden de compra aprobada,
     * formateando encabezados institucionales, tablas de detalle, firmas y totales en una sola página autoescalada.
     * 
     * A dónde apunta:
     * - Generación en memoria (OpenPDF/iText) retornando `byte[]`.
     * 
     * @param orden Entidad {@link OrdenCompra} con sus datos completos y detalles.
     * @return Arreglo de bytes del archivo PDF generado.
     * @throws Exception Si ocurre un fallo en el renderizado del documento PDF.
     */
    byte[] generarPdfOrdenCompra(OrdenCompra orden) throws Exception;
}

