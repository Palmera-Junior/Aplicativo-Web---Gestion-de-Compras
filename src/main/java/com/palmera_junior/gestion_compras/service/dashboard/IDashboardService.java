package com.palmera_junior.gestion_compras.service.dashboard;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;

/**
 * Contrato del servicio coordinador de la interfaz principal del Dashboard de Gestión de Compras.
 */
public interface IDashboardService {

    /**
     * Qué hace:
     * Resuelve los privilegios del usuario autenticado (sede local vs. Sede Nacional), calcula el rango de fechas por defecto,
     * ejecuta la consulta paginada de órdenes filtradas, obtiene el estado de envíos de correo outbox y alimenta el objeto {@link Model} de Spring MVC.
     * 
     * A dónde apunta:
     * - Servicios consultados: {@link com.palmera_junior.gestion_compras.service.usuario.IUsuarioService}, {@link com.palmera_junior.gestion_compras.service.orden.IOrdenCompraService}, {@link com.palmera_junior.gestion_compras.service.catalogo.IProveedorService}, {@link com.palmera_junior.gestion_compras.service.catalogo.IProductoService}, {@link com.palmera_junior.gestion_compras.service.organizacion.ICentroCostoService}, {@link com.palmera_junior.gestion_compras.service.correo.CorreoOrdenOutboxService}
     * - Retorno: Vista Thymeleaf "dashboard" (templates/dashboard.html).
     * 
     * @param page Índice de página actual.
     * @param size Registros por página.
     * @param q Texto libre de búsqueda (número de orden, proveedor, etc.).
     * @param fechaDesde Fecha de inicio para filtro temporal (YYYY-MM-DD).
     * @param fechaHasta Fecha final para filtro temporal (YYYY-MM-DD).
     * @param estado Filtro por estado de orden de compra.
     * @param soloModificadas Filtro booleano para órdenes con diferencias en recepción.
     * @param model Modelo para Thymeleaf.
     * @param authentication Información del usuario en sesión.
     * @return Nombre de la plantilla ("dashboard").
     */
    String prepararModeloDashboard(int page, int size, String q, String fechaDesde, String fechaHasta,
            String estado, boolean soloModificadas, Model model, Authentication authentication);
}

