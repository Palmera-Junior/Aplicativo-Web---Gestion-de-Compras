package com.palmera_junior.gestion_compras.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.service.OrdenCompraService;
import com.palmera_junior.gestion_compras.service.PdfService;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenCompraApiController {

    @Autowired
    private OrdenCompraService ordenCompraService;

    @Autowired
    private PdfService pdfService;

    @PostMapping("/guardar")
    public ResponseEntity<byte[]> guardarYGenerarPdf(@RequestBody OrdenCompraDTO ordenCompraDTO) {
        try {
            // 1. Guardar la orden y sus detalles en la base de datos
            OrdenCompra nuevaOrden = ordenCompraService.guardarOrdenDesdeDTO(ordenCompraDTO);
            
            // 2. Generar el archivo PDF en memoria con toda la información
            byte[] pdfBytes = pdfService.generarPdfOrdenCompra(nuevaOrden);

            // 3. Preparar los Headers HTTP para forzar la descarga en el navegador
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // El navegador usará este nombre por defecto para el archivo descargado
            headers.setContentDispositionFormData("attachment", "Orden_Compra_" + (nuevaOrden.getNumeroOrden() != null ? nuevaOrden.getNumeroOrden() : "reciente") + ".pdf");

            // 4. Retornar el archivo binario exitosamente
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            e.printStackTrace();
            // Si algo falla, devolver error 500 para que el frontend lo capture en el bloque .catch()
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}