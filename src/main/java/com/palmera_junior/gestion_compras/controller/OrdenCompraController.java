package com.palmera_junior.gestion_compras.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.dto.RecibirOrdenDTO;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.service.IOrdenCompraService;
import com.palmera_junior.gestion_compras.service.IPdfService;

@RestController
@RequestMapping("/orden-compra")
public class OrdenCompraController {

    private final IOrdenCompraService ordenCompraService;
    private final IPdfService pdfService;

    public OrdenCompraController(IOrdenCompraService ordenCompraService, IPdfService pdfService) {
        this.ordenCompraService = ordenCompraService;
        this.pdfService = pdfService;
    }

    @PostMapping
    public ResponseEntity<String> guardarOrden(
            @RequestBody OrdenCompraDTO dto) {

        if (dto.getIdCentroCosto() == null) {
            return ResponseEntity.badRequest()
                    .body("Centro de costo requerido");
        }

        ordenCompraService.guardarOrdenDesdeDTO(dto);

        return ResponseEntity.ok("Orden guardada correctamente");
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> actualizarOrden(
            @PathVariable Integer id,
            @RequestBody OrdenCompraDTO dto) {

        if (dto.getIdCentroCosto() == null) {
            return ResponseEntity.badRequest()
                    .body("Centro de costo requerido");
        }

        ordenCompraService.actualizarOrdenDesdeDTO(id, dto);

        return ResponseEntity.ok("Orden actualizada correctamente");
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarOrden(
            @PathVariable Integer id) {

        try {

            ordenCompraService.aprobarOrden(id);

            return ResponseEntity.ok(
                    "Orden aprobada correctamente");

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @PutMapping("/{id}/recibir")
    public ResponseEntity<?> recibirOrden(
            @PathVariable Integer id,
            @RequestBody RecibirOrdenDTO dto) {

        try {

            ordenCompraService.recibirOrden(id, dto);

            return ResponseEntity.ok(
                    "Orden recibida correctamente");

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarOrden(
            @PathVariable Integer id) {

        try {

            ordenCompraService.eliminarOrden(id);

            return ResponseEntity.ok(
                    "Orden eliminada correctamente");

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Integer id) {
        try {
            OrdenCompra orden = ordenCompraService.obtenerPorId(id);
            if (orden == null) {
                return ResponseEntity.notFound().build();
            }

            if (orden.getEstado() != com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra.APROBADA
                    && orden.getEstado() != com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra.RECIBIDA) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            byte[] pdfBytes = pdfService.generarPdfOrdenCompra(orden);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "Orden_Compra_" + (orden.getNumeroOrden() != null ? orden.getNumeroOrden() : id) + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}