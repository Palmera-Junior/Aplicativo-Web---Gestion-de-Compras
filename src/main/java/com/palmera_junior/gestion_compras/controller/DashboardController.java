package com.palmera_junior.gestion_compras.controller;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.security.CustomOAuth2User;
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

        Object principal = authentication.getPrincipal();

        Usuario usuario;

        if (principal instanceof UsuarioPrincipal up) {

            usuario = up.getUsuario();

        } else if (principal instanceof CustomOAuth2User cu) {

            usuario = cu.getUsuario();

        } else {

            throw new RuntimeException(
                    "Tipo de usuario no soportado");
        }
        Integer idSede = usuario.getSede() != null
                ? usuario.getSede().getIdSede()
                : null;

        boolean esNacional = usuario.getSede() != null
                && "Sede Nacional".equalsIgnoreCase(
                        usuario.getSede().getNombre());

        if ((fechaDesde == null || fechaDesde.isBlank()) && (fechaHasta == null || fechaHasta.isBlank())) {
            LocalDate now = LocalDate.now();
            fechaDesde = now.withDayOfMonth(1).toString();
            fechaHasta = now.withDayOfMonth(now.lengthOfMonth()).toString();
        }

        Page<OrdenCompra> ordenesCompra = ordenCompraService.ordenesDeCompraPaginadas(PageRequest.of(page, size), q,
                fechaDesde, fechaHasta, idSede, esNacional);

        // Obtener centros de costo asociados a la sede del usuario autenticado
        List<CentroCosto> centroCostos;
        // Si la sede del usuario es la sede "Nacional", listar todos los centros de
        // costo
        if (usuario.getSede() != null
                && "Sede Nacional".equalsIgnoreCase(
                        usuario.getSede().getNombre())) {
            centroCostos = centroCostoService.getAllCentroCostos();
        } else {
            centroCostos = centroCostoService.listarPorSede(idSede);
        }

        model.addAttribute("centroCostos", centroCostos);
        model.addAttribute("productos", productoService.getAllProductos());
        model.addAttribute("paginaActual", page);
        model.addAttribute("ordenesCompra", ordenesCompra);
        model.addAttribute("proveedores",
                esNacional ? proveedorService.getAllProveedores() : proveedorService.listarPorSede(idSede));
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