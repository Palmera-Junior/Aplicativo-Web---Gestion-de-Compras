package com.palmera_junior.gestion_compras.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.service.ProveedorService;


@RestController
@RequestMapping("/api/proveedores")
@CrossOrigin(origins ="*")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    @GetMapping("/sede/{idSede}")
    public ResponseEntity<List<Proveedor>> listarPorSede(@PathVariable Integer idSede) {
        List<Proveedor> proveedores = proveedorService.listarPorSede(idSede);

        if (proveedores.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(proveedores);
    }

       

   

        
    
}

