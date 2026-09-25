package com.palmera_junior.gestion_compras.controller.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.palmera_junior.gestion_compras.entity.Categoria;
import com.palmera_junior.gestion_compras.dto.ResumenOrdenesPorSedeDTO;
import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra;
import com.palmera_junior.gestion_compras.entity.EstadoPoliza;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.service.catalogo.IProductoService;
import com.palmera_junior.gestion_compras.service.catalogo.IProveedorService;
import com.palmera_junior.gestion_compras.service.orden.IOrdenCompraService;
import com.palmera_junior.gestion_compras.service.organizacion.ICentroCostoService;
import com.palmera_junior.gestion_compras.service.organizacion.ISedeService;
import com.palmera_junior.gestion_compras.service.poliza.IPolizaService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Controlador principal del Panel de Administración del Sistema (/admin).
 * Compone y carga en un único modelo de vista los cinco agregados administrativos
 * (Proveedores, Usuarios, Productos con sus presentaciones, Sedes y Centros de Costo),
 * incluyendo paginación independiente y filtros dinámicos.
 */
@Controller
public class AdminDashboardController {

    private static final List<String> COLORES_SEDES = List.of(
            "#5b56d9", "#1d9bd1", "#19aa9b", "#f57c1f", "#f2bb16",
            "#e95378", "#9168e9", "#48b87d", "#dd63b5", "#756ee8");

    private final IProveedorService proveedorService;
    private final IUsuarioService usuarioService;
    private final IProductoService productoService;
    private final ISedeService sedeService;
    private final ICentroCostoService centroCostoService;
    private final IOrdenCompraService ordenCompraService;
    private final IPolizaService polizaService;

    /**
     * Constructor para inyección de dependencias de todos los servicios de administración.
     */
    public AdminDashboardController(IProveedorService proveedorService, IUsuarioService usuarioService,
            IProductoService productoService, ISedeService sedeService, ICentroCostoService centroCostoService,
            IOrdenCompraService ordenCompraService, IPolizaService polizaService) {
        this.proveedorService = proveedorService;
        this.usuarioService = usuarioService;
        this.productoService = productoService;
        this.sedeService = sedeService;
        this.centroCostoService = centroCostoService;
        this.ordenCompraService = ordenCompraService;
        this.polizaService = polizaService;
    }

    /**
     * Qué hace:
     * Consulta y pagina todos los catálogos y entidades del panel de administración (Proveedores,
     * Usuarios, Productos, Sedes, Centros de Costo, métricas de órdenes incompletas), aplicando filtros
     * de búsqueda reactivos por texto, rol, sede y categoría.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /admin
     * - Seguridad: `@PreAuthorize("hasRole('ADMINISTRADOR')")`
     * - Servicios consultados:
     *   - {@link IProveedorService#listarTodos()}
     *   - {@link IUsuarioService#listarTodos()}
     *   - {@link IProductoService#getAllProductos()}
     *   - {@link ISedeService#listarTodos()} / {@link ISedeService#paginar(int, int)}
     *   - {@link ICentroCostoService#getAllCentroCostos()} / {@link ICentroCostoService#paginar(int, int)}
     *   - {@link IOrdenCompraService#listarOrdenesCompra()}
     * - Vista Thymeleaf: templates/admin.html
     * 
     * @param model Objeto Model de Spring MVC para inyectar los catálogos y páginas.
     * @param pageProveedores Número de página para la tabla de proveedores.
     * @param pageUsuarios Número de página para la tabla de usuarios.
     * @param pageProductos Número de página para la tabla de productos.
     * @param pageSedes Número de página para la tabla de sedes.
     * @param pageCentros Número de página para la tabla de centros de costo.
     * @param size Cantidad de registros por página.
     * @param searchProveedores Filtro de texto para proveedores (nombre o NIT).
     * @param searchUsuarios Filtro de texto para usuarios (nombre, cédula, usuario).
     * @param rolUsuario Filtro por rol de usuario.
     * @param sedeIdUsuario Filtro por sede del usuario.
     * @param searchProductos Filtro de texto para productos (nombre o código).
     * @param categoriaProducto Filtro por categoría de producto.
     * @param success Mensaje de éxito temporal para toasts.
     * @param error Mensaje de error temporal para toasts.
     * @return Nombre de la plantilla ("admin").
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('SUPERADMINISTRADOR', 'ADMINISTRADOR')")
    public String mostrarPanelAdministracion(Model model,

            @RequestParam(defaultValue = "0") int pageProveedores,
            @RequestParam(defaultValue = "0") int pageUsuarios,
            @RequestParam(defaultValue = "0") int pageProductos,
            @RequestParam(defaultValue = "0") int pageSedes,
            @RequestParam(defaultValue = "0") int pageCentros,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String searchProveedores,
            @RequestParam(required = false) String searchUsuarios,
            @RequestParam(required = false) String rolUsuario,
            @RequestParam(required = false) Integer sedeIdUsuario,
            @RequestParam(required = false) String searchProductos,
            @RequestParam(required = false) String categoriaProducto,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) Integer sedeOrdenes,
            @RequestParam(defaultValue = "false") boolean mostrarFiltroFecha,
            @RequestParam(required = false) String success,
            @RequestParam(required = false) String error) {

        List<Proveedor> proveedores = proveedorService.listarTodos();
        boolean periodoMesActual = (fechaDesde == null || fechaDesde.isBlank())
            && (fechaHasta == null || fechaHasta.isBlank());
        if (periodoMesActual) {
            LocalDate ahora = LocalDate.now();
            fechaDesde = ahora.withDayOfMonth(1).toString();
            fechaHasta = ahora.withDayOfMonth(ahora.lengthOfMonth()).toString();
        }
        LocalDate inicioMetricas = parsearFecha(fechaDesde, "La fecha inicial no es válida");
        LocalDate finMetricas = parsearFecha(fechaHasta, "La fecha final no es válida");
        if (inicioMetricas != null && finMetricas != null && inicioMetricas.isAfter(finMetricas)) {
            throw new IllegalArgumentException("La fecha inicial no puede ser posterior a la fecha final");
        }
        List<OrdenCompra> ordenesCompra = ordenCompraService.listarOrdenesCompra().stream()
                .filter(orden -> estaEnRango(orden, inicioMetricas, finMetricas))
                .toList();
        List<OrdenCompra> ordenesCompraValidas = ordenesCompra.stream()
            .filter(this::esOrdenValidaParaTotales)
            .toList();
        List<OrdenCompra> ordenesCompraTarjeta = ordenesCompra.stream()
            .filter(orden -> sedeOrdenes == null || (obtenerSedeDestino(orden) != null
                && sedeOrdenes.equals(obtenerSedeDestino(orden).getIdSede())))
            .toList();
        List<Poliza> todasLasPolizas = polizaService.listarPolizas();
        List<Poliza> polizas = todasLasPolizas.stream()
            .filter(poliza -> estaEnRango(poliza, inicioMetricas, finMetricas))
            .toList();
        long ordenesEnProceso = ordenesCompra.stream()
            .filter(orden -> orden != null && orden.getEstado() != null)
            .filter(orden -> !orden.getEstado().equalsIgnoreCase("COMPLETADA")
                && !orden.getEstado().equalsIgnoreCase("ANULADA"))
            .count();
        long ordenesConDiferenciasRecepcion = ordenesCompra.stream()
            .filter(this::esOrdenValidaParaTotales)
            .filter(orden -> orden.getIdOrden() != null)
            .filter(orden -> ordenCompraService.tieneDiferenciasRecepcion(orden.getIdOrden()))
            .count();
        OptionalDouble promedioDiasCalculado = ordenesCompra.stream()
            .filter(this::esOrdenValidaParaTotales)
            .filter(orden -> orden.getFecha() != null && orden.getFechaAprobacion() != null)
            .filter(orden -> !orden.getFechaAprobacion().isBefore(orden.getFecha()))
            .mapToLong(orden -> ChronoUnit.DAYS.between(orden.getFecha(), orden.getFechaAprobacion()))
            .average();
        Double promedioDiasAprobacion = promedioDiasCalculado.isPresent()
                ? promedioDiasCalculado.getAsDouble()
                : null;
        String tiempoPromedioAprobacion = formatearTiempoPromedioAprobacion(promedioDiasAprobacion);
        long polizasProximasAVencer = todasLasPolizas.stream()
            .filter(poliza -> poliza != null && poliza.getEstado() != EstadoPoliza.ANULADA)
            .filter(Poliza::estaProximaAVencer)
            .count();
        long totalPolizas = polizas.stream()
            .filter(poliza -> poliza != null && poliza.getEstado() != EstadoPoliza.ANULADA)
            .count();
        BigDecimal valorTotalOrdenes = ordenesCompraValidas.stream()
            .map(OrdenCompra::getTotal)
            .filter(total -> total != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<ResumenOrdenesPorSedeDTO> ordenesPorSede = resumirOrdenesPorSede(ordenesCompraValidas);
        BigDecimal valorTotalOrdenesPorSede = ordenesPorSede.stream()
                .map(ResumenOrdenesPorSedeDTO::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long ordenesIncompletas = ordenesCompra.stream()
                .filter(orden -> orden != null && orden.getIdOrden() != null)
                .filter(orden -> ordenCompraService.tieneDiferenciasRecepcion(orden.getIdOrden()))
                .filter(orden -> orden.getEstado() != null &&
                        !orden.getEstado().equalsIgnoreCase("BORRADOR") &&
                        !orden.getEstado().equalsIgnoreCase("FACTURADA") &&
                        !orden.getEstado().equalsIgnoreCase("ANULADA") &&
                        !orden.getEstado().equalsIgnoreCase("APROBADA")
                    )
                .count();
        List<Usuario> usuarios = usuarioService.listarTodos();
        List<Producto> productos = productoService.getAllProductos();
        List<Sede> sedes = sedeService.listarTodos();
        List<CentroCosto> centroCostos = centroCostoService.getAllCentroCostos();

        // Aplicar filtros en memoria (suficiente para datasets pequeños y evita cambios
        // en repositorios)
        List<Proveedor> proveedoresFiltrados = proveedores.stream().filter(p -> {
            if (searchProveedores == null || searchProveedores.trim().isEmpty())
                return true;
            String s = searchProveedores.trim().toLowerCase();
            return (p.getNombre() != null && p.getNombre().toLowerCase().contains(s))
                    || (p.getNit() != null && p.getNit().toLowerCase().contains(s));
        }).collect(Collectors.toList());

        List<Usuario> usuariosFiltrados = usuarios.stream().filter(u -> {
            boolean ok = true;
            if (searchUsuarios != null && !searchUsuarios.trim().isEmpty()) {
                String s = searchUsuarios.trim().toLowerCase();
                String nombreCompleto = (u.getNombre() != null ? u.getNombre() : "") + " "
                        + (u.getApellido() != null ? u.getApellido() : "");
                ok = (nombreCompleto.toLowerCase().contains(s))
                        || (u.getCedula() != null && u.getCedula().toLowerCase().contains(s))
                        || (u.getNombreUsuario() != null && u.getNombreUsuario().toLowerCase().contains(s));
            }
            if (ok && rolUsuario != null && !rolUsuario.trim().isEmpty()) {
                ok = u.getRol() != null && u.getRol().toString().equalsIgnoreCase(rolUsuario);
            }
            if (ok && sedeIdUsuario != null) {
                ok = u.getSede() != null && u.getSede().getIdSede().equals(sedeIdUsuario);
            }
            return ok;
        }).collect(Collectors.toList());

        List<Producto> productosFiltrados = productos.stream().filter(p -> {
            boolean ok = true;
            if (searchProductos != null && !searchProductos.trim().isEmpty()) {
                String s = searchProductos.trim().toLowerCase();
                ok = (p.getNombre() != null && p.getNombre().toLowerCase().contains(s))
                        || (p.getCodigoInventario() != null && p.getCodigoInventario().toLowerCase().contains(s));
            }
            if (ok && categoriaProducto != null && !categoriaProducto.trim().isEmpty()) {
                ok = p.getCategoria() != null && p.getCategoria().toString().equalsIgnoreCase(categoriaProducto);
            }
            return ok;
        }).collect(Collectors.toList());

        // Crear pages manualmente para respetar paginación
        Page<Proveedor> proveedoresPage;
        {
            int total = proveedoresFiltrados.size();
            int start = Math.min(pageProveedores * size, total);
            int end = Math.min(start + size, total);
            List<Proveedor> content = (start < end) ? proveedoresFiltrados.subList(start, end)
                    : java.util.Collections.emptyList();
            proveedoresPage = new PageImpl<>(content, PageRequest.of(pageProveedores, size), total);
        }

        Page<Usuario> usuariosPage;
        {
            int total = usuariosFiltrados.size();
            int start = Math.min(pageUsuarios * size, total);
            int end = Math.min(start + size, total);
            List<Usuario> content = (start < end) ? usuariosFiltrados.subList(start, end)
                    : java.util.Collections.emptyList();
            usuariosPage = new PageImpl<>(content, PageRequest.of(pageUsuarios, size), total);
        }

        Page<Producto> productosPage;
        {
            int total = productosFiltrados.size();
            int start = Math.min(pageProductos * size, total);
            int end = Math.min(start + size, total);
            List<Producto> content = (start < end) ? productosFiltrados.subList(start, end)
                    : java.util.Collections.emptyList();
            productosPage = new PageImpl<>(content, PageRequest.of(pageProductos, size), total);
        }

        Page<Sede> sedesPage = sedeService.paginar(pageSedes, size);
        Page<CentroCosto> centrosCostoPage = centroCostoService.paginar(pageCentros, size);

        model.addAttribute("proveedoresPage", proveedoresPage);
        model.addAttribute("usuariosPage", usuariosPage);
        model.addAttribute("productosPage", productosPage);
        model.addAttribute("sedesPage", sedesPage);
        model.addAttribute("centrosCostoPage", centrosCostoPage);

        model.addAttribute("proveedores", proveedores);
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("ordenesCompra", ordenesCompra);
        model.addAttribute("ordenesCompraTarjeta", ordenesCompraTarjeta);
        model.addAttribute("sedeOrdenesSeleccionada", sedeOrdenes);
        model.addAttribute("totalOrdenes", ordenesCompraValidas.size());
        model.addAttribute("ordenesEnProceso", ordenesEnProceso);
        model.addAttribute("ordenesConDiferenciasRecepcion", ordenesConDiferenciasRecepcion);
        model.addAttribute("tiempoPromedioAprobacion", tiempoPromedioAprobacion);
        model.addAttribute("totalPolizas", totalPolizas);
        model.addAttribute("polizasProximasAVencer", polizasProximasAVencer);
        model.addAttribute("valorTotalOrdenes", valorTotalOrdenes);
        model.addAttribute("valorTotalOrdenesPorSede", valorTotalOrdenesPorSede);
        model.addAttribute("ordenesPorSede", ordenesPorSede);
        model.addAttribute("ordenesIncompletas", ordenesIncompletas);
        model.addAttribute("productos", productos);
        model.addAttribute("sedes", sedes);
        model.addAttribute("centrosCosto", centroCostos);
        model.addAttribute("pageProveedores", pageProveedores);
        model.addAttribute("pageUsuarios", pageUsuarios);
        model.addAttribute("pageProductos", pageProductos);
        model.addAttribute("pageSedes", pageSedes);
        model.addAttribute("pageCentros", pageCentros);
        model.addAttribute("size", size);
        model.addAttribute("roles", Arrays.asList(Rol.values()));
        model.addAttribute("categorias", Arrays.asList(Categoria.values()));
        model.addAttribute("categoriasUnicas", Arrays.asList(Categoria.values()));

        // Mantener valores de filtros en la vista
        model.addAttribute("searchProveedores", searchProveedores);
        model.addAttribute("searchUsuarios", searchUsuarios);
        model.addAttribute("rolUsuario", rolUsuario);
        model.addAttribute("sedeIdUsuario", sedeIdUsuario);
        model.addAttribute("searchProductos", searchProductos);
        model.addAttribute("categoriaProducto", categoriaProducto);
        model.addAttribute("fechaDesdeMetricas", fechaDesde);
        model.addAttribute("fechaHastaMetricas", fechaHasta);
        model.addAttribute("periodoMesActual", periodoMesActual);
        model.addAttribute("mostrarFiltroFecha", mostrarFiltroFecha);

        model.addAttribute("successMessage", success);
        model.addAttribute("errorMessage", error);

        return "admin";
    }

    private List<ResumenOrdenesPorSedeDTO> resumirOrdenesPorSede(List<OrdenCompra> ordenes) {
        Map<String, BigDecimal> totales = ordenes.stream()
                .filter(this::esOrdenValidaParaTotales)
                .filter(orden -> orden.getTotal() != null)
                .filter(orden -> obtenerSedeDestino(orden) != null)
                .collect(Collectors.groupingBy(orden -> obtenerSedeDestino(orden).getNombre(), LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, OrdenCompra::getTotal, BigDecimal::add)));

        BigDecimal totalConsolidado = totales.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map.Entry<String, BigDecimal>> entradas = totales.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .toList();
        List<ResumenOrdenesPorSedeDTO> resumenes = new java.util.ArrayList<>();
        BigDecimal valorMaximoSede = entradas.isEmpty() ? BigDecimal.ZERO : entradas.get(0).getValue();
        double inicio = 0;
        for (int indice = 0; indice < entradas.size(); indice++) {
            Map.Entry<String, BigDecimal> entrada = entradas.get(indice);
            double porcentaje = totalConsolidado.signum() == 0 ? 0
                    : entrada.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(totalConsolidado, 4, java.math.RoundingMode.HALF_UP).doubleValue();
            double porcentajeBarra = valorMaximoSede.signum() == 0 ? 0
                : entrada.getValue().multiply(BigDecimal.valueOf(100))
                    .divide(valorMaximoSede, 4, java.math.RoundingMode.HALF_UP).doubleValue();
            double longitud = Math.max(0, porcentaje - 1.2);
            resumenes.add(new ResumenOrdenesPorSedeDTO(
                entrada.getKey(), entrada.getValue(), porcentaje, porcentajeBarra, inicio, longitud,
                    COLORES_SEDES.get(indice % COLORES_SEDES.size())));
            inicio += porcentaje;
        }
        return resumenes;
    }

    private boolean esOrdenValidaParaTotales(OrdenCompra orden) {
        return orden != null && orden.getEstado() != EstadoOrdenCompra.ANULADA;
    }

    private String formatearTiempoPromedioAprobacion(Double promedioDias) {
        if (promedioDias == null) {
            return "Sin datos";
        }
        if (promedioDias < 1) {
            return "Menos de 1 día";
        }
        return Math.round(promedioDias) + (Math.round(promedioDias) == 1 ? " día" : " días");
    }

    /**
     * Clasifica la orden por la sede propietaria del centro de costo. Esto permite que
     * una orden creada desde Sede Nacional se acumule en la sede del centro atendido.
     */
    private Sede obtenerSedeDestino(OrdenCompra orden) {
        if (orden.getCentroCosto() != null && orden.getCentroCosto().getSede() != null) {
            return orden.getCentroCosto().getSede();
        }
        return orden.getSede();
    }

    private boolean estaEnRango(OrdenCompra orden, LocalDate inicio, LocalDate fin) {
        if (orden == null || orden.getFecha() == null) {
            return false;
        }
        return (inicio == null || !orden.getFecha().isBefore(inicio))
                && (fin == null || !orden.getFecha().isAfter(fin));
    }

    private boolean estaEnRango(Poliza poliza, LocalDate inicio, LocalDate fin) {
        if (poliza == null || poliza.getFechaVencimiento() == null) {
            return false;
        }
        return (inicio == null || !poliza.getFechaVencimiento().isBefore(inicio))
                && (fin == null || !poliza.getFechaVencimiento().isAfter(fin));
    }

    private LocalDate parsearFecha(String valor, String mensajeError) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(mensajeError);
        }
    }
}
