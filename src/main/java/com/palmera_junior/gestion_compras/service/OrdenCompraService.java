package com.palmera_junior.gestion_compras.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import com.palmera_junior.gestion_compras.repository.OrdenCompraRepository;
import com.palmera_junior.gestion_compras.repository.ProductoRepository;
import com.palmera_junior.gestion_compras.repository.ProveedorRepository;
import com.palmera_junior.gestion_compras.security.CustomOAuth2User;
import com.palmera_junior.gestion_compras.security.UsuarioPrincipal;
import com.palmera_junior.gestion_compras.entity.Proveedor;

@Service
public class OrdenCompraService {


    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private CentroCostoService centroCostoService;

    private Usuario obtenerUsuarioAutenticado() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Object principal = auth.getPrincipal();

        if (principal instanceof UsuarioPrincipal up) {
            return up.getUsuario();
        }

        if (principal instanceof CustomOAuth2User cu) {
            return cu.getUsuario();
        }

        throw new RuntimeException(
                "Tipo de autenticación no soportado");
    }

    // Método para listar órdenes paginadas en el Dashboard
    public Page<OrdenCompra> ordenesDeCompraPaginadas(Pageable pageable, String search, String fechaDesde,
            String fechaHasta, Integer idSede, boolean esNacional) {
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

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "fecha"));
        Page<OrdenCompra> resultado = ordenCompraRepository.findAll(spec, sortedPageable);

        resultado.getContent().forEach(orden -> {
            if (necesitaNumeroOrden(orden)) {
                asignarNumeroOrden(orden);
            }
        });

        return resultado;
    }

    // Método para obtener todas las órdenes
    public List<OrdenCompra> getAllOrdenes() {
        return ordenCompraRepository.findAll();
    }

    // Método para buscar una orden por ID
    public OrdenCompra obtenerPorId(Integer idOrden) {
        OrdenCompra orden = ordenCompraRepository.findById(idOrden).orElse(null);
        if (orden != null && necesitaNumeroOrden(orden)) {
            return asignarNumeroOrden(orden);
        }
        return orden;
    }

    // Guardar orden de compra desde el DTO del formulario
    @Transactional
    public OrdenCompra guardarOrdenDesdeDTO(OrdenCompraDTO dto) {
        OrdenCompra orden = new OrdenCompra();

        // 2. Totales y fecha
        // Usar la fecha seleccionada por el usuario en el formulario; si no viene, usar
        // la actual
        if (dto.getFecha() != null && !dto.getFecha().isBlank()) {
            orden.setFecha(LocalDate.parse(dto.getFecha(), DateTimeFormatter.ISO_LOCAL_DATE));
        } else {
            orden.setFecha(LocalDate.now());
        }
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

        // 3. obtener sede

        Usuario usuarioLogueado = obtenerUsuarioAutenticado();

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

        // 3. Mapeo de los detalles
        if (dto.getDetalles() != null) {
            List<DetalleCompra> detallesEntidad = dto.getDetalles().stream().map(dDto -> {
                DetalleCompra detalle = new DetalleCompra();

                // Si el usuario seleccionó un producto existente que tiene ID, puedes buscarlo
                // opcionalmente:
                if (dDto.getIdProducto() != null) {
                    Producto prodExistente = productoRepository.findById(dDto.getIdProducto().intValue()).orElse(null);
                    detalle.setProducto(prodExistente);
                } else {
                    detalle.setProducto(null); // Producto nuevo, no hay llave foránea
                }

                // Datos snapshot del producto
                detalle.setCodigoInventario(dDto.getCodigoInventario());
                detalle.setPresentacion(dDto.getPresentacion());
                detalle.setDescripcion(dDto.getDescripcion());

                // Valores numéricos de la línea
                detalle.setCantidad(dDto.getCantidad());
                detalle.setValorUnitario(dDto.getValorUnitario());
                detalle.setIvaProducto(dDto.getIvaProducto());
                detalle.setValorIva(dDto.getValorIva());
                detalle.setValorTotalLinea(dDto.getValorTotalLinea());

                // (Misma regla de arriba para la FK de idProducto)

                // Relación bidireccional (asignar el padre al hijo)
                detalle.setOrdenCompra(orden);

                return detalle;
            }).collect(Collectors.toList());

            // Asignamos la lista completa de detalles a la orden
            orden.setDetalles(detallesEntidad);
        }

        orden.setEstado(
                EstadoOrdenCompra.BORRADOR);
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

    @Transactional
    public OrdenCompra actualizarOrdenDesdeDTO(Integer idOrden, OrdenCompraDTO dto) {
        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new RuntimeException("Solo las órdenes en BORRADOR pueden editarse");
        }

        LocalDate fechaAnterior = orden.getFecha();
        LocalDate fechaNueva = fechaAnterior;

        Usuario usuarioLogueado = obtenerUsuarioAutenticado();

        Proveedor proveedorAsignado = obtenerProveedorParaOrden(dto, usuarioLogueado);
        orden.setProveedor(proveedorAsignado);
        orden.setObservaciones(dto.getObservaciones());

        if (dto.getFecha() != null && !dto.getFecha().isBlank()) {
            fechaNueva = LocalDate.parse(dto.getFecha(), DateTimeFormatter.ISO_LOCAL_DATE);
            orden.setFecha(fechaNueva);
        }
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

    @PreAuthorize("hasRole('APROBADOR')")
    @Transactional
    public OrdenCompra aprobarOrden(Integer idOrden) {

        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new RuntimeException(
                    "Solo las órdenes en BORRADOR pueden aprobarse");
        }

        Usuario usuarioAprobador = obtenerUsuarioAutenticado();

        if (!orden.getSede().getIdSede()
                .equals(usuarioAprobador.getSede().getIdSede())) {

            throw new RuntimeException(
                    "No tiene permisos para aprobar órdenes de otra sede");
        }

        if (usuarioAprobador.getRol() != Rol.APROBADOR) {

            throw new RuntimeException(
                    "Solo un aprobador puede aprobar órdenes");
        }

        orden.setEstado(EstadoOrdenCompra.APROBADA);

        orden.setUsuarioAprobacion(usuarioAprobador);

        orden.setFechaAprobacion(LocalDate.now());

        return ordenCompraRepository.save(orden);
    }

    @Transactional
    public void eliminarOrden(Integer idOrden) {

        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.BORRADOR) {
            throw new RuntimeException("Solo las órdenes en BORRADOR pueden eliminarse");
        }

        Usuario usuarioLogueado =
        obtenerUsuarioAutenticado();

        if (orden.getUsuario() == null
                || !orden.getUsuario().getIdUsuario().equals(usuarioLogueado.getIdUsuario())) {
            throw new RuntimeException("Solo el usuario que creó la orden puede eliminarla");
        }

        ordenCompraRepository.delete(orden);
    }

    @Transactional
    public OrdenCompra recibirOrden(
            Integer idOrden,
            RecibirOrdenDTO dto) {

        OrdenCompra orden = ordenCompraRepository.findById(idOrden)
                .orElseThrow(() -> new RuntimeException(
                        "Orden no encontrada"));

        if (orden.getEstado() != EstadoOrdenCompra.APROBADA) {

            throw new RuntimeException(
                    "Solo las órdenes APROBADAS pueden recibirse");
        }

        // Validación de flete: solo se permite capturar el valor del flete
        // si al crear la orden se definió que se pagará flete.
        if (!Boolean.TRUE.equals(orden.getPagaFlete())) {
            if (dto.getValorFlete() != null && dto.getValorFlete().signum() != 0) {
                throw new RuntimeException(
                        "Esta orden no contempla el pago de flete. No puede ingresar un valor de flete.");
            }
        } else {
            // Si la orden contempla flete, se actualiza el valor (se conserva el
            // inicial si no se envía uno nuevo).
            if (dto.getValorFlete() != null) {
                orden.setValorFlete(dto.getValorFlete());
            }
        }

        orden.setNumeroFactura(
                dto.getNumeroFactura());

        orden.setRecibidoPor(
                dto.getRecibidoPor());

        orden.setObservacionRecepcion(
                dto.getObservacionRecepcion());

        // Recalcular el total incluyendo el flete (si aplica)
        orden.setTotal(calcularTotal(orden));

        orden.setFechaRecepcion(
                LocalDate.now());

        orden.setEstado(
                EstadoOrdenCompra.RECIBIDA);

        return ordenCompraRepository.save(
                orden);
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
            detalleDTO.setValorUnitario(detalle.getValorUnitario());
            detalleDTO.setIvaProducto(detalle.getIvaProducto());
            detalleDTO.setValorIva(detalle.getValorIva());
            detalleDTO.setValorTotalLinea(detalle.getValorTotalLinea());
            return detalleDTO;
        }).collect(java.util.stream.Collectors.toList());
    }

}
