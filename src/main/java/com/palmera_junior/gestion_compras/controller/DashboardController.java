package com.palmera_junior.gestion_compras.controller;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.security.UsuarioPrincipal;
import com.palmera_junior.gestion_compras.service.CentroCostoService;
import com.palmera_junior.gestion_compras.service.OrdenCompraService;
import com.palmera_junior.gestion_compras.service.ProductoService;
import com.palmera_junior.gestion_compras.service.ProveedorService;

import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private OrdenCompraService ordenCompraService;

    @Autowired
    private ProveedorService proveedorService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CentroCostoService centroCostoService;

    @GetMapping("/dashboard")
    public String listarOrdenesCompra(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String fechaDesde,
            @RequestParam(defaultValue = "") String fechaHasta,
            Model model,
            Authentication authentication) {

        UsuarioPrincipal usuarioPrincipal = (UsuarioPrincipal) authentication.getPrincipal();
        Integer idSede = usuarioPrincipal.getSedeId();

        boolean esNacional = usuarioPrincipal.getUsuario() != null
                && usuarioPrincipal.getUsuario().getSede() != null
                && "Sede Nacional".equalsIgnoreCase(usuarioPrincipal.getUsuario().getSede().getNombre());

        if ((fechaDesde == null || fechaDesde.isBlank()) && (fechaHasta == null || fechaHasta.isBlank())) {
            LocalDate now = LocalDate.now();
            fechaDesde = now.withDayOfMonth(1).toString();
            fechaHasta = now.withDayOfMonth(now.lengthOfMonth()).toString();
        }

        Page<OrdenCompra> ordenesCompra = ordenCompraService.ordenesDeCompraPaginadas(PageRequest.of(page, size), q, fechaDesde, fechaHasta, idSede, esNacional);

        // Obtener centros de costo asociados a la sede del usuario autenticado
        List<CentroCosto> centroCostos;
        // Si la sede del usuario es la sede "Nacional", listar todos los centros de costo
        if (usuarioPrincipal.getUsuario() != null
                && usuarioPrincipal.getUsuario().getSede() != null
                && "Sede Nacional".equalsIgnoreCase(usuarioPrincipal.getUsuario().getSede().getNombre())) {
            centroCostos = centroCostoService.getAllCentroCostos();
        } else {
            centroCostos = centroCostoService.listarPorSede(idSede);
        }

        model.addAttribute("centroCostos", centroCostos);
        model.addAttribute("productos", productoService.getAllProductos());
        model.addAttribute("paginaActual", page);
        model.addAttribute("ordenesCompra", ordenesCompra);
        model.addAttribute("proveedores", proveedorService.getAllProveedores());
        model.addAttribute("q", q);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        return "dashboard";
    }

   
@GetMapping("/dashboard/producto")
@ResponseBody
public Producto buscarProducto(@RequestParam(required = false) String codigo) {
    if (codigo == null || codigo.isBlank()) {
        return null;
    }
    return productoService.buscarPorCodigo(codigo);
}

@GetMapping("/orden-compra/{id}")
@ResponseBody
public OrdenCompraDTO obtenerOrden(
        @PathVariable Integer id) {

    return ordenCompraService.obtenerOrdenDTO(id);
}


}