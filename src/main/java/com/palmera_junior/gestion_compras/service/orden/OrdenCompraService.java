package com.palmera_junior.gestion_compras.service.orden;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.dto.DetalleCompraDTO;
import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.dto.RecibirOrdenDTO;
import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.entity.DetalleCompra;
import com.palmera_junior.gestion_compras.entity.EstadoOrdenCompra;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.entity.Rol;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.events.OrdenCompraAprobadaEvent;
import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.repository.OrdenCompraRepository;
import com.palmera_junior.gestion_compras.repository.ProductoRepository;
import com.palmera_junior.gestion_compras.repository.ProveedorRepository;
import com.palmera_junior.gestion_compras.security.TotalesValidationService;
import com.palmera_junior.gestion_compras.service.correo.CorreoOrdenOutboxService;
import com.palmera_junior.gestion_compras.service.organizacion.ICentroCostoService;
import com.palmera_junior.gestion_compras.service.usuario.IUsuarioService;

/**
 * Servicio central para el procesamiento integral del ciclo de vida de las Órdenes de Compra.
 * Gestiona la persistencia de encabezados y detalles, validación de totales en backend,
 * aprobación con numeración consecutiva, recepción física de mercancías, asociación de facturas y anulación.
 */
@Service
public class OrdenCompraService implements IOrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final ICentroCostoService centroCostoService;
    private final IUsuarioService usuarioService;
    private final ApplicationEventPublisher eventPublisher;
    private final CorreoOrdenOutboxService correoOrdenOutboxService;
    private final TotalesValidationService totalesValidationService;

    /**
     * Constructor para inyección de dependencias de repositorios y servicios de soporte.
     */
    public OrdenCompraService(OrdenCompraRepository ordenCompraRepository, ProductoRepository productoRepository,
            ProveedorRepository proveedorRepository, ICentroCostoService centroCostoService,
            IUsuarioService usuarioService, ApplicationEventPublisher eventPublisher,
            CorreoOrdenOutboxService correoOrdenOutboxService, TotalesValidationService totalesValidationService) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.productoRepository = productoRepository;
        this.proveedorRepository = proveedorRepository;
        this.centroCostoService = centroCostoService;
        this.usuarioService = usuarioService;
        this.eventPublisher = eventPublisher;
        this.correoOrdenOutboxService = correoOrdenOutboxService;
        this.totalesValidationService = totalesValidationService;
    }

    /**
     * Qué hace: Retorna la totalidad de órdenes de compra en el sistema sin filtros.
     * A dónde apunta: {@link OrdenCompraRepository#findAll()} -> tabla orden_compra
     */
    @Override
    public List<OrdenCompra> listarOrdenesCompra() {        
        return ordenCompraRepository.findAll();
    }
    
    /**
     * Qué hace:
     * Construye una especificación dinámica con filtros combinados (texto de búsqueda en número o proveedor,
     * rango de fechas de orden, sede organizacional asignada, estado de orden y discrepancias)
     * y consulta la página correspondiente ordenada descendentemente por ID.
     * 
     * A dónde apunta:
     * - Repositorio: {@link OrdenCompraRepository#findAll(Specification, Pageable)} -> tabla orden_compra
     */
    @Override
    public Page<OrdenCompra> ordenesDeCompraPaginadas(Pageable pageable, String search, String fechaDesde,
            String fechaHasta, Integer idSede, boolean esNacional, String estado, boolean soloModificadas) {
        Specification<OrdenCompra> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (search != null && !search.isBlank()) {
            String termino = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> {
                jakarta.persistence.criteria.Join<Object, Object> provJoin = root.join("proveedor",
                        jakarta.persistence.criteria.JoinType.LEFT);
                return cb.or(
                        cb.like(cb.lower(root.get("numeroOrden")), termino),
                        cb.like(cb.lower(provJoin.get("nombre")), termino));
            });
        }

        if (fechaDesde != null && !fechaDesde.isBlank() && fechaHasta != null && !fechaHasta.isBlank()) {
            try {
                LocalDate fechaInicioFiltro = LocalDate.parse(fechaDesde, DateTimeFormatter.ISO_LOCAL_DATE);
                LocalDate fechaFinFiltro = LocalDate.parse(fechaHasta, DateTimeFormatter.ISO_LOCAL_DATE);
                spec = spec.and((root, query, cb) -> cb.between(root.get("fecha"), fechaInicioFiltro, fechaFinFiltro));
            } catch (DateTimeParseException ex) {
                // Ignorar rango inválido y continuar sin filtrar
            }
        } else if (fechaDesde != null && !fechaDesde.isBlank()) {
            try {
                LocalDate fechaInicioFiltro = LocalDate.parse(fechaDesde, DateTimeFormatter.ISO_LOCAL_DATE);
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fecha"), fechaInicioFiltro));
            } catch (DateTimeParseException ex) {
                // Ignorar fecha inválida y continuar sin filtrar
            }
        } else if (fechaHasta != null && !fechaHasta.isBlank()) {
            try {
                LocalDate fechaFinFiltro = LocalDate.parse(fechaHasta, DateTimeFormatter.ISO_LOCAL_DATE);
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fecha"), fechaFinFiltro));
            } catch (DateTimeParseException ex) {
                // Ignorar fecha inválida y continuar sin filtrar
            }
        }

        if (!esNacional && idSede != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("sede").get("idSede"), idSede));
        }

        // Filtro por estado
        if (estado != null && !estado.isBlank()) {
            try {
                EstadoOrdenCompra estadoEnum = EstadoOrdenCompra.valueOf(estado.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estadoEnum));
            } catch (IllegalArgumentException ex) {
                System.err.println("Valor de estado de orden de compra inválido: " + estado);
            }
        }
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "idOrden"));
        Page<OrdenCompra> resultado = ordenCompraRepository.findAll(spec, sortedPageable);

        resultado.getContent().forEach(orden -> {
            if (necesitaNumeroOrden(orden)) {
                asignarNumeroOrden(orden);
            }
        });

        return resultado;
    }

    /**
     * Qué hace: Busca una orden por su identificador primario.
     * A dónde apunta: {@link OrdenCompraRepository#findById(Object)}
     */
    @Override
    public OrdenCompra obtenerPorId(Integer idOrden) {
        OrdenCompra orden = ordenCompraRepository.findById(idOrden).orElse(null);
        if (orden != null && necesitaNumeroOrden(orden)) {
            return asignarNumeroOrden(orden);
        }
        return orden;
    }

    /**
     * Qué hace: Verifica si alguna línea de detalle de la orden tiene una cantidad recibida distinta de la cantidad solicitada.
     * A dónde apunta: Colección {@link OrdenCompra#getDetalles()} -> tabla detalle_compra
     */
    @Override
    public boolean tieneDiferenciasRecepcion(Integer idOrden) {
        OrdenCompra orden = ordenCompraRepository.findById(idOrden).orElse(null);
        if (orden == null) return false;
        List<com.palmera_junior.gestion_compras.entity.DetalleCompra> detalles = orden.getDetalles();
        if (detalles == null) return false;

        for (com.palmera_junior.gestion_compras.entity.DetalleCompra det : detalles) {
            Integer solicitado = det.getCantidad() != null ? det.getCantidad() : 0;
            Integer recibido = det.getCantidadRecibida() != null ? det.getCantidadRecibida() : 0;
            if (!solicitado.equals(recibido)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Qué hace:
     * Valida y recalcula montos en backend (SEC-05), vincula o crea al proveedor en la sede del usuario,
     * asocia el centro de costo, mapea y persiste cada línea de detalle y deja la orden en BORRADOR con número consecutivo temporal.
     * 
     * A dónde apunta:
     * - Tablas JPA: orden_compra, detalle_compra, proveedor, proveedor_sede
     * - Servicios: {@link TotalesValidationService}, {@link ICentroCostoService}
     */
    @Override
    @Transactional
    public OrdenCompra guardarOrdenDesdeDTO(OrdenCompraDTO dto) {
        // SEC-05: Validar y recalcular todos los totales en el servidor
        // No confiar en los valores enviados por el cliente
        totalesValidationService.validarYRecalcularTotalesOrden(dto);

        OrdenCompra orden = new OrdenCompra();

        // 2. Totales y fecha
        // Usar la fecha seleccionada por el usuario en el formulario; si no viene, usar la actual
        if (dto.getFecha() != null && !dto.getFecha().isBlank()) {
            orden.setFecha(LocalDate.parse(dto.getFecha(), DateTimeFormatter.ISO_LOCAL_DATE));
        } else {
            orden.setFecha(LocalDate.now());
        }
        // Los totales ahora vienen validados y recalculados del servicio
        orden.setDescuento(dto.getDescuento());
        orden.setSubTotal(dto.getSubTotal());
        orden.setIvaTotal(dto.getIvaTotal());

        // Flete: se guarda la decisión y el valor (si paga flete)
        boolean pagaFlete = Boolean.TRUE.equals(dto.getPagaFlete());
        orden.setPagaFlete(pagaFlete);
        if (pagaFlete) {
            orden.setValorFlete(dto.getValorFlete());
        } else {
            orden.setValorFlete(null);
        }
        orden.setTotal(calcularTotal(orden));

        Usuario usuarioLogueado = usuarioService.obtenerUsuarioAutenticado();

        // Asignar o crear proveedor asociado
        Proveedor proveedorAsignado = obtenerProveedorParaOrden(dto, usuarioLogueado);
        orden.setProveedor(proveedorAsignado);

        orden.setSede(usuarioLogueado.getSede());
        orden.setUsuario(usuarioLogueado);
        orden.setObservaciones(dto.getObservaciones());

        // Asignar centro de costo seleccionado
        if (dto.getIdCentroCosto() != null) {
            CentroCosto centroCosto = centroCostoService.buscarPorId(dto.getIdCentroCosto().intValue());
            orden.setCentroCosto(centroCosto);
        }

        // Mapeo de los detalles
        if (dto.getDetalles() != null) {
            List<DetalleCompra> detallesEntidad = dto.getDetalles().stream().map(dDto -> {
                DetalleCompra detalle = new DetalleCompra();

                if (dDto.getIdProducto() != null) {
                    Producto prodExistente = productoRepository.findById(dDto.getIdProducto().intValue()).orElse(null);
                    detalle.setProducto(prodExistente);
                } else {
                    detalle.setProducto(null);
                }

                // Datos snapshot del producto (ya validados y recalculados)
                detalle.setCodigoInventario(dDto.getCodigoInventario());
                detalle.setPresentacion(dDto.getPresentacion());
                detalle.setDescripcion(dDto.getDescripcion());

                // Valores numéricos de la línea (ya validados y recalculados)
                detalle.setCantidad(dDto.getCantidad());
                detalle.setValorUnitario(dDto.getValorUnitario());
                detalle.setIvaProducto(dDto.getIvaProducto());
                detalle.setValorIva(dDto.getValorIva());
                detalle.setValorTotalLinea(dDto.getValorTotalLinea());

                detalle.setOrdenCompra(orden);

                return detalle;
            }).collect(Collectors.toList());

            orden.setDetalles(detallesEntidad);
        }

        orden.setEstado(EstadoOrdenCompra.BORRADOR);
        OrdenCompra ordenGuardada = ordenCompraRepository.saveAndFlush(orden);
        return asignarNumeroOrden(ordenGuardada);
    }


    private Proveedor obtenerProveedorParaOrden(OrdenCompraDTO dto, Usuario usuarioLogueado) {
        String nit = dto.getNitProv() != null ? dto.getNitProv().trim() : null;
        String nombre = dto.getNombreProv() != null ? dto.getNombreProv().trim() : null;
        String ciudad = dto.getCiudadProv() != null ? dto.getCiudadProv().trim() : null;
        String direccion = dto.getDireccionProv() != null ? dto.getDireccionProv().trim() : null;
        String telefono = dto.getTelefonoProv() != null ? dto.getTelefonoProv().trim() : null;
        String correo = dto.getCorreoProv() != null ? dto.getCorreoProv().trim() : null;

        boolean tieneDatosProveedor = (nit != null && !nit.isBlank())
                || (nombre != null && !nombre.isBlank())
                || (ciudad != null && !ciudad.isBlank())
                || (direccion != null && !direccion.isBlank())
                || (telefono != null && !telefono.isBlank())
                || (correo != null && !correo.isBlank());

        if (dto.getIdProv() == null && !tieneDatosProveedor) {
            return null;
        }

        if (dto.getIdProv() != null) {
            if (nit == null || nit.isBlank()) {
                throw new RuntimeException("El NIT del proveedor es obligatorio.");
            }

            Proveedor proveedorAsignado = proveedorRepository.findById(dto.getIdProv().intValue())
                    .orElseThrow(() -> new RuntimeException("Proveedor seleccionado no existe"));

            Optional<Proveedor> proveedorConMismoNit = proveedorRepository.findByNitIgnoreCase(nit);
            if (proveedorConMismoNit.isPresent()
                    && !proveedorConMismoNit.get().getIdProv().equals(proveedorAsignado.getIdProv())) {
                throw new RuntimeException("Ya existe otro proveedor con ese NIT.");
            }

            actualizarCamposProveedor(proveedorAsignado, dto);
            asociarProveedorASede(proveedorAsignado, usuarioLogueado.getSede());
            return proveedorRepository.save(proveedorAsignado);
        }

        if (nit == null || nit.isBlank()) {
            throw new RuntimeException("El NIT del proveedor es obligatorio.");
        }

        Optional<Proveedor> proveedorExistente = proveedorRepository.findByNitIgnoreCase(nit);
        if (proveedorExistente.isPresent()) {
            Proveedor proveedor = proveedorExistente.get();
            actualizarCamposProveedor(proveedor, dto);
            asociarProveedorASede(proveedor, usuarioLogueado.getSede());
            return proveedorRepository.save(proveedor);
        }

        Proveedor nuevoProv = new Proveedor();
        nuevoProv.setNit(nit);
        nuevoProv.setNombre(nombre);
        nuevoProv.setCorreo(correo);
        nuevoProv.setDireccion(direccion);
        nuevoProv.setTelefono(telefono);
        nuevoProv.setCiudad(ciudad);
        asociarProveedorASede(nuevoProv, usuarioLogueado.getSede());
        return proveedorRepository.save(nuevoProv);
    }

    private void actualizarCamposProveedor(Proveedor proveedor, OrdenCompraDTO dto) {
        proveedor.setNit(dto.getNitProv());
        proveedor.setNombre(dto.getNombreProv());
        proveedor.setCorreo(dto.getCorreoProv());
        proveedor.setDireccion(dto.getDireccionProv());
        proveedor.setTelefono(dto.getTelefonoProv());
        proveedor.setCiudad(dto.getCiudadProv());
    }

    private void asociarProveedorASede(Proveedor proveedor, Sede sede) {
        if (sede == null) {
            return;
        }
        if (!proveedor.getSedes().contains(sede)) {
            proveedor.getSedes().add(sede);
        }
    }

    /**
     * Qué hace:
     * Actualiza una orden de compra existente en estado BORRADOR, recalculando montos, regenerando número si cambió el mes
     * y reemplazando los ítems de detalle.
     * 
     * A dónde apunta:
     * - Tablas JPA: orden_compra, detalle_compra
     */
    @Override
    @Transactional
    public OrdenCompra actualizarOrdenDesdeDTO(Integer idOrden, OrdenCompraDTO dto) {
        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new RuntimeException("Solo las órdenes en BORRADOR pueden editarse");
        }

        // SEC-05: Validar y recalcular todos los totales en el servidor
        // No confiar en los valores enviados por el cliente
        totalesValidationService.validarYRecalcularTotalesOrden(dto);

        LocalDate fechaAnterior = orden.getFecha();
        LocalDate fechaNueva = fechaAnterior;

        Usuario usuarioLogueado = usuarioService.obtenerUsuarioAutenticado();

        Proveedor proveedorAsignado = obtenerProveedorParaOrden(dto, usuarioLogueado);
        orden.setProveedor(proveedorAsignado);
        orden.setObservaciones(dto.getObservaciones());

        if (dto.getFecha() != null && !dto.getFecha().isBlank()) {
            fechaNueva = LocalDate.parse(dto.getFecha(), DateTimeFormatter.ISO_LOCAL_DATE);
            orden.setFecha(fechaNueva);
        }
        // Los totales ahora vienen validados y recalculados del servicio
        orden.setDescuento(dto.getDescuento());
        orden.setSubTotal(dto.getSubTotal());
        orden.setIvaTotal(dto.getIvaTotal());

        // Flete: se actualiza la decisión y el valor
        boolean pagaFlete = Boolean.TRUE.equals(dto.getPagaFlete());
        orden.setPagaFlete(pagaFlete);
        if (pagaFlete) {
            orden.setValorFlete(dto.getValorFlete());
        } else {
            orden.setValorFlete(null);
        }
        orden.setTotal(calcularTotal(orden));

        if (dto.getIdCentroCosto() != null) {
            CentroCosto centroCosto = centroCostoService.buscarPorId(dto.getIdCentroCosto().intValue());
            orden.setCentroCosto(centroCosto);
        }

        if (dto.getDetalles() != null) {
            orden.getDetalles().clear();
            List<DetalleCompra> detallesEntidad = dto.getDetalles().stream().map(dDto -> {
                DetalleCompra detalle = new DetalleCompra();
                if (dDto.getIdProducto() != null) {
                    Producto prodExistente = productoRepository.findById(dDto.getIdProducto().intValue()).orElse(null);
                    detalle.setProducto(prodExistente);
                } else {
                    detalle.setProducto(null);
                }
                // Usar valores validados y recalculados
                detalle.setCodigoInventario(dDto.getCodigoInventario());
                detalle.setPresentacion(dDto.getPresentacion());
                detalle.setDescripcion(dDto.getDescripcion());
                detalle.setCantidad(dDto.getCantidad());
                detalle.setValorUnitario(dDto.getValorUnitario());
                detalle.setIvaProducto(dDto.getIvaProducto());
                detalle.setValorIva(dDto.getValorIva());
                detalle.setValorTotalLinea(dDto.getValorTotalLinea());
                detalle.setOrdenCompra(orden);
                return detalle;
            }).collect(Collectors.toList());
            orden.getDetalles().addAll(detallesEntidad);
        }

        OrdenCompra ordenGuardada = ordenCompraRepository.saveAndFlush(orden);
        if (debeRegenerarNumeroOrden(ordenGuardada, fechaNueva, fechaAnterior) || necesitaNumeroOrden(ordenGuardada)) {
            return asignarNumeroOrden(ordenGuardada);
        }

        return ordenGuardada;
    }

    private boolean necesitaNumeroOrden(OrdenCompra orden) {
        return orden != null
                && orden.getEstado() == EstadoOrdenCompra.BORRADOR
                && (orden.getNumeroOrden() == null || orden.getNumeroOrden().isBlank());
    }

    private boolean debeRegenerarNumeroOrden(OrdenCompra orden, LocalDate fechaNueva, LocalDate fechaAnterior) {
        if (orden == null || orden.getEstado() != EstadoOrdenCompra.BORRADOR || fechaNueva == null) {
            return false;
        }

        if (fechaAnterior == null) {
            return true;
        }

        return fechaAnterior.getMonthValue() != fechaNueva.getMonthValue();
    }

    private String generarNumeroOrden(OrdenCompra ordenCompra) {

        String prefijo = ordenCompra.getSede()
                .getPrefijoCiudad();

        String mes = String.format(
                "%02d",
                ordenCompra.getFecha()
                        .getMonthValue());

        String consecutivo = String.format(
                "%04d",
                ordenCompra.getIdOrden());

        return prefijo
                + "-"
                + mes
                + "-"
                + consecutivo;
    }

    private OrdenCompra asignarNumeroOrden(
            OrdenCompra ordenCompra) {

        String numeroOrden = generarNumeroOrden(ordenCompra);

        ordenCompra.setNumeroOrden(numeroOrden);

        return ordenCompraRepository.saveAndFlush(ordenCompra);
    }

    /**
     * Qué hace:
     * Transiciona la orden a estado APROBADA, asigna usuario y fecha de aprobación,
     * registra el correo pendiente en la tabla de auditoría outbox y dispara el evento {@link OrdenCompraAprobadaEvent}.
     * 
     * A dónde apunta:
     * - Tabla: orden_compra
     * - Servicio outbox: {@link CorreoOrdenOutboxService#registrarPendiente}
     * - Event Publisher: {@link ApplicationEventPublisher#publishEvent}
     */
    @Override
    @PreAuthorize("hasRole('APROBADOR') or hasRole('ADMINISTRADOR')")
    @Transactional
    public OrdenCompra aprobarOrden(Integer idOrden) {

        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new RuntimeException(
                    "Solo las órdenes en estado BORRADOR pueden aprobarse");
        }

        Usuario usuarioAprobador = usuarioService.obtenerUsuarioAutenticado();

        boolean esAdministrador = usuarioAprobador.getRol() == Rol.ADMINISTRADOR;

        boolean mismaSede = orden.getSede().getIdSede()
                .equals(usuarioAprobador.getSede().getIdSede());

        if (!esAdministrador && !mismaSede) {
            throw new RuntimeException(
                    "No tiene permisos para aprobar órdenes de otra sede");
        }

        orden.setEstado(EstadoOrdenCompra.APROBADA);
        orden.setUsuarioAprobacion(usuarioAprobador);
        orden.setFechaAprobacion(LocalDate.now());

        OrdenCompra ordenGuardada = ordenCompraRepository.save(orden);

        Long idAuditoria = correoOrdenOutboxService
                .registrarPendiente(ordenGuardada);

        eventPublisher.publishEvent(
                new OrdenCompraAprobadaEvent(idAuditoria));

        return ordenGuardada;
    }

    /**
     * Qué hace:
     * Registra la recepción física de la orden, actualizando cantidades recibidas por línea,
     * receptor, observaciones, foto/soporte y flete. Pasa la orden a RECIBIDA o COMPLETADA (si ya estaba facturada).
     * 
     * A dónde apunta:
     * - Tablas: orden_compra, detalle_compra
     */
    @Override
    @Transactional
    public OrdenCompra recibirOrden(
            Integer idOrden,
            RecibirOrdenDTO dto) {

        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException(
                        "Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.APROBADA && orden.getEstado() != EstadoOrdenCompra.FACTURADA) {

            throw new RuntimeException(
                    "Solo las órdenes APROBADAS o FACTURADAS pueden recibirse");
        }

        if (dto.getProductos() != null && !dto.getProductos().isEmpty()) {
            validarProductosRecibidos(orden, dto.getProductos());
            actualizarDetallesRecibidos(orden, dto.getProductos());
        }

        // Validación de flete: solo se permite capturar el valor del flete si al crear la orden se definió que se pagará flete.
        if (!Boolean.TRUE.equals(orden.getPagaFlete())) {
            if (dto.getValorFlete() != null && dto.getValorFlete().signum() != 0) {
                throw new RuntimeException(
                        "Esta orden no contempla el pago de flete. No puede ingresar un valor de flete.");
            }
        } else {
            if (dto.getValorFlete() != null) {
                orden.setValorFlete(dto.getValorFlete());
            }
        }

        // Si el DTO trae numero de factura al recibir, se asigna
        if (dto.getNumeroFactura() != null && !dto.getNumeroFactura().isBlank()) {
            orden.setNumeroFactura(dto.getNumeroFactura());
            orden.setSeFacturo(true);
        }

        orden.setRecibidoPor(
                dto.getRecibidoPor());

        orden.setObservacionRecepcion(
                dto.getObservacionRecepcion());

        orden.setFotoRecepcion(
                dto.getFotoRecepcion());

        // Recalcular el total incluyendo el flete (si aplica)
        orden.setTotal(calcularTotal(orden));

        orden.setFechaRecepcion(
                LocalDate.now());

        // Marcar recibida
        orden.setSeRecibio(true);

        // Determinar estado final: si ya estaba facturada o se facturó ahora, pasar a COMPLETADA
        if (Boolean.TRUE.equals(orden.getSeFacturo())) {
            orden.setEstado(EstadoOrdenCompra.COMPLETADA);
        } else {
            orden.setEstado(EstadoOrdenCompra.RECIBIDA);
        }

        return ordenCompraRepository.save(
                orden);
    }


    private DetalleCompra buscarDetallePorProducto(OrdenCompra orden, RecibirOrdenDTO.ProductoRecepcionDTO producto) {
        if (orden == null || orden.getDetalles() == null) {
            return null;
        }

        if (producto.getIdDetalle() != null) {
            return orden.getDetalles().stream()
                    .filter(detalle -> detalle != null && detalle.getIdDetalle() != null)
                    .filter(detalle -> detalle.getIdDetalle().equals(producto.getIdDetalle()))
                    .findFirst()
                    .orElse(null);
        }

        if (producto.getIdProducto() != null) {
            return orden.getDetalles().stream()
                    .filter(detalle -> detalle != null && detalle.getProducto() != null && detalle.getProducto().getIdProducto() != null)
                    .filter(detalle -> producto.getIdProducto().longValue() == detalle.getProducto().getIdProducto().longValue())
                    .findFirst()
                    .orElse(null);
        }

        return orden.getDetalles().stream()
                .filter(detalle -> detalle != null)
                .filter(detalle -> {
                    boolean coincideCodigo = producto.getCodigoInventario() != null
                            && producto.getCodigoInventario().equalsIgnoreCase(detalle.getCodigoInventario());
                    boolean coincidePresentacion = producto.getPresentacion() != null
                            && producto.getPresentacion().equalsIgnoreCase(detalle.getPresentacion());
                    boolean coincideDescripcion = producto.getDescripcion() != null
                            && producto.getDescripcion().equalsIgnoreCase(detalle.getDescripcion());
                    return coincideCodigo || coincidePresentacion || coincideDescripcion;
                })
                .findFirst()
                .orElse(null);
    }

    private void actualizarDetallesRecibidos(OrdenCompra orden, List<RecibirOrdenDTO.ProductoRecepcionDTO> productos) {
        if (productos == null || productos.isEmpty()) {
            return;
        }

        for (RecibirOrdenDTO.ProductoRecepcionDTO producto : productos) {
            DetalleCompra detalle = buscarDetallePorProducto(orden, producto);
            if (detalle == null) {
                continue;
            }

            boolean recibido = Boolean.TRUE.equals(producto.getRecibido());
            int cantidadSolicitada = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
            int cantidadRecibida = producto.getCantidadRecibida() == null ? 0 : producto.getCantidadRecibida();
            if (recibido) {
                cantidadRecibida = cantidadSolicitada;
            }

            detalle.setRecibido(recibido);
            detalle.setCantidadRecibida(cantidadRecibida);
        }
    }

    private void validarProductosRecibidos(OrdenCompra orden, List<RecibirOrdenDTO.ProductoRecepcionDTO> productos) {
        if (productos == null || productos.isEmpty()) {
            return;
        }

        for (RecibirOrdenDTO.ProductoRecepcionDTO producto : productos) {
            DetalleCompra detalle = buscarDetallePorProducto(orden, producto);

            if (detalle == null) {
                throw new RuntimeException("No se encontró el producto de la orden para validar la recepción.");
            }

            int cantidadSolicitada = detalle.getCantidad() == null ? 0 : detalle.getCantidad();
            int cantidadRecibida = producto.getCantidadRecibida() == null ? 0 : producto.getCantidadRecibida();

            if (Boolean.TRUE.equals(producto.getRecibido())) {
                cantidadRecibida = cantidadSolicitada;
            }

            if (cantidadRecibida < 0) {
                throw new RuntimeException("La cantidad recibida no puede ser negativa para el producto " + (detalle.getDescripcion() != null ? detalle.getDescripcion() : detalle.getPresentacion()));
            }

            if (cantidadRecibida > cantidadSolicitada) {
                throw new RuntimeException("La cantidad recibida supera la cantidad solicitada para el producto " + (detalle.getDescripcion() != null ? detalle.getDescripcion() : detalle.getPresentacion()));
            }

            if (producto.getCantidadSolicitada() != null && producto.getCantidadSolicitada() > 0
                    && !Objects.equals(producto.getCantidadSolicitada(), cantidadSolicitada)) {
                // Antes de fallar, registrar detalle en logs y aceptar el valor del servidor.
                System.err.println("Advertencia: cantidadSolicitada enviada (" + producto.getCantidadSolicitada() + ") no coincide con la orden (" + cantidadSolicitada + ") para el producto: " + (detalle.getDescripcion() != null ? detalle.getDescripcion() : detalle.getPresentacion()));
                // Normalizar: usar el valor del detalle en servidor como fuente de la verdad
                producto.setCantidadSolicitada(cantidadSolicitada);
                // NOTA: no interrumpir el flujo; se actualizan las cantidades recibidas en actualizarDetallesRecibidos
            }
        }
    }

    /**
     * Qué hace: Registra factura comercial sin archivo adjunto.
     * A dónde apunta: delega a {@link #facturarOrden(Integer, String, String)}
     */
    public OrdenCompra facturarOrden(Integer idOrden, String numeroFactura) {
        return facturarOrden(idOrden, numeroFactura, null);
    }

    /**
     * Qué hace:
     * Guarda el número de factura y soporte digital en base64; si la orden ya estaba recibida, pasa a COMPLETADA, de lo contrario a FACTURADA.
     * 
     * A dónde apunta:
     * - Tabla: orden_compra
     */
    @Override
    @Transactional
    public OrdenCompra facturarOrden(Integer idOrden, String numeroFactura, String fotoFactura) {
        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.APROBADA && orden.getEstado() != EstadoOrdenCompra.RECIBIDA) {
            throw new RuntimeException("Solo las órdenes APROBADAS o RECIBIDAS pueden facturarse");
        }

        if (numeroFactura == null || numeroFactura.isBlank()) {
            throw new RuntimeException("Número de factura requerido para facturar la orden");
        }

        orden.setNumeroFactura(numeroFactura);
        if (fotoFactura != null && !fotoFactura.isBlank()) {
            orden.setFotoFactura(fotoFactura);
        }
        orden.setSeFacturo(true);

        // Si ya se recibió, completar; si no, marcar como FACTURADA
        if (Boolean.TRUE.equals(orden.getSeRecibio())) {
            orden.setEstado(EstadoOrdenCompra.COMPLETADA);
        } else {
            orden.setEstado(EstadoOrdenCompra.FACTURADA);
        }

        return ordenCompraRepository.save(orden);
    }

    /**
     * Qué hace:
     * Cambia el estado de una orden a ANULADA si está en estado BORRADOR o APROBADA.
     * 
     * A dónde apunta:
     * - Tabla: orden_compra
     */
    @Override
    @Transactional
    public OrdenCompra anularOrden(Integer idOrden) {
        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.BORRADOR && orden.getEstado() != EstadoOrdenCompra.APROBADA) {
            throw new RuntimeException("Solo las órdenes en BORRADOR o APROBADA pueden anularse");
        }

        orden.setEstado(EstadoOrdenCompra.ANULADA);
        return ordenCompraRepository.save(orden);
    }

    /**
     * Calcula el total de la orden: subtotal + IVA - descuento + flete (si se
     * paga).
     */
    private BigDecimal calcularTotal(OrdenCompra orden) {
        BigDecimal subtotal = orden.getSubTotal() != null ? orden.getSubTotal() : BigDecimal.ZERO;
        BigDecimal iva = orden.getIvaTotal() != null ? orden.getIvaTotal() : BigDecimal.ZERO;
        BigDecimal descuento = orden.getDescuento() != null ? orden.getDescuento() : BigDecimal.ZERO;
        BigDecimal flete = BigDecimal.ZERO;

        if (Boolean.TRUE.equals(orden.getPagaFlete()) && orden.getValorFlete() != null) {
            flete = orden.getValorFlete();
        }

        return subtotal.add(iva).subtract(descuento).add(flete);
    }

    /**
     * Qué hace:
     * Construye y retorna un {@link OrdenCompraDTO} con los detalles de cabecera, proveedor, líneas y estados para consumo AJAX.
     * 
     * A dónde apunta:
     * - Tablas: orden_compra, detalle_compra, proveedor, usuario
     */
    @Override
    public OrdenCompraDTO obtenerOrdenDTO(
            Integer id) {


        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Orden no encontrada"));

        OrdenCompraDTO dto = new OrdenCompraDTO();

        dto.setFecha(
                orden.getFecha().toString());

        if (orden.getProveedor() != null) {
            dto.setNombreProv(orden.getProveedor().getNombre());
            dto.setNitProv(orden.getProveedor().getNit());
            dto.setCiudadProv(orden.getProveedor().getCiudad());
            dto.setDireccionProv(orden.getProveedor().getDireccion());
            dto.setTelefonoProv(orden.getProveedor().getTelefono());
            dto.setCorreoProv(orden.getProveedor().getCorreo());
        } else {
            dto.setNombreProv(null);
            dto.setNitProv(null);
            dto.setCiudadProv(null);
            dto.setDireccionProv(null);
            dto.setTelefonoProv(null);
            dto.setCorreoProv(null);
        }

        dto.setObservaciones(
                orden.getObservaciones());

        dto.setSubTotal(
                orden.getSubTotal());

        dto.setIvaTotal(
                orden.getIvaTotal());

        dto.setDescuento(
                orden.getDescuento());

        dto.setTotal(
                orden.getTotal());

        // Datos del flete
        dto.setPagaFlete(
                orden.getPagaFlete());
        dto.setValorFlete(
                orden.getValorFlete());

        dto.setNumeroOrden(
                orden.getNumeroOrden());

        dto.setEstado(
                orden.getEstado() != null ? orden.getEstado().name() : null);

        dto.setAprobadoPor(
                orden.getUsuarioAprobacion() != null
                        ? orden.getUsuarioAprobacion().getNombre() + " " + orden.getUsuarioAprobacion().getApellido()
                        : null);

        dto.setFechaAprobacion(
                orden.getFechaAprobacion() != null ? orden.getFechaAprobacion().toString() : null);

        dto.setRecibidoPor(orden.getRecibidoPor() != null ? orden.getRecibidoPor() : null);
        dto.setFechaRecepcion(orden.getFechaRecepcion() != null ? (orden.getFechaRecepcion().toString()) : null);
        dto.setNumeroFactura(orden.getNumeroFactura() != null ? orden.getNumeroFactura() : null);
        dto.setFotoRecepcion(orden.getFotoRecepcion());

        dto.setIdCentroCosto(
                orden.getCentroCosto() != null ? orden.getCentroCosto().getIdCentroCosto().longValue() : null);

        dto.setDetalles(
                mapearDetalles(orden));

        return dto;
    }

    private java.util.List<DetalleCompraDTO> mapearDetalles(OrdenCompra orden) {
        if (orden == null || orden.getDetalles() == null) {
            return java.util.Collections.emptyList();
        }

        return orden.getDetalles().stream().map(detalle -> {
            DetalleCompraDTO detalleDTO = new DetalleCompraDTO();
            if (detalle.getProducto() != null) {
                detalleDTO.setIdProducto(detalle.getProducto().getIdProducto() != null
                        ? detalle.getProducto().getIdProducto().longValue()
                        : null);
            }
            detalleDTO.setCodigoInventario(detalle.getCodigoInventario());
            detalleDTO.setPresentacion(detalle.getPresentacion());
            detalleDTO.setDescripcion(detalle.getDescripcion());
            detalleDTO.setCantidad(detalle.getCantidad());
            detalleDTO.setCantidadRecibida(detalle.getCantidadRecibida() != null ? detalle.getCantidadRecibida() : 0);
            detalleDTO.setRecibido(detalle.getRecibido() != null ? detalle.getRecibido() : false);
            detalleDTO.setValorUnitario(detalle.getValorUnitario());
            detalleDTO.setIvaProducto(detalle.getIvaProducto());
            detalleDTO.setValorIva(detalle.getValorIva());
            detalleDTO.setValorTotalLinea(detalle.getValorTotalLinea());
            return detalleDTO;
        }).collect(java.util.stream.Collectors.toList());
    }

}
