package com.palmera_junior.gestion_compras.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Vistas iniciales del mÃ³dulo de Recursos Humanos. */
@Controller
public class RecursosHumanosController {

    @GetMapping("/recursos-humanos/dashboard")
    public String dashboard() {
        return "recursos-humanos-dashboard";
    }

    @GetMapping("/recursos-humanos/principal")
    public String principal() {
        return "recursos-humanos-principal";
    }
}
