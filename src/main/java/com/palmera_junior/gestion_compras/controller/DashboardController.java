package com.palmera_junior.gestion_compras.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.service.catalogo.IProductoService;
import com.palmera_junior.gestion_compras.service.dashboard.IDashboardService;
import com.palmera_junior.gestion_compras.service.orden.IOrdenCompraService;

/**

 * Controlador principal para la vista y operaciones del Dashboard de Gestión de Compras.
 * Gestiona el renderizado de la interfaz del usuario autenticado y las consultas AJAX rápidas
 * de productos y órdenes de compra.
 */
@Controller
public class DashboardController {

    private final IDashboardService dashboardService;
    private final IOrdenCompraService ordenCompraService;
    private final IProductoService productoService;

    /**
     * Constructor para inyección de dependencias de servicios del dashboard, órdenes y productos.
     */
    public DashboardController(IDashboardService dashboardService, IOrdenCompraService ordenCompraService,
            IProductoService productoService) {
        this.dashboardService = dashboardService;
        this.ordenCompraService = ordenCompraService;
        this.productoService = productoService;
    }

    /**
     * Qué hace:
     * Consulta y pagina el listado de órdenes de compra según los filtros aplicados (búsqueda por texto,
     * rango de fechas, estado de la orden y discrepancias en recepción), restringiendo la visibilidad
     * según el rol y sede del usuario autenticado. Prepara los datos en el modelo de Thymeleaf.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /dashboard
     * - Servicio delegado: {@link IDashboardService#prepararModeloDashboard}
     * - Vista / Plantilla renderizada: templates/dashboard.html
     * 
     * @param page Número de página actual (0-indexed).
     * @param size Cantidad de elementos por página.
     * @param q Término de búsqueda general (número de orden, proveedor, etc.).
     * @param fechaDesde Fecha de inicio para filtro.
     * @param fechaHasta Fecha de fin para filtro.
     * @param estado Estado de la orden (BORRADOR, APROBADA, RECIBIDA, FACTURADA, etc.).
     * @param soloModificadas Filtro booleano para órdenes con discrepancias en la recepción.
     * @param model Objeto Model de Spring MVC para inyectar atributos a la vista.
     * @param authentication Información de autenticación y rol del usuario en sesión.
     * @return Nombre de la plantilla Thymeleaf a renderizar ("dashboard").
     */
    @GetMapping("/dashboard")
    public String listarOrdenesCompra(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String fechaDesde,
            @RequestParam(defaultValue = "") String fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false, defaultValue = "false") boolean soloModificadas,
            Model model,
            Authentication authentication) {
        model.addAttribute("estados", EstadoOrdenCompra.values());
        model.addAttribute("estadoSeleccionado", estado);
        return dashboardService.prepararModeloDashboard(page, size, q, fechaDesde, fechaHasta, estado, soloModificadas, model, authentication);
    }

    /**
     * Qué hace:
     * Busca un producto por su código exacto de inventario y retorna la entidad con sus presentaciones y precios.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /dashboard/producto?codigo=...
     * - Servicio delegado: {@link IProductoService#buscarPorCodigo(String)} -> ProductoRepository
     * - Respuesta: Objeto JSON con el Producto encontrado o null si no existe.
     * 
     * @param codigo Código de inventario del producto.
     * @return Producto encontrado o null si el parámetro es vacío o inexistente.
     */
    @GetMapping("/dashboard/producto")
    @ResponseBody
    public ResponseEntity<Producto> buscarProducto(@RequestParam(required = false) String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Producto producto = productoService.buscarPorCodigo(codigo);
        return producto == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(producto);
    }

    /**
     * Qué hace:
     * Realiza una búsqueda predictiva/autocompletado de productos por coincidencia parcial en nombre o código.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /dashboard/productos/buscar?query=...
     * - Servicio delegado: {@link IProductoService#buscarPorTermino(String)} -> ProductoRepository
     * - Respuesta: Lista JSON de productos que coinciden con el término.
     * 
     * @param query Texto o término de búsqueda ingresado por el usuario.
     * @return Lista de productos encontrados (JSON).
     */
    @GetMapping("/dashboard/productos/buscar")
    @ResponseBody
    public java.util.List<Producto> buscarProductosPorTermino(
            @RequestParam(required = false) String query) {
        return productoService.buscarPorTermino(query);
    }

    /**
     * Qué hace:
     * Obtiene la información completa de una orden de compra en formato DTO para precargarla
     * en el modal de edición o visualización en el cliente.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /orden-compra/{id}
     * - Servicio delegado: {@link IOrdenCompraService#obtenerOrdenDTO(Integer)}
     * - Respuesta: DTO JSON con cabecera y líneas de detalle de la orden.
     * 
     * @param id Identificador numérico de la orden de compra.
     * @return {@link OrdenCompraDTO} con los datos de la orden.
     */
    @GetMapping("/orden-compra/{id}")
    @ResponseBody
    public OrdenCompraDTO obtenerOrden(
            @PathVariable Integer id) {

        return ordenCompraService.obtenerOrdenDTO(id);
    }

}
