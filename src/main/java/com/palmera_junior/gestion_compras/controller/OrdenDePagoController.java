package com.palmera_junior.gestion_compras.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;



@Controller
public class OrdenDePagoController {

     @GetMapping("/orden_pago")
     public String OrdenPago() {
         return "orden_pago";
     }
     
    
}
