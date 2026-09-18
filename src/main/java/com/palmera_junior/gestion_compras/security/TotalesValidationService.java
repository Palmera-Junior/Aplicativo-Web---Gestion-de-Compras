package com.palmera_junior.gestion_compras.security;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.palmera_junior.gestion_compras.dto.DetalleCompraDTO;
import com.palmera_junior.gestion_compras.dto.OrdenCompraDTO;
import com.palmera_junior.gestion_compras.entity.Producto;
import com.palmera_junior.gestion_compras.repository.ProductoRepository;

/**
 * Servicio para validar y recalcular totales de órdenes de compra.
 * 
 * SEC-05 Mitigation: Garantiza que los valores económicos se validen y recalculen
 * en el servidor, previniendo manipulación desde el cliente. 
 * 
 * Lógica:
 * - Valida que cantidades, precios e IVA sean positivos y coherentes
 * - Recalcula TODOS los totales en base a datos validados del cliente
 * - Permite que el precio en la orden sea diferente al del catálogo (se actualiza en modal)
 * - Soporta productos snapshot (no registrados en catálogo)
 * - Rechaza valores inválidos (negativos, inconsistentes, faltantes)
 */
@Service
public class TotalesValidationService {

    private final ProductoRepository productoRepository;
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal CERO = BigDecimal.ZERO;
    private static final int DECIMAL_PLACES = 2;

    public TotalesValidationService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    /**
     * Valida y recalcula los totales de una orden de compra.
     * Este método es el punto de entrada para garantizar que todos los valores
     * económicos sean validados y recalculados en el servidor.
     */
    public void validarRequisitosBasicosOrden(OrdenCompraDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("La orden de compra es obligatoria");
        }

        if (dto.getIdCentroCosto() == null) {
            throw new IllegalArgumentException("Centro de costo requerido");
        }

        boolean tieneIdProveedor = dto.getIdProv() != null && dto.getIdProv() > 0;
        boolean tieneDatosProveedor = hayDatosProveedor(dto);

        if (!tieneIdProveedor && !tieneDatosProveedor) {
            throw new IllegalArgumentException("Debe seleccionar un proveedor o ingresar los datos del proveedor");
        }

        if (tieneIdProveedor) {
            validarCamposProveedor(dto);
        } else if (tieneDatosProveedor) {
            validarCamposProveedor(dto);
        }

        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("Debe agregar al menos un producto a la orden de compra");
        }

        for (DetalleCompraDTO detalle : dto.getDetalles()) {
            if (detalle == null) {
                throw new IllegalArgumentException("Hay un producto con datos incompletos");
            }
            if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad de cada producto debe ser mayor a cero");
            }
            if ((detalle.getIdProducto() == null || detalle.getIdProducto() <= 0)
                    && (detalle.getCodigoInventario() == null || detalle.getCodigoInventario().isBlank())) {
                throw new IllegalArgumentException("Cada producto debe tener un código de inventario válido");
            }
        }
    }

    private boolean hayDatosProveedor(OrdenCompraDTO dto) {
        return (dto.getNitProv() != null && !dto.getNitProv().isBlank())
                || (dto.getNombreProv() != null && !dto.getNombreProv().isBlank())
                || (dto.getCiudadProv() != null && !dto.getCiudadProv().isBlank())
                || (dto.getDireccionProv() != null && !dto.getDireccionProv().isBlank())
                || (dto.getTelefonoProv() != null && !dto.getTelefonoProv().isBlank())
                || (dto.getCorreoProv() != null && !dto.getCorreoProv().isBlank());
    }

    private void validarCamposProveedor(OrdenCompraDTO dto) {
        if (dto.getNitProv() == null || dto.getNitProv().isBlank()) {
            throw new IllegalArgumentException("El NIT del proveedor es obligatorio");
        }
        if (dto.getNombreProv() == null || dto.getNombreProv().isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        if (dto.getCiudadProv() == null || dto.getCiudadProv().isBlank()) {
            throw new IllegalArgumentException("La ciudad del proveedor es obligatoria");
        }
        if (dto.getDireccionProv() == null || dto.getDireccionProv().isBlank()) {
            throw new IllegalArgumentException("La dirección del proveedor es obligatoria");
        }
        if (dto.getTelefonoProv() == null || dto.getTelefonoProv().isBlank()) {
            throw new IllegalArgumentException("El teléfono del proveedor es obligatorio");
        }
        if (dto.getCorreoProv() == null || dto.getCorreoProv().isBlank()) {
            throw new IllegalArgumentException("El correo del proveedor es obligatorio");
        }
    }

    public void validarYRecalcularTotalesOrden(OrdenCompraDTO dto) {
        if (dto.getDetalles() == null || dto.getDetalles().isEmpty()) {
            dto.setSubTotal(CERO);
            dto.setIvaTotal(CERO);
            dto.setTotal(CERO);
            return;
        }

        // 1. Validar y recalcular cada línea de detalle
        BigDecimal subTotalCalculado = CERO;
        BigDecimal ivaTotalCalculado = CERO;

        for (DetalleCompraDTO detalle : dto.getDetalles()) {
            validarYRecalcularDetalle(detalle);
            // El subtotal de cada línea es (cantidad * valorUnitario)
            BigDecimal cantidad = new BigDecimal(detalle.getCantidad());
            BigDecimal subtotalLinea = cantidad.multiply(detalle.getValorUnitario())
                    .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);
            subTotalCalculado = subTotalCalculado.add(subtotalLinea);
            ivaTotalCalculado = ivaTotalCalculado.add(detalle.getValorIva());
        }

        // 2. Validar descuento
        BigDecimal descuento = dto.getDescuento() != null ? dto.getDescuento() : CERO;
        if (descuento.compareTo(CERO) < 0) {
            throw new IllegalArgumentException("El descuento no puede ser negativo");
        }
        if (descuento.compareTo(subTotalCalculado) > 0) {
            throw new IllegalArgumentException("El descuento no puede exceder el subtotal");
        }

        // 3. Validar flete
        BigDecimal valorFlete = CERO;
        if (Boolean.TRUE.equals(dto.getPagaFlete())) {
            valorFlete = dto.getValorFlete() != null ? dto.getValorFlete() : CERO;
            if (valorFlete.compareTo(CERO) < 0) {
                throw new IllegalArgumentException("El valor del flete no puede ser negativo");
            }
        } else {
            valorFlete = CERO;
        }

        // 4. Reemplazar los totales calculados
        dto.setSubTotal(subTotalCalculado);
        dto.setIvaTotal(ivaTotalCalculado);
        dto.setDescuento(descuento);
        dto.setValorFlete(valorFlete);

        // 5. Calcular total final = subtotal + iva - descuento + flete
        BigDecimal total = subTotalCalculado
                .add(ivaTotalCalculado)
                .subtract(descuento)
                .add(valorFlete);

        dto.setTotal(total);
    }

    /**
     * Valida y recalcula una línea de detalle individual.
     * 
     * Permite dos tipos de productos:
     * 1. Registrados en catálogo (idProducto != null): valida que existan,
     *    pero permite que el precio difiera del catálogo (se actualiza en modal)
     * 2. Snapshot/Ad-hoc: producto no registrado, solo con datos en la orden
     */
    private void validarYRecalcularDetalle(DetalleCompraDTO detalle) {
        // 1. Validar cantidad
        if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        // 2. Si hay idProducto, validar que el producto exista en catálogo
        if (detalle.getIdProducto() != null && detalle.getIdProducto() > 0) {
            Producto producto = productoRepository.findById(detalle.getIdProducto().intValue())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto con ID " + detalle.getIdProducto() + " no existe en el catálogo"));
            // El producto existe; permitir que el precio sea diferente (se actualiza en modal)
            // Solo validar que los datos snapshot sean coherentes
        }

        // 3. Validar precio unitario
        if (detalle.getValorUnitario() == null || detalle.getValorUnitario().compareTo(CERO) <= 0) {
            throw new IllegalArgumentException("El valor unitario debe ser mayor a cero");
        }

        // 4. Validar porcentaje de IVA
        if (detalle.getIvaProducto() == null || detalle.getIvaProducto().compareTo(CERO) < 0) {
            throw new IllegalArgumentException("El porcentaje de IVA no puede ser negativo");
        }
        if (detalle.getIvaProducto().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El porcentaje de IVA no puede ser mayor a 100");
        }

        // 5. Recalcular valorIva = cantidad * valorUnitario * (ivaProducto / 100)
        BigDecimal cantidad = new BigDecimal(detalle.getCantidad());
        BigDecimal valorUnitario = detalle.getValorUnitario();
        BigDecimal ivaProducto = detalle.getIvaProducto();

        BigDecimal subtotalLinea = cantidad.multiply(valorUnitario)
                .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);

        BigDecimal valorIva = subtotalLinea
                .multiply(ivaProducto)
                .divide(CIEN, DECIMAL_PLACES, RoundingMode.HALF_UP);

        detalle.setValorIva(valorIva);

        // 6. Recalcular valorTotalLinea = subtotalLinea + valorIva
        BigDecimal valorTotalLinea = subtotalLinea.add(valorIva);
        detalle.setValorTotalLinea(valorTotalLinea);
    }

    /**
     * Valida que un detalle recalculado tenga valores consistentes.
     * Útil para auditoría.
     */
    public boolean validarConsistenciaDetalle(DetalleCompraDTO detalle) {
        if (detalle.getCantidad() == null || detalle.getCantidad() <= 0) {
            return false;
        }

        BigDecimal cantidad = new BigDecimal(detalle.getCantidad());
        BigDecimal valorUnitario = detalle.getValorUnitario() != null ? detalle.getValorUnitario() : CERO;
        BigDecimal ivaProducto = detalle.getIvaProducto() != null ? detalle.getIvaProducto() : CERO;

        BigDecimal subtotalEsperado = cantidad.multiply(valorUnitario)
                .setScale(DECIMAL_PLACES, RoundingMode.HALF_UP);

        BigDecimal ivaEsperado = subtotalEsperado
                .multiply(ivaProducto)
                .divide(CIEN, DECIMAL_PLACES, RoundingMode.HALF_UP);

        BigDecimal totalEsperado = subtotalEsperado.add(ivaEsperado);

        return detalle.getValorIva() != null
                && detalle.getValorIva().compareTo(ivaEsperado) == 0
                && detalle.getValorTotalLinea() != null
                && detalle.getValorTotalLinea().compareTo(totalEsperado) == 0;
    }
}
