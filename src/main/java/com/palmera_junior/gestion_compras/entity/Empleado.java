package com.palmera_junior.gestion_compras.entity;

import java.time.LocalDate;

import com.palmera_junior.gestion_compras.rrhh.entity.Area;
import com.palmera_junior.gestion_compras.rrhh.entity.Cargo;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.NivelEducativo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
@Table(name = "empleado", schema = "core")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 30)
    private TipoDocumentoEmpleado tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 30)
    private String numeroDocumento;

    @Column(name = "primer_nombre", nullable = false, length = 100)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 100)
    private String segundoNombre;

    @Column(name = "primer_apellido", nullable = false, length = 100)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 100)
    private String segundoApellido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cargo")
    @ToString.Exclude
    private Cargo cargo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_area")
    @ToString.Exclude
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_centro_costo")
    @ToString.Exclude
    private CentroCosto centroCosto;

    @Column(length = 100)
    private String subcentro;

    @Column(name = "fecha_expedicion")
    private LocalDate fechaExpedicion;

    @Column(name = "lugar_expedicion", length = 150)
    private String lugarExpedicion;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "lugar_nacimiento", length = 150)
    private String lugarNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_sangre", length = 2)
    private TipoSangre tipoSangre;

    @Enumerated(EnumType.STRING)
    @Column(name = "rh", length = 10)
    private Rh rh;

    @Column(name = "direccion_residencia", length = 255)
    private String direccionResidencia;

    @Column(length = 100)
    private String barrio;

    @Column(length = 30)
    private String celular;

    @Column(name = "correo_electronico", length = 150)
    private String correoElectronico;

    @Column(length = 100)
    private String municipio;

    @Column(length = 100)
    private String departamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel_educativo")
    @ToString.Exclude
    private NivelEducativo nivelEducativo;

    @Column(length = 150)
    private String profesion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoEmpleado estado = EstadoEmpleado.REGISTRO_INICIAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobado_por")
    @ToString.Exclude
    private Usuario aprobadoPor;

    @Column(name = "aprobado_en")
    private java.time.OffsetDateTime aprobadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "completado_por")
    @ToString.Exclude
    private Usuario completadoPor;

    @Column(name = "completado_en")
    private java.time.OffsetDateTime completadoEn;

    @Version
    @Column(nullable = false)
    private Long version;
}
