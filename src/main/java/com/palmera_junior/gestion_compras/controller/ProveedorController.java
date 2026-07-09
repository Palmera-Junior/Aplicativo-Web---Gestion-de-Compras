package com.palmera_junior.gestion_compras.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.palmera_junior.gestion_compras.service.ProveedorService;

import org.springframework.ui.Model;

@Controller
@RequestMapping("/proveedores")
public class ProveedorController {

    private final ProveedorService service;

    public ProveedorController(
                ProveedorService service){
                    this.service = service;
                }

    @GetMapping
    public String listar(Model model) {

        model.addAttribute(
            "proveedores",
            service.Listar()
        );

        return "proveedores";
    }
}

