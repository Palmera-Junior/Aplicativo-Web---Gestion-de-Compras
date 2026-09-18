package com.palmera_junior.gestion_compras.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class PolizaDTO {

    private Integer idPoliza;
    private String fechaCreacion;
    private Long idProveedor;
    private String cliente;
    private String descripcion;
    private String numeroContrato;
    private BigDecimal valorPrima;
    private BigDecimal valorContrato;
    /** Archivo original del contrato, recibido desde el formulario multipart. */
    private MultipartFile contratoAdjunto;
    private boolean eliminarContratoAdjunto;
    private String estado;
}
