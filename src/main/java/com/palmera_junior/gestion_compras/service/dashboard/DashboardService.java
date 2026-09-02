package com.palmera_junior.gestion_compras.service.dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra;
import com.palmera_junior.gestion_compras.entity.EstadoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.TipoEnvioCorreo;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.catalogo.IProductoService;
import com.palmera_junior.gestion_compras.service.catalogo.IProveedorService;
import com.palmera_junior.gestion_compras.service.correo.CorreoOrdenOutboxService;
import com.palmera_junior.gestion_compras.service.orden.IOrdenCompraService;
import com.palmera_junior.gestion_compras.service.organizacion.ICentroCostoService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Implementación del servicio de agregación del Dashboard.
 * Coordina la recopilación de datos de múltiples módulos (órdenes, productos, proveedores, sedes, correos)
 * respetando el alcance de seguridad por sede.
 */
@Service
public class DashboardService implements IDashboardService {

    private final IOrdenCompraService ordenCompraService;
    private final IProveedorService proveedorService;
    private final IProductoService productoService;
    private final ICentroCostoService centroCostoService;
    private final IUsuarioService usuarioService;
    private final CorreoOrdenOutboxService correoOrdenOutboxService;

    /**
     * Constructor con inyección de servicios requeridos por el dashboard.
     */
    public DashboardService(IOrdenCompraService ordenCompraService, IProveedorService proveedorService,
            IProductoService productoService, ICentroCostoService centroCostoService, IUsuarioService usuarioService,
            CorreoOrdenOutboxService correoOrdenOutboxService) {
        this.ordenCompraService = ordenCompraService;
        this.proveedorService = proveedorService;
        this.productoService = productoService;
        this.centroCostoService = centroCostoService;
        this.usuarioService = usuarioService;
        this.correoOrdenOutboxService = correoOrdenOutboxService;
    }

    /**
     * Qué hace:
     * Carga todos los catálogos y órdenes de compra correspondientes a la sede del usuario (o visión global si es Sede Nacional),
     * aplica filtros por rango de fechas (default: mes actual), texto y estado, y asocia el estado del correo outbox.
     * 
     * A dónde apunta:
     * - Servicios: {@link IOrdenCompraService#ordenesDeCompraPaginadas}, {@link CorreoOrdenOutboxService#obtenerEstadosPorOrdenes}, {@link ICentroCostoService}, {@link IProveedorService}, {@link IProductoService}
     * - Retorno: Plantilla "dashboard".
     */
    @Override
    public String prepararModeloDashboard(int page, int size, String q, String fechaDesde, String fechaHasta,
            String estado, boolean soloModificadas, Model model, Authentication authentication) {
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
                PageRequest.of(page, size), q, fechaDesdeAplicada, fechaHastaAplicada, idSede, esNacional, estado,
                soloModificadas);

        List<Integer> idsOrdenes = ordenesCompra.getContent().stream()
                .map(OrdenCompra::getIdOrden)
                .toList();
        Map<Integer, EstadoEnvioCorreo> estadosCorreoAprobacion = correoOrdenOutboxService
                .obtenerEstadosPorOrdenes(idsOrdenes, TipoEnvioCorreo.APROBACION);
        Map<Integer, EstadoEnvioCorreo> estadosCorreoFacturacion = correoOrdenOutboxService
                .obtenerEstadosPorOrdenes(idsOrdenes, TipoEnvioCorreo.FACTURACION);

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
        model.addAttribute("estadosCorreoAprobacion", estadosCorreoAprobacion);
        model.addAttribute("estadosCorreoFacturacion", estadosCorreoFacturacion);
        model.addAttribute("proveedores",
                esNacional ? proveedorService.getAllProveedores() : proveedorService.listarPorSede(idSede));
        model.addAttribute("q", q);
        model.addAttribute("fechaDesde", fechaDesdeAplicada);
        model.addAttribute("fechaHasta", fechaHastaAplicada);
        model.addAttribute("soloModificadas", soloModificadas);
        return "dashboard";
    }
}

