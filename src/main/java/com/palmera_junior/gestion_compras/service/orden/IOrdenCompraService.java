package com.palmera_junior.gestion_compras.service.orden;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.dto.RecibirOrdenDTO;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;

/**
 * Contrato de servicio para la lógica de negocio y ciclo de vida de las Órdenes de Compra.
 */
public interface IOrdenCompraService {

    /**
     * Qué hace:
     * Consulta de forma paginada y filtrada las órdenes de compra según texto de búsqueda, rango de fechas, sede, estado y diferencias.
     * 
     * A dónde apunta:
     * - Repositorio: {@link com.palmera_junior.gestion_compras.repository.OrdenCompraRepository#findOrdenesPaginadasConDiferencias} o similar
     * 
     * @param pageable Parámetros de paginación y ordenamiento.
     * @param search Término de búsqueda libre.
     * @param fechaDesde Fecha inicial (YYYY-MM-DD).
     * @param fechaHasta Fecha final (YYYY-MM-DD).
     * @param idSede ID de la sede a filtrar (null si nacional).
     * @param esNacional Indica si el usuario tiene visibilidad global.
     * @param estado Estado de orden a filtrar.
     * @param soloModificadas Filtro para órdenes con discrepancias de recepción.
     * @return Página de entidades {@link OrdenCompra}.
     */
    Page<OrdenCompra> ordenesDeCompraPaginadas(Pageable pageable, String search, String fechaDesde,
            String fechaHasta, Integer idSede, boolean esNacional, String estado, boolean soloModificadas);

    /**
     * Qué hace:
     * Retorna todas las órdenes de compra sin paginación (utilizado en paneles de administración).
     * 
     * A dónde apunta:
     * - Consulta tabla: orden_compra
     * 
     * @return Lista completa de órdenes.
     */
    List<OrdenCompra> listarOrdenesCompra();

    /**
     * Qué hace:
     * Busca una orden de compra por su ID con sus relaciones cargadas.
     * 
     * A dónde apunta:
     * - Consulta tabla: orden_compra
     * 
     * @param idOrden Identificador numérico de la orden.
     * @return Entidad {@link OrdenCompra} o null.
     */
    OrdenCompra obtenerPorId(Integer idOrden);

    /**
     * Qué hace:
     * Crea y persiste una nueva orden de compra en estado BORRADOR a partir de su DTO.
     * 
     * A dónde apunta:
     * - Modifica tablas: orden_compra, detalle_compra
     * 
     * @param dto Datos transferidos de la orden.
     * @return Orden guardada.
     */
    OrdenCompra guardarOrdenDesdeDTO(OrdenCompraDTO dto);

    /**
     * Qué hace:
     * Modifica una orden existente en estado BORRADOR con nuevos totales y líneas de producto.
     * 
     * A dónde apunta:
     * - Modifica tablas: orden_compra, detalle_compra
     * 
     * @param idOrden ID de la orden.
     * @param dto Nuevos datos.
     * @return Orden actualizada.
     */
    OrdenCompra actualizarOrdenDesdeDTO(Integer idOrden, OrdenCompraDTO dto);

    /**
     * Qué hace:
     * Aprueba la orden de compra, genera el consecutivo oficial y publica el evento de aprobación para envío de correo y PDF.
     * 
     * A dónde apunta:
     * - Modifica tabla: orden_compra
     * - Publica evento: {@link com.palmera_junior.gestion_compras.events.OrdenCompraAprobadaEvent}
     * 
     * @param idOrden ID de la orden.
     * @return Orden aprobada.
     */
    OrdenCompra aprobarOrden(Integer idOrden);

    /**
     * Qué hace:
     * Registra la recepción de mercancía física, observaciones, flete, soporte digital y cantidades recibidas por ítem.
     * 
     * A dónde apunta:
     * - Modifica tablas: orden_compra, detalle_compra
     * 
     * @param idOrden ID de la orden.
     * @param dto Datos de recepción.
     * @return Orden recibida.
     */
    OrdenCompra recibirOrden(Integer idOrden, RecibirOrdenDTO dto);

    /**
     * Qué hace:
     * Asocia la factura comercial y soporte fotográfico de facturación a la orden.
     * 
     * A dónde apunta:
     * - Modifica tabla: orden_compra
     * 
     * @param idOrden ID de la orden.
     * @param numeroFactura Número fiscal de la factura.
     * @param fotoFactura Imagen/PDF en base64.
     * @return Orden facturada o completada.
     */
    OrdenCompra facturarOrden(Integer idOrden, String numeroFactura, String fotoFactura);

    /**
     * Qué hace:
     * Realiza la anulación de una orden de compra registrando el cambio de estado a ANULADA.
     * 
     * A dónde apunta:
     * - Modifica tabla: orden_compra
     * 
     * @param idOrden ID de la orden.
     * @return Orden anulada.
     */
    OrdenCompra anularOrden(Integer idOrden);

    /**
     * Qué hace:
     * Construye un {@link OrdenCompraDTO} con todas las líneas y datos enriquecidos para consumo frontend.
     * 
     * A dónde apunta:
     * - Consulta tablas: orden_compra, detalle_compra, producto, presentacion_producto
     * 
     * @param id ID de la orden.
     * @return DTO enriquecido.
     */
    OrdenCompraDTO obtenerOrdenDTO(Integer id);

    /**
     * Qué hace:
     * Indica si una orden tiene diferencias persistidas entre cantidad solicitada y cantidad recibida en cualquiera de sus líneas.
     * 
     * A dónde apunta:
     * - Consulta tabla: detalle_compra
     * 
     * @param idOrden ID de la orden.
     * @return true si existen diferencias en recepción.
     */
    boolean tieneDiferenciasRecepcion(Integer idOrden);

}

