package com.palmera_junior.gestion_compras.rrhh.service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Permiso;
import com.palmera_junior.gestion_compras.entity.TipoDocumentoEmpleado;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.web.EmpleadoPerfilForm;
import com.palmera_junior.gestion_compras.repository.EmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.NivelEducativoRepository;

@Service
public class EmpleadoPerfilService {

    private final EmpleadoRepository empleadoRepository;
    private final JdbcTemplate jdbcTemplate;
    private final EmpleadoFotoService empleadoFotoService;
    private final NivelEducativoRepository nivelEducativoRepository;

    public EmpleadoPerfilService(EmpleadoRepository empleadoRepository, JdbcTemplate jdbcTemplate,
            EmpleadoFotoService empleadoFotoService, NivelEducativoRepository nivelEducativoRepository) {
        this.empleadoRepository = empleadoRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.empleadoFotoService = empleadoFotoService;
        this.nivelEducativoRepository = nivelEducativoRepository;
    }

    @Transactional(readOnly = true)
    public Optional<String> obtenerNombreFoto(Integer idEmpleado) {
        return empleadoFotoService.obtenerNombre(idEmpleado);
    }

    @Transactional
    public Empleado actualizar(Integer idEmpleado, EmpleadoPerfilForm form, Usuario actor) {
        exigirPermiso(actor);
        if (form == null || form.getMotivo() == null || form.getMotivo().isBlank()) {
            throw new IllegalArgumentException("Indica el motivo del cambio de perfil");
        }
        Empleado empleado = empleadoRepository.findByIdForUpdate(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("No existe el empleado solicitado"));
        if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            throw new IllegalStateException("Solo se puede editar el perfil de un empleado activo");
        }
        validarIdentificacion(form, empleado.getIdEmpleado());
        String correo = limpiar(form.getCorreoElectronico());
        ValidacionDatosEmpleado.validarCorreo(correo);
        if (correo != null && empleadoRepository
                .existsByCorreoElectronicoNormalizadoExcludingEmpleado(correo, idEmpleado)) {
            throw new IllegalArgumentException("El correo ya está asociado a otro empleado");
        }
        Map<String, String[]> cambios = new LinkedHashMap<>();
        cambiar(cambios, "tipo_documento", empleado.getTipoDocumento().name(),
                form.getTipoDocumento().name(),
                valor -> empleado.setTipoDocumento(TipoDocumentoEmpleado.valueOf(valor)));
        cambiar(cambios, "numero_documento", empleado.getNumeroDocumento(),
                limpiar(form.getNumeroDocumento()), empleado::setNumeroDocumento);
        cambiar(cambios, "fecha_expedicion", texto(empleado.getFechaExpedicion()),
                texto(form.getFechaExpedicion()), valor -> empleado.setFechaExpedicion(fecha(valor)));
        cambiar(cambios, "lugar_expedicion", empleado.getLugarExpedicion(),
                limpiar(form.getLugarExpedicion()), empleado::setLugarExpedicion);
        cambiar(cambios, "primer_nombre", empleado.getPrimerNombre(),
                limpiar(form.getPrimerNombre()), empleado::setPrimerNombre);
        cambiar(cambios, "segundo_nombre", empleado.getSegundoNombre(),
                limpiar(form.getSegundoNombre()), empleado::setSegundoNombre);
        cambiar(cambios, "primer_apellido", empleado.getPrimerApellido(),
                limpiar(form.getPrimerApellido()), empleado::setPrimerApellido);
        cambiar(cambios, "segundo_apellido", empleado.getSegundoApellido(),
                limpiar(form.getSegundoApellido()), empleado::setSegundoApellido);
        cambiar(cambios, "fecha_nacimiento", texto(empleado.getFechaNacimiento()),
                texto(form.getFechaNacimiento()), valor -> empleado.setFechaNacimiento(fecha(valor)));
        cambiar(cambios, "lugar_nacimiento", empleado.getLugarNacimiento(),
                limpiar(form.getLugarNacimiento()), empleado::setLugarNacimiento);
        cambiar(cambios, "tipo_sangre", nombre(empleado.getTipoSangre()),
                nombre(form.getTipoSangre()), valor -> empleado.setTipoSangre(valor == null ? null
                        : com.palmera_junior.gestion_compras.entity.TipoSangre.valueOf(valor)));
        cambiar(cambios, "rh", nombre(empleado.getRh()), nombre(form.getRh()),
                valor -> empleado.setRh(valor == null ? null
                        : com.palmera_junior.gestion_compras.entity.Rh.valueOf(valor)));
        cambiar(cambios, "celular", empleado.getCelular(), limpiar(form.getCelular()), empleado::setCelular);
        cambiar(cambios, "correo_electronico", empleado.getCorreoElectronico(), correo,
                empleado::setCorreoElectronico);
        cambiar(cambios, "direccion_residencia", empleado.getDireccionResidencia(),
                limpiar(form.getDireccionResidencia()), empleado::setDireccionResidencia);
        cambiar(cambios, "barrio", empleado.getBarrio(), limpiar(form.getBarrio()), empleado::setBarrio);
        cambiar(cambios, "municipio", empleado.getMunicipio(), limpiar(form.getMunicipio()), empleado::setMunicipio);
        cambiar(cambios, "departamento", empleado.getDepartamento(), limpiar(form.getDepartamento()),
                empleado::setDepartamento);
        cambiar(cambios, "profesion", empleado.getProfesion(), limpiar(form.getProfesion()), empleado::setProfesion);
        Integer nivelActual = empleado.getNivelEducativo() == null
                ? null : empleado.getNivelEducativo().getId();
        if (!java.util.Objects.equals(nivelActual, form.getNivelEducativoId())) {
            var nivel = form.getNivelEducativoId() == null ? null
                    : nivelEducativoRepository.findByIdAndActivoTrue(form.getNivelEducativoId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Selecciona un nivel educativo válido"));
            cambios.put("nivel_educativo", new String[] {
                    empleado.getNivelEducativo() == null ? null : empleado.getNivelEducativo().getNombre(),
                    nivel == null ? null : nivel.getNombre()
            });
            empleado.setNivelEducativo(nivel);
        }
        empleadoRepository.save(empleado);
        for (Map.Entry<String, String[]> cambio : cambios.entrySet()) {
            jdbcTemplate.update("""
                    INSERT INTO rrhh.empleado_perfil_historial
                        (id_empleado, id_usuario, campo, valor_anterior, valor_nuevo, motivo)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, idEmpleado, actor.getIdUsuario(), cambio.getKey(), cambio.getValue()[0],
                    cambio.getValue()[1], form.getMotivo().trim());
        }
        empleadoFotoService.guardar(empleado, form.getFoto());
        return empleado;
    }

    private static void cambiar(Map<String, String[]> cambios, String campo, String anterior,
            String nuevo, java.util.function.Consumer<String> setter) {
        if (!java.util.Objects.equals(anterior, nuevo)) {
            cambios.put(campo, new String[] { anterior, nuevo });
            setter.accept(nuevo);
        }
    }

    private static String limpiar(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    private void validarIdentificacion(EmpleadoPerfilForm form, Integer idEmpleado) {
        if (form.getTipoDocumento() == null || form.getNumeroDocumento() == null
                || form.getNumeroDocumento().isBlank()) {
            throw new IllegalArgumentException("El tipo y número de documento son obligatorios");
        }
        String documento = form.getNumeroDocumento().trim();
        if (documento.length() > 30 || documento.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("El número de documento no tiene un formato válido");
        }
        ValidacionDatosEmpleado.validarSoloDigitos(documento, "El número de documento");
        ValidacionDatosEmpleado.validarSoloDigitos(form.getCelular(), "El celular");
        ValidacionDatosEmpleado.validarFechasIdentificacion(
                form.getFechaExpedicion(), form.getFechaNacimiento());
        validarLongitud(form.getPrimerNombre(), 100, "El primer nombre");
        validarLongitud(form.getSegundoNombre(), 100, "El segundo nombre");
        validarLongitud(form.getPrimerApellido(), 100, "El primer apellido");
        validarLongitud(form.getSegundoApellido(), 100, "El segundo apellido");
        validarLongitud(form.getLugarExpedicion(), 150, "El lugar de expedición");
        validarLongitud(form.getLugarNacimiento(), 150, "El lugar de nacimiento");
        validarLongitud(form.getCelular(), 30, "El celular");
        validarLongitud(form.getCorreoElectronico(), 150, "El correo electrónico");
        validarLongitud(form.getDireccionResidencia(), 255, "La dirección");
        validarLongitud(form.getBarrio(), 100, "El barrio");
        validarLongitud(form.getMunicipio(), 100, "El municipio");
        validarLongitud(form.getDepartamento(), 100, "El departamento");
        validarLongitud(form.getProfesion(), 150, "La profesión");
        empleadoRepository.findByTipoDocumentoAndNumeroDocumento(form.getTipoDocumento(), documento)
                .filter(otro -> !otro.getIdEmpleado().equals(idEmpleado))
                .ifPresent(otro -> {
                    throw new IllegalArgumentException("El tipo y número de documento ya están registrados");
                });
        if (form.getPrimerNombre() == null || form.getPrimerNombre().isBlank()
                || form.getPrimerApellido() == null || form.getPrimerApellido().isBlank()) {
            throw new IllegalArgumentException("El primer nombre y el primer apellido son obligatorios");
        }
    }

    private static void validarLongitud(String valor, int maximo, String campo) {
        if (valor != null && valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " supera la longitud permitida");
        }
    }

    private static String texto(LocalDate valor) {
        return valor == null ? null : valor.toString();
    }

    private static LocalDate fecha(String valor) {
        return valor == null ? null : LocalDate.parse(valor);
    }

    private static String nombre(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }

    private static void exigirPermiso(Usuario actor) {
        Set<String> permisos = actor == null || actor.getPermisos() == null ? Set.of()
                : actor.getPermisos().stream().map(Permiso::getCodigo)
                        .collect(java.util.stream.Collectors.toSet());
        if (!permisos.contains("RRHH_PERFIL_EDITAR")) {
            throw new SecurityException("No tiene permiso para editar el perfil del empleado");
        }
    }
}
