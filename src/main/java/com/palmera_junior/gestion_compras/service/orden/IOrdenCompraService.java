package com.palmera_junior.gestion_compras.service.orden;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.dto.RecibirOrdenDTO;
import com.palmera_junior.gestion_compras.entity.OrdenCompra;

public interface IOrdenCompraService {
    Page<OrdenCompra> ordenesDeCompraPaginadas(Pageable pageable, String search, String fechaDesde,
            String fechaHasta, Integer idSede, boolean esNacional, String estado);
    List<OrdenCompra> listarOrdenesCompra();
    OrdenCompra obtenerPorId(Integer idOrden);
    OrdenCompra guardarOrdenDesdeDTO(OrdenCompraDTO dto);
    OrdenCompra actualizarOrdenDesdeDTO(Integer idOrden, OrdenCompraDTO dto);
    OrdenCompra aprobarOrden(Integer idOrden);
    OrdenCompra recibirOrden(Integer idOrden, RecibirOrdenDTO dto);
    OrdenCompra facturarOrden(Integer idOrden, String numeroFactura, String fotoFactura);
    OrdenCompra anularOrden(Integer idOrden);
    OrdenCompraDTO obtenerOrdenDTO(Integer id);

}
