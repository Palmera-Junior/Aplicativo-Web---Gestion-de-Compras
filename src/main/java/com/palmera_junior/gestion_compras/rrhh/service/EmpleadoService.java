package com.palmera_junior.gestion_compras.rrhh.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Permiso;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleadoHistorialEstado;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoHistorialEstado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoHistorialEstadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoHistorialEstadoRepository;
import com.palmera_junior.gestion_compras.repository.EmpleadoRepository;

@Service
@Transactional(readOnly = true)
public class EmpleadoService implements IEmpleadoService {

    private final EmpleadoRepository empleadoRepository;
    private final EmpleadoHistorialEstadoRepository historialRepository;
    private final ContratoEmpleadoRepository contratoRepository;
    private final ContratoEmpleadoHistorialEstadoRepository historialContratoRepository;
    private final EmpleadoCompletitudService completitudService;
    private final Clock rrhhClock;

    public EmpleadoService(
            EmpleadoRepository empleadoRepository,
            EmpleadoHistorialEstadoRepository historialRepository,
            ContratoEmpleadoRepository contratoRepository,
            ContratoEmpleadoHistorialEstadoRepository historialContratoRepository,
            EmpleadoCompletitudService completitudService,
            Clock rrhhClock) {
        this.empleadoRepository = empleadoRepository;
        this.historialRepository = historialRepository;
        this.contratoRepository = contratoRepository;
        this.historialContratoRepository = historialContratoRepository;
        this.completitudService = completitudService;
        this.rrhhClock = rrhhClock;
    }

    @Override
    public List<Empleado> listar() {
        return empleadoRepository.findAllByOrderByPrimerApellidoAscPrimerNombreAsc();
    }

    @Override
    public Optional<Empleado> buscarPorId(Integer idEmpleado) {
        return empleadoRepository.findById(idEmpleado);
    }

    @Override
    public List<EmpleadoHistorialEstado> listarHistorialEstado(Integer idEmpleado) {
        return historialRepository
                .findAllByEmpleado_IdEmpleadoOrderByFechaCambioDescIdEmpleadoHistorialEstadoDesc(
                        idEmpleado);
    }

    @Override
    @Transactional
    public Empleado crearRegistroInicial(Empleado empleado, Usuario actor) {
        exigirPermiso(actor, "RRHH_GESTIONAR");
        if (empleado == null || empleado.getIdEmpleado() != null) {
            throw new IllegalArgumentException("Se requieren datos nuevos de empleado");
        }
        empleado.setEstado(EstadoEmpleado.REGISTRO_INICIAL);
        Empleado guardado = empleadoRepository.save(empleado);
        registrarCambioEstado(
                guardado, null, actor, OffsetDateTime.now(rrhhClock), "Creación de registro inicial");
        return guardado;
    }

    @Override
    @Transactional
    public Empleado editarRegistroInicial(Integer idEmpleado, Empleado cambios, Usuario actor) {
        exigirPermiso(actor, "RRHH_REGISTRO_EDITAR");
        Empleado existente = buscarObligatorio(idEmpleado);
        if (existente.getEstado() != EstadoEmpleado.REGISTRO_INICIAL) {
            throw new IllegalStateException("Solo se puede editar un empleado en Registro inicial");
        }
        if (cambios == null || cambios.getIdEmpleado() != null
                && !idEmpleado.equals(cambios.getIdEmpleado())) {
            throw new IllegalArgumentException("Los datos no corresponden al empleado indicado");
        }
        cambios.setIdEmpleado(idEmpleado);
        cambios.setEstado(existente.getEstado());
        cambios.setAprobadoPor(existente.getAprobadoPor());
        cambios.setAprobadoEn(existente.getAprobadoEn());
        cambios.setCompletadoPor(existente.getCompletadoPor());
        cambios.setCompletadoEn(existente.getCompletadoEn());
        cambios.setVersion(existente.getVersion());
        return empleadoRepository.save(cambios);
    }

    @Override
    @Transactional
    public Empleado aprobarRegistroInicial(Integer idEmpleado, Usuario aprobador) {
        exigirPermiso(aprobador, "RRHH_APROBAR");
        Empleado empleado = buscarObligatorio(idEmpleado);
        if (empleado.getEstado() != EstadoEmpleado.REGISTRO_INICIAL) {
            throw new IllegalStateException("Solo se puede aprobar un empleado en Registro inicial");
        }

        List<String> faltantes = completitudService.faltantesRegistroInicial(empleado);
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "No se puede aprobar el registro. Faltan: " + String.join(", ", faltantes));
        }

        EstadoEmpleado estadoAnterior = empleado.getEstado();
        OffsetDateTime ahora = OffsetDateTime.now(rrhhClock);
        empleado.setEstado(EstadoEmpleado.PROCESO_CONTRATACION);
        empleado.setAprobadoPor(aprobador);
        empleado.setAprobadoEn(ahora);
        empleadoRepository.save(empleado);
        registrarCambioEstado(empleado, estadoAnterior, aprobador, ahora, "Aprobación de registro inicial");
        return empleado;
    }

    @Override
    @Transactional
    public Empleado completarRegistro(
            Integer idEmpleado, ContratoEmpleado contrato, Usuario encargado) {
        exigirPermiso(encargado, "RRHH_GESTIONAR");
        Empleado empleado = buscarObligatorio(idEmpleado);
        if (empleado.getEstado() != EstadoEmpleado.PROCESO_CONTRATACION) {
            throw new IllegalStateException("Solo se puede completar un empleado en proceso de contratación");
        }
        if (contrato == null) {
            throw new IllegalArgumentException("La información del contrato es obligatoria");
        }
        if (contrato.getEmpleado() != null
                && !empleado.getIdEmpleado().equals(contrato.getEmpleado().getIdEmpleado())) {
            throw new IllegalArgumentException("El contrato no pertenece al empleado indicado");
        }
        contrato.setEmpleado(empleado);

        List<String> faltantes = completitudService.faltantesContratacion(empleado, contrato);
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "No se puede completar el registro. Faltan: " + String.join(", ", faltantes));
        }

        LocalDate hoy = LocalDate.now(rrhhClock);
        EstadoContratoEmpleado estadoContratoAnterior = contrato.getEstado();
        contrato.setEstado(EstadoContratoEmpleado.calcular(
                contrato.getFechaIngreso(), contrato.getFechaFin(), hoy));
        ContratoEmpleado contratoGuardado = contratoRepository.save(contrato);
        registrarCambioEstadoContrato(
                contratoGuardado, estadoContratoAnterior, encargado, OffsetDateTime.now(rrhhClock),
                "Contrato inicial creado al completar el registro");

        EstadoEmpleado estadoAnterior = empleado.getEstado();
        OffsetDateTime ahora = OffsetDateTime.now(rrhhClock);
        empleado.setEstado(EstadoEmpleado.ACTIVO);
        empleado.setCompletadoPor(encargado);
        empleado.setCompletadoEn(ahora);
        empleadoRepository.save(empleado);
        registrarCambioEstado(empleado, estadoAnterior, encargado, ahora, "Registro de contratación completado");
        return empleado;
    }

    public List<String> faltantesRegistroInicial(Integer idEmpleado) {
        return completitudService.faltantesRegistroInicial(buscarObligatorio(idEmpleado));
    }

    public List<String> faltantesContratacion(Integer idEmpleado, ContratoEmpleado contrato) {
        return completitudService.faltantesContratacion(
                buscarObligatorio(idEmpleado), contrato);
    }

    private Empleado buscarObligatorio(Integer idEmpleado) {
        return empleadoRepository.findById(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("No existe el empleado indicado"));
    }

    private void registrarCambioEstado(
            Empleado empleado,
            EstadoEmpleado estadoAnterior,
            Usuario actor,
            OffsetDateTime fechaCambio,
            String detalle) {
        EmpleadoHistorialEstado historial = new EmpleadoHistorialEstado();
        historial.setEmpleado(empleado);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(empleado.getEstado());
        historial.setUsuario(actor);
        historial.setFechaCambio(fechaCambio);
        historial.setDetalle(detalle);
        historialRepository.save(historial);
    }

    private void registrarCambioEstadoContrato(
            ContratoEmpleado contrato,
            EstadoContratoEmpleado estadoAnterior,
            Usuario actor,
            OffsetDateTime fechaCambio,
            String detalle) {
        ContratoEmpleadoHistorialEstado historial = new ContratoEmpleadoHistorialEstado();
        historial.setContrato(contrato);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(contrato.getEstado());
        historial.setUsuario(actor);
        historial.setFechaCambio(fechaCambio);
        historial.setDetalle(detalle);
        historialContratoRepository.save(historial);
    }

    private static void exigirPermiso(Usuario actor, String codigoPermiso) {
        Set<String> permisos = actor == null || actor.getPermisos() == null
                ? Set.of()
                : actor.getPermisos().stream()
                        .map(Permiso::getCodigo)
                        .collect(java.util.stream.Collectors.toSet());
        if (!permisos.contains(codigoPermiso)) {
            throw new SecurityException("No tiene permiso para esta operación");
        }
    }
}
