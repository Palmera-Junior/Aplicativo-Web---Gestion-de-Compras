package com.palmera_junior.gestion_compras.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
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
    public Page<OrdenCompra> ordenesDeCompraPaginadas(Pageable pageable) {
        return ordenCompraRepository.findAllByOrderByIdOrdenDesc(pageable);
    }

    // Método para obtener todas las órdenes
    public List<OrdenCompra> getAllOrdenes() {
        return ordenCompraRepository.findAll();
    }

    // Método para buscar una orden por ID
    public OrdenCompra obtenerPorId(Integer idOrden) {
        return ordenCompraRepository.findById(idOrden).orElse(null);
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
        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
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
        OrdenCompra ordenGuardada = ordenCompraRepository.save(orden);

        return asignarNumeroOrden(ordenGuardada);
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

        return ordenCompraRepository.save(ordenCompra);
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

}
