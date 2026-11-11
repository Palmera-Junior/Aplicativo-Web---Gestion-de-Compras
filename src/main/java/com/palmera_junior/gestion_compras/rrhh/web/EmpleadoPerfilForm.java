package com.palmera_junior.gestion_compras.rrhh.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.palmera_junior.gestion_compras.entity.Rh;
import com.palmera_junior.gestion_compras.entity.TipoDocumentoEmpleado;
import com.palmera_junior.gestion_compras.entity.TipoSangre;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class EmpleadoPerfilForm {
    private TipoDocumentoEmpleado tipoDocumento;
    private String numeroDocumento;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaExpedicion;
    private String lugarExpedicion;
    private String primerNombre;
    private String segundoNombre;
    private String primerApellido;
    private String segundoApellido;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaNacimiento;
    private String lugarNacimiento;
    private TipoSangre tipoSangre;
    private Rh rh;
    private String celular;
    private String correoElectronico;
    private String direccionResidencia;
    private String barrio;
    private String municipio;
    private String departamento;
    private String profesion;
    private Integer nivelEducativoId;
    private String motivo;
    private MultipartFile foto;
}
