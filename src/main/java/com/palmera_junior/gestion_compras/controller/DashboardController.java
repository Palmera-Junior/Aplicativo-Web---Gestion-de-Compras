package com.palmera_junior.gestion_compras.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.service.DashboardService;
import com.palmera_junior.gestion_compras.service.OrdenCompraService;
import com.palmera_junior.gestion_compras.service.ProductoService;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final OrdenCompraService ordenCompraService;
    private final ProductoService productoService;

    public DashboardController(DashboardService dashboardService, OrdenCompraService ordenCompraService,
            ProductoService productoService) {
        this.dashboardService = dashboardService;
        this.ordenCompraService = ordenCompraService;
        this.productoService = productoService;
    }

    @GetMapping("/dashboard")
    public String listarOrdenesCompra(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String fechaDesde,
            @RequestParam(defaultValue = "") String fechaHasta,
            @RequestParam(required = false) String estado,
            Model model,
            Authentication authentication) {
        model.addAttribute("estados", EstadoOrdenCompra.values());
        model.addAttribute("estadoSeleccionado", estado);
        return dashboardService.prepararModeloDashboard(page, size, q, fechaDesde, fechaHasta, estado, model, authentication);
    }

    @GetMapping("/dashboard/producto")
    @ResponseBody
    public Producto buscarProducto(@RequestParam(required = false) String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return null;
        }
        return productoService.buscarPorCodigo(codigo);
    }

    @GetMapping("/dashboard/productos/buscar")
    @ResponseBody
    public java.util.List<Producto> buscarProductosPorTermino(
            @RequestParam(required = false) String query) {
        return productoService.buscarPorTermino(query);
    }

    @GetMapping("/orden-compra/{id}")
    @ResponseBody
    public OrdenCompraDTO obtenerOrden(
            @PathVariable Integer id) {

        return ordenCompraService.obtenerOrdenDTO(id);
    }

}