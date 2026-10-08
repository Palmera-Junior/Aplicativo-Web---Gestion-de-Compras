package com.palmera_junior.gestion_compras.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RenovarPolizaDTO {

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private List<MultipartFile> archivos;
}