package com.palmera_junior.gestion_compras.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.dto.RecibirOrdenDTO;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.service.OrdenCompraService;

@RestController
@RequestMapping("/orden-compra")
public class OrdenCompraController {

    @Autowired
    private OrdenCompraService ordenCompraService;

    @PostMapping
    public ResponseEntity<OrdenCompra> guardarOrden(
            @RequestBody OrdenCompraDTO dto) {

        OrdenCompra ordenGuardada = ordenCompraService.guardarOrdenDesdeDTO(dto);

        return ResponseEntity.ok(ordenGuardada);
    }

    @PutMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarOrden(
            @PathVariable Integer id) {

        try {

            OrdenCompra orden = ordenCompraService.aprobarOrden(id);

            return ResponseEntity.ok(orden);

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
}