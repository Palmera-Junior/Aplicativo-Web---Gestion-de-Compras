package com.palmera_junior.gestion_compras.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class ActivarPolizaDTO {

    private String fechaVencimiento;
    private MultipartFile polizaFisica;
}