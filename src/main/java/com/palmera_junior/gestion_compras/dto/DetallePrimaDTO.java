package com.palmera_junior.gestion_compras.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DetallePrimaDTO {

    private BigDecimal valor;
    private String descripcion;
}