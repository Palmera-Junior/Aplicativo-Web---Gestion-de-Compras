package com.palmera_junior.gestion_compras.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.entity.DetalleCompra;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.OrdenCompraRepository;
import com.palmera_junior.gestion_compras.repository.ProductoRepository;
import com.palmera_junior.gestion_compras.repository.UsuarioRepository;

import jakarta.persistence.criteria.Order;

@Service
public class OrdenCompraService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private OrdenCompraRepository ordenCompraRepository;

    @Autowired
    private ProductoRepository productoRepository;

    // Método para listar órdenes paginadas en el Dashboard
    public Page<OrdenCompra> ordenesDeCompraPaginadas(Pageable pageable) {
        return ordenCompraRepository.findAll(pageable);
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


        if (dto.getNumeroOrden() != null && !dto.getNumeroOrden().trim().isEmpty()) {
            orden.setNumeroOrden(dto.getNumeroOrden());
        } else {
            // Genera un número único basado en milisegundos (ej: OC-1721592000000)
            orden.setNumeroOrden("OC-" + System.currentTimeMillis());
        }

        // 1. Datos principales y del Proveedor (Snapshot del formulario)
        orden.setNumeroOrden(dto.getNumeroOrden());
        orden.setNitProv(dto.getNitProv());
        orden.setNombreProv(dto.getNombreProv());
        orden.setTelefonoProv(dto.getTelefonoProv());
        orden.setCiudadProv(dto.getCiudadProv());
        orden.setCorreoProv(dto.getCorreoProv());
        orden.setDireccionProv(dto.getDireccionProv());
        orden.setObservaciones(dto.getObservaciones());

        // 2. Totales y fecha
        orden.setFecha(LocalDate.now()); 
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
    


        // 3. Mapeo de los detalles
        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            List<DetalleCompra> detallesEntidad = dto.getDetalles().stream().map(dDto -> {
                DetalleCompra detalle = new DetalleCompra();

            // Si el usuario seleccionó un producto existente que tiene ID, puedes buscarlo opcionalmente:
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

        // 4. Guardar en base de datos
        // Nota: Asegúrate de que en OrdenCompra.java tengas cascade = CascadeType.ALL en tu @OneToMany
        return ordenCompraRepository.save(orden);
    }
} 