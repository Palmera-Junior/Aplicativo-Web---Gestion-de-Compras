package com.palmera_junior.gestion_compras.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import com.palmera_junior.gestion_compras.service.ProveedorService;

@Controller
public class OrdenDeCompraController {

    private final ProveedorService proveedorService;

    public OrdenDeCompraController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping("/orden_pago")
public String ordenCompra(Model model) {

    var proveedores = proveedorService.Listar();

    System.out.println("Cantidad: " + proveedores.size());

    model.addAttribute("proveedores", proveedores);

    return "orden_pago";
}
  
}