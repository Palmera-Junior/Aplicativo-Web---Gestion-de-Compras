package com.palmera_junior.gestion_compras.service;

import java.time.LocalDate;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
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
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.OrdenCompraRepository;
import com.palmera_junior.gestion_compras.repository.ProductoRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;

@Service
public class OrdenCompraService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CentroCostoService centroCostoService;

    // Método para listar órdenes paginadas en el Dashboard
    public Page<OrdenCompra> ordenesDeCompraPaginadas(Pageable pageable, String search, String fechaDesde, String fechaHasta, Integer idSede, boolean esNacional) {
        Specification<OrdenCompra> spec = Specification.where((root, query, cb) -> cb.conjunction());

        if (search != null && !search.isBlank()) {
            String termino = "%" + search.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("numeroOrden")), termino),
                    cb.like(cb.lower(root.get("nombreProv")), termino)
            ));
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

        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "fecha"));
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

        // 1. Datos principales y del Proveedor (Snapshot del formulario)
        orden.setNitProv(dto.getNitProv());
        orden.setNombreProv(dto.getNombreProv());
        orden.setTelefonoProv(dto.getTelefonoProv());
        orden.setCiudadProv(dto.getCiudadProv());
        orden.setCorreoProv(dto.getCorreoProv());
        orden.setDireccionProv(dto.getDireccionProv());
        orden.setObservaciones(dto.getObservaciones());

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
        orden.setTotal(dto.getTotal());

        // 3. obtener sede

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        // Esto suele devolver el email o el username con el que se logueó

        // 2. Buscar el usuario en la base de datos
        Usuario usuarioLogueado = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new RuntimeException("Usuario no autenticado o no encontrado"));

        orden.setSede(usuarioLogueado.getSede());
        orden.setUsuario(usuarioLogueado);

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
                    Producto prodExistente = productoRepository.findById(dDto.getIdProducto()).orElse(null);
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

        System.out.println("NIT: " + dto.getNitProv());
        System.out.println("Nombre: " + dto.getNombreProv());
        System.out.println("Ciudad: " + dto.getCiudadProv());
        System.out.println("Correo: " + dto.getCorreoProv());

        orden.setEstado(
                EstadoOrdenCompra.BORRADOR);
        OrdenCompra ordenGuardada = ordenCompraRepository.saveAndFlush(orden);

                return asignarNumeroOrden(ordenGuardada);
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

                orden.setNitProv(dto.getNitProv());
                orden.setNombreProv(dto.getNombreProv());
                orden.setTelefonoProv(dto.getTelefonoProv());
                orden.setCiudadProv(dto.getCiudadProv());
                orden.setCorreoProv(dto.getCorreoProv());
                orden.setDireccionProv(dto.getDireccionProv());
                orden.setObservaciones(dto.getObservaciones());

                if (dto.getFecha() != null && !dto.getFecha().isBlank()) {
                        fechaNueva = LocalDate.parse(dto.getFecha(), DateTimeFormatter.ISO_LOCAL_DATE);
                        orden.setFecha(fechaNueva);
                }
                orden.setDescuento(dto.getDescuento());
                orden.setSubTotal(dto.getSubTotal());
                orden.setIvaTotal(dto.getIvaTotal());
                orden.setTotal(dto.getTotal());

                if (dto.getIdCentroCosto() != null) {
                        CentroCosto centroCosto = centroCostoService.buscarPorId(dto.getIdCentroCosto().intValue());
                        orden.setCentroCosto(centroCosto);
                }

                if (dto.getDetalles() != null) {
                        orden.getDetalles().clear();
                        List<DetalleCompra> detallesEntidad = dto.getDetalles().stream().map(dDto -> {
                                DetalleCompra detalle = new DetalleCompra();
                                if (dDto.getIdProducto() != null) {
                                        Producto prodExistente = productoRepository.findById(dDto.getIdProducto()).orElse(null);
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String username = auth.getName();

        Usuario usuarioAprobador = usuarioRepository.findByNombreUsuario(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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

        orden.setNumeroFactura(
                dto.getNumeroFactura());

        orden.setRecibidoPor(
                dto.getRecibidoPor());

        orden.setObservacionRecepcion(
                dto.getObservacionRecepcion());

        orden.setFechaRecepcion(
                LocalDate.now());

        orden.setEstado(
                EstadoOrdenCompra.RECIBIDA);

        return ordenCompraRepository.save(
                orden);
    }

    public OrdenCompraDTO obtenerOrdenDTO(
            Integer id) {

        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Orden no encontrada"));

        OrdenCompraDTO dto = new OrdenCompraDTO();

        dto.setFecha(
                orden.getFecha().toString());

        dto.setNombreProv(
                orden.getNombreProv());

        dto.setNitProv(
                orden.getNitProv());

        dto.setCiudadProv(
                orden.getCiudadProv());

        dto.setDireccionProv(
                orden.getDireccionProv());

        dto.setTelefonoProv(
                orden.getTelefonoProv());

        dto.setCorreoProv(
                orden.getCorreoProv());

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

        dto.setNumeroOrden(
                orden.getNumeroOrden());

        dto.setEstado(
                orden.getEstado() != null ? orden.getEstado().name() : null);

        dto.setAprobadoPor(
                orden.getUsuarioAprobacion() != null ?
                        orden.getUsuarioAprobacion().getNombre() + " " + orden.getUsuarioAprobacion().getApellido() : null);

        dto.setFechaAprobacion(
                orden.getFechaAprobacion() != null ?
                        orden.getFechaAprobacion().toString() : null);

        dto.setRecibidoPor(orden.getRecibidoPor() != null ? orden.getRecibidoPor() : null);
        dto.setFechaRecepcion(orden.getFechaRecepcion() != null ? (orden.getFechaRecepcion().toString()) : null);
        dto.setNumeroFactura(orden.getNumeroFactura() != null ? orden.getNumeroFactura() : null);

        dto.setIdCentroCosto(
                orden.getCentroCosto() != null ?
                        orden.getCentroCosto().getIdCentroCosto().longValue() : null);

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
                detalleDTO.setIdProducto(detalle.getProducto().getIdProducto() != null ?
                        detalle.getProducto().getIdProducto().longValue() : null);
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
