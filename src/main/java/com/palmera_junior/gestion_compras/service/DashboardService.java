package com.palmera_junior.gestion_compras.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.Usuario;

@Service
public class DashboardService {

    private final OrdenCompraService ordenCompraService;
    private final ProveedorService proveedorService;
    private final ProductoService productoService;
    private final CentroCostoService centroCostoService;
    private final UsuarioService usuarioService;

    public DashboardService(OrdenCompraService ordenCompraService, ProveedorService proveedorService,
            ProductoService productoService, CentroCostoService centroCostoService, UsuarioService usuarioService) {
        this.ordenCompraService = ordenCompraService;
        this.proveedorService = proveedorService;
        this.productoService = productoService;
        this.centroCostoService = centroCostoService;
        this.usuarioService = usuarioService;
    }

    public String prepararModeloDashboard(int page, int size, String q, String fechaDesde, String fechaHasta,
            String estado, Model model, Authentication authentication) {
        Usuario usuario = usuarioService.obtenerUsuarioAutenticado(authentication);

        Integer idSede = usuario.getSede() != null ? usuario.getSede().getIdSede() : null;

        boolean esNacional = usuario.getSede() != null
                && "Sede Nacional".equalsIgnoreCase(usuario.getSede().getNombre());

        String fechaDesdeAplicada = fechaDesde;
        String fechaHastaAplicada = fechaHasta;

        if ((fechaDesdeAplicada == null || fechaDesdeAplicada.isBlank())
                && (fechaHastaAplicada == null || fechaHastaAplicada.isBlank())) {
            LocalDate now = LocalDate.now();
            fechaDesdeAplicada = now.withDayOfMonth(1).toString();
            fechaHastaAplicada = now.withDayOfMonth(now.lengthOfMonth()).toString();
        }

        Page<OrdenCompra> ordenesCompra = ordenCompraService.ordenesDeCompraPaginadas(
                PageRequest.of(page, size), q, fechaDesdeAplicada, fechaHastaAplicada, idSede, esNacional, estado);

        List<CentroCosto> centroCostos;
        if (usuario.getSede() != null && "Sede Nacional".equalsIgnoreCase(usuario.getSede().getNombre())) {
            centroCostos = centroCostoService.getAllCentroCostos();
        } else {
            centroCostos = centroCostoService.listarPorSede(idSede);
        }

        model.addAttribute("estados", EstadoOrdenCompra.values());
        model.addAttribute("estadoSeleccionado", estado);
        model.addAttribute("centroCostos", centroCostos);
        model.addAttribute("productos", productoService.getAllProductos());
        model.addAttribute("paginaActual", page);
        model.addAttribute("ordenesCompra", ordenesCompra);
        model.addAttribute("proveedores",
                esNacional ? proveedorService.getAllProveedores() : proveedorService.listarPorSede(idSede));
        model.addAttribute("q", q);
        model.addAttribute("fechaDesde", fechaDesdeAplicada);
        model.addAttribute("fechaHasta", fechaHastaAplicada);
        return "dashboard";
    }
}
