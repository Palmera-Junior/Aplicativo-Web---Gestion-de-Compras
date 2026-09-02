package com.palmera_junior.gestion_compras.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.dto.RecibirOrdenDTO;
import com.palmera_junior.gestion_compras.dto.MarcarCorreoEnviadoDTO;
import com.palmera_junior.gestion_compras.dto.FacturarOrdenDTO;
import com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.service.correo.CorreoOrdenOutboxService;
import com.palmera_junior.gestion_compras.service.orden.IOrdenCompraService;
import com.palmera_junior.gestion_compras.service.orden.IPdfService;

/**
 * Controlador REST para el ciclo de vida completo de las Órdenes de Compra.
 * Gestiona endpoints para creación, edición, aprobación, recepción de mercancía,
 * registro de facturas, anulación, descarga de PDF y auditoría de correos.
 */
@RestController
@RequestMapping("/orden-compra")
public class OrdenCompraController {

    private final IOrdenCompraService ordenCompraService;
    private final IPdfService pdfService;
    private final CorreoOrdenOutboxService correoOrdenOutboxService;

    /**
     * Constructor para inyección de dependencias de servicios de orden, generación de PDF y mensajería outbox.
     */
    public OrdenCompraController(IOrdenCompraService ordenCompraService, IPdfService pdfService,
            CorreoOrdenOutboxService correoOrdenOutboxService) {
        this.ordenCompraService = ordenCompraService;
        this.pdfService = pdfService;
        this.correoOrdenOutboxService = correoOrdenOutboxService;
    }

    /**
     * Qué hace:
     * Valida y persiste una nueva orden de compra en estado BORRADOR a partir de los datos
     * del formulario (cabecera, proveedor, centro de costo, líneas de productos y totales).
     * 
     * A dónde apunta:
     * - Ruta HTTP: POST /orden-compra
     * - Servicio delegado: {@link IOrdenCompraService#guardarOrdenDesdeDTO(OrdenCompraDTO)} -> OrdenCompraRepository, DetalleCompra
     * - Respuesta: Mensaje de éxito 200 OK o 400 Bad Request si faltan datos requeridos.
     * 
     * @param dto Datos transferidos desde el frontend con la información de la orden.
     * @return {@link ResponseEntity} indicando el resultado de la operación.
     */
    @PostMapping
    public ResponseEntity<String> guardarOrden(
            @RequestBody OrdenCompraDTO dto) {

        if (dto.getIdCentroCosto() == null) {
            return ResponseEntity.badRequest()
                    .body("Centro de costo requerido");
        }

        ordenCompraService.guardarOrdenDesdeDTO(dto);

        return ResponseEntity.ok("Orden guardada correctamente");
    }

    /**
     * Qué hace:
     * Actualiza una orden de compra existente que se encuentre en estado BORRADOR,
     * recalculando valores y reemplazando o actualizando sus líneas de producto.
     * 
     * A dónde apunta:
     * - Ruta HTTP: PUT /orden-compra/{id}
     * - Servicio delegado: {@link IOrdenCompraService#actualizarOrdenDesdeDTO(Integer, OrdenCompraDTO)} -> OrdenCompraRepository
     * 
     * @param id Identificador numérico de la orden a actualizar.
     * @param dto Datos actualizados de la orden.
     * @return {@link ResponseEntity} indicando el resultado de la actualización.
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> actualizarOrden(
            @PathVariable Integer id,
            @RequestBody OrdenCompraDTO dto) {

        if (dto.getIdCentroCosto() == null) {
            return ResponseEntity.badRequest()
                    .body("Centro de costo requerido");
        }

        ordenCompraService.actualizarOrdenDesdeDTO(id, dto);

        return ResponseEntity.ok("Orden actualizada correctamente");
    }

    /**
     * Qué hace:
     * Cambia el estado de una orden de compra de BORRADOR a APROBADA, asigna el usuario y fecha
     * de aprobación, genera su número consecutivo definitivo y dispara el evento asíncrono
     * para la generación de PDF y envío automático por correo al proveedor.
     * 
     * A dónde apunta:
     * - Ruta HTTP: PUT /orden-compra/{id}/aprobar
     * - Servicio delegado: {@link IOrdenCompraService#aprobarOrden(Integer)}
     * - Evento publicado: {@link com.palmera_junior.gestion_compras.events.OrdenCompraAprobadaEvent}
     * 
     * @param id Identificador de la orden a aprobar.
     * @return 200 OK con confirmación o 400 Bad Request en caso de error de negocio.
     */
    @PutMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobarOrden(
            @PathVariable Integer id) {

        try {

            ordenCompraService.aprobarOrden(id);

            return ResponseEntity.ok(
                    "Orden aprobada correctamente");

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    /**
     * Qué hace:
     * Retorna los detalles completos y líneas de producto asociadas a una orden de compra específica.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /orden-compra/{id}/detalles
     * - Servicio delegado: {@link IOrdenCompraService#obtenerPorId(Integer)} y {@link IOrdenCompraService#obtenerOrdenDTO(Integer)}
     * 
     * @param id Identificador de la orden de compra.
     * @return DTO con la estructura de la orden o 404 si no existe.
     */
    @GetMapping("/{id}/detalles")
    public ResponseEntity<?> obtenerDetallesOrden(@PathVariable Integer id) {
        try {
            OrdenCompra orden = ordenCompraService.obtenerPorId(id);
            if (orden == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ordenCompraService.obtenerOrdenDTO(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Qué hace:
     * Verifica si una orden de compra presenta discrepancias entre las cantidades solicitadas y las recibidas en la entrega.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /orden-compra/{id}/has-diferencias-recepcion
     * - Servicio delegado: {@link IOrdenCompraService#tieneDiferenciasRecepcion(Integer)}
     * 
     * @param id Identificador de la orden.
     * @return JSON con booleano {"hasDifferences": true/false}.
     */
    @GetMapping("/{id}/has-diferencias-recepcion")
    public ResponseEntity<?> tieneDiferenciasRecepcion(@PathVariable Integer id) {
        try {
            boolean tiene = ordenCompraService.tieneDiferenciasRecepcion(id);
            return ResponseEntity.ok(java.util.Map.of("hasDifferences", tiene));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Qué hace:
     * Comprueba en lote una lista de IDs de órdenes separadas por comas y devuelve cuáles de ellas tienen diferencias persistidas en recepción.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /orden-compra/has-diferencias?ids=1,2,3
     * - Servicio delegado: {@link IOrdenCompraService#tieneDiferenciasRecepcion(Integer)}
     * 
     * @param idsCsv Cadena con identificadores numéricos separados por comas.
     * @return JSON con lista de IDs que tienen discrepancias {"ids": [1, 5]}.
     */
    @GetMapping("/has-diferencias")
    public ResponseEntity<?> tieneDiferenciasMultiple(@RequestParam(name = "ids") String idsCsv) {
        try {
            if (idsCsv == null || idsCsv.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Parameter 'ids' is required");
            }
            String[] parts = idsCsv.split(",");
            java.util.List<Integer> ids = new java.util.ArrayList<>();
            for (String p : parts) {
                try {
                    ids.add(Integer.parseInt(p.trim()));
                } catch (NumberFormatException ex) {
                    // ignorar entradas no numéricas
                }
            }
            java.util.List<Integer> withDifferences = new java.util.ArrayList<>();
            for (Integer id : ids) {
                try {
                    if (ordenCompraService.tieneDiferenciasRecepcion(id)) {
                        withDifferences.add(id);
                    }
                } catch (Exception ex) {
                    // ignorar errores por orden individual
                }
            }
            return ResponseEntity.ok(java.util.Map.of("ids", withDifferences));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Qué hace:
     * Registra la recepción física de los productos de la orden de compra, actualizando cantidades recibidas por línea,
     * receptor, observaciones, valor de flete y evidencia fotográfica opcional. Cambia el estado a RECIBIDA.
     * 
     * A dónde apunta:
     * - Ruta HTTP: PUT /orden-compra/{id}/recibir
     * - Servicio delegado: {@link IOrdenCompraService#recibirOrden(Integer, RecibirOrdenDTO)}
     * 
     * @param id Identificador de la orden a recibir.
     * @param dto Datos de recepción (receptor, flete, foto/PDF base64, productos).
     * @return 200 OK con confirmación o 400 Bad Request en caso de error.
     */
    @PutMapping("/{id}/recibir")
    public ResponseEntity<?> recibirOrden(
            @PathVariable Integer id,
            @RequestBody RecibirOrdenDTO dto) {

        try {

            ordenCompraService.recibirOrden(id, dto);

            return ResponseEntity.ok(
                    "Orden recibida correctamente");

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    /**
     * Qué hace:
     * Registra el número de factura y la evidencia del soporte de facturación del proveedor.
     * Si la orden ya estaba RECIBIDA, transiciona a COMPLETADA; si estaba APROBADA, pasa a FACTURADA.
     * 
     * A dónde apunta:
     * - Ruta HTTP: PUT /orden-compra/{id}/facturar
     * - Servicio delegado: {@link IOrdenCompraService#facturarOrden(Integer, String, String)}
     * 
     * @param id Identificador de la orden.
     * @param dto DTO con el número de factura y soporte base64.
     * @return 200 OK con confirmación.
     */
    @PutMapping("/{id}/facturar")
    public ResponseEntity<?> facturarOrden(
            @PathVariable Integer id,
            @RequestBody FacturarOrdenDTO dto) {

        try {
            String fotoFactura = dto.getFotoFactura() != null && !dto.getFotoFactura().isBlank()
                    ? dto.getFotoFactura()
                    : dto.getFotoRecepcion();
            ordenCompraService.facturarOrden(id, dto.getNumeroFactura(), fotoFactura);
            return ResponseEntity.ok("Orden facturada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Qué hace:
     * Realiza la anulación lógica de una orden de compra, cambiando su estado a ANULADA
     * sin eliminarla físicamente de la base de datos para mantener integridad contable y auditoría.
     * 
     * A dónde apunta:
     * - Ruta HTTP: PUT /orden-compra/{id}/anular
     * - Servicio delegado: {@link IOrdenCompraService#anularOrden(Integer)}
     * 
     * @param id Identificador de la orden a anular.
     * @return 200 OK o 400 Bad Request.
     */
    @PutMapping("/{id}/anular")
    public ResponseEntity<?> anularOrden(
            @PathVariable Integer id) {
        try {
            ordenCompraService.anularOrden(id);
            return ResponseEntity.ok("Orden anulada correctamente");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Qué hace:
     * Marca manualmente un envío de correo fallido como completado/enviado, registrando la justificación
     * del administrador en la auditoría outbox del sistema.
     * 
     * A dónde apunta:
     * - Ruta HTTP: PUT /orden-compra/{id}/correo/marcar-enviado
     * - Servicio delegado: {@link CorreoOrdenOutboxService#marcarEnviadoManualmente(Integer, com.palmera_junior.gestion_compras.entity.TipoEnvioCorreo, String)} -> AuditoriaEnvioCorreoRepository
     * 
     * @param id Identificador de la orden.
     * @param dto DTO con el tipo de envío (APROBACION/FACTURACION) y la descripción o motivo del marcado manual.
     * @return 200 OK con confirmación.
     */
    @PutMapping("/{id}/correo/marcar-enviado")
    public ResponseEntity<?> marcarCorreoEnviadoManualmente(
            @PathVariable Integer id,
            @RequestBody MarcarCorreoEnviadoDTO dto) {

        try {

            if (dto.getTipoEnvio() == null) {
                return ResponseEntity.badRequest().body("Debe indicar el tipo de correo a marcar (APROBACION o FACTURACION)");
            }

            correoOrdenOutboxService.marcarEnviadoManualmente(id, dto.getTipoEnvio(), dto.getDescripcion());

            return ResponseEntity.ok(
                    "Correo marcado como enviado correctamente");

        } catch (IllegalArgumentException | IllegalStateException e) {

            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    /**
     * Qué hace:
     * Genera dinámicamente y transmite el archivo PDF binario de la orden de compra aprobada para descarga del cliente.
     * 
     * A dónde apunta:
     * - Ruta HTTP: GET /orden-compra/{id}/pdf
     * - Servicio delegado: {@link IPdfService#generarPdfOrdenCompra(OrdenCompra)}
     * - Retorno: Stream binario `application/pdf` con header `Content-Disposition: attachment; filename=...`.
     * 
     * @param id Identificador de la orden de compra.
     * @return {@link ResponseEntity} con el arreglo de bytes del PDF y cabeceras de descarga.
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Integer id) {
        try {
            OrdenCompra orden = ordenCompraService.obtenerPorId(id);
            if (orden == null) {
                return ResponseEntity.notFound().build();
            }

            if (orden.getEstado() != EstadoOrdenCompra.APROBADA
                    && orden.getEstado() != EstadoOrdenCompra.RECIBIDA
                    && orden.getEstado() != EstadoOrdenCompra.FACTURADA
                    && orden.getEstado() != EstadoOrdenCompra.COMPLETADA) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            byte[] pdfBytes = pdfService.generarPdfOrdenCompra(orden);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "Orden_Compra_" + (orden.getNumeroOrden() != null ? orden.getNumeroOrden() : id) + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
