package com.palmera_junior.gestion_compras.rrhh.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import com.palmera_junior.gestion_compras.entity.Rh;
import com.palmera_junior.gestion_compras.entity.TipoDocumentoEmpleado;
import com.palmera_junior.gestion_compras.entity.TipoSangre;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmpleadoRegistroForm {

    private Integer idEmpleado;
    private Long version;
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
    private Integer nivelEducativoId;
    private String profesion;
    private Integer areaId;
    private Integer cargoId;
    private Integer centroCostoId;
    private String subcentro;
    private MultipartFile foto;
    private List<Integer> herramientasSolicitadasIds = new ArrayList<>();
    private List<Integer> tiposAdjuntoRequeridos = new ArrayList<>();

    private Integer tipoContratoId;
    private Integer tipoPagoId;
    private Integer entidadBancariaId;
    private Integer idContratoEmpleado;
    private Long versionContrato;
    private String numeroCuentaBancaria;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaIngreso;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fechaFin;
    private BigDecimal salarioBasico;
    private boolean auxilioTransporteAplica;
    private BigDecimal auxilioTransporteValor;
    private boolean auxilioRodamientoAplica;
    private BigDecimal auxilioRodamientoValor;
    private boolean bonificacionNoSalarialAplica;
    private BigDecimal bonificacionNoSalarialValor;
    private Integer epsId;
    private Integer afpId;
    private Integer arlId;
    private BigDecimal tarifaArlPorcentaje;
    private Integer cajaCompensacionId;
    private boolean condicionesEspecialesAplica;
    private String condicionesEspeciales;
}
