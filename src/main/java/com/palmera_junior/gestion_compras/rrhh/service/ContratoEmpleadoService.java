package com.palmera_junior.gestion_compras.rrhh.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.math.BigDecimal;
import org.springframework.beans.BeanUtils;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Permiso;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleadoHistorialEstado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.AreaRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.CargoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoHistorialEstadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoContratoRepository;

@Service
@Transactional(readOnly = true)
public class ContratoEmpleadoService implements IContratoEmpleadoService {

    private final ContratoEmpleadoRepository contratoRepository;
    private final ContratoEmpleadoHistorialEstadoRepository historialRepository;
    private final IEmpleadoAdjuntoService adjuntoService;
    private final TipoContratoRepository tipoContratoRepository;
    private final CargoRepository cargoRepository;
    private final AreaRepository areaRepository;
    private final Clock rrhhClock;

    public ContratoEmpleadoService(
            ContratoEmpleadoRepository contratoRepository,
            ContratoEmpleadoHistorialEstadoRepository historialRepository,
            IEmpleadoAdjuntoService adjuntoService,
            TipoContratoRepository tipoContratoRepository,
            CargoRepository cargoRepository,
            AreaRepository areaRepository,
            Clock rrhhClock) {
        this.contratoRepository = contratoRepository;
        this.historialRepository = historialRepository;
        this.adjuntoService = adjuntoService;
        this.tipoContratoRepository = tipoContratoRepository;
        this.cargoRepository = cargoRepository;
        this.areaRepository = areaRepository;
        this.rrhhClock = rrhhClock;
    }

    @Override
    public List<ContratoEmpleado> listarPorEmpleado(Integer idEmpleado) {
        return contratoRepository.findAllByEmpleado_IdEmpleadoOrderByFechaIngresoDesc(idEmpleado);
    }

    @Override
    public Optional<ContratoEmpleado> buscarPorId(Integer idContratoEmpleado) {
        return contratoRepository.findById(idContratoEmpleado);
    }

    @Override
    @Transactional
    public ContratoEmpleado guardar(ContratoEmpleado contrato) {
        if (contrato.getIdContratoEmpleado() == null) {
            contrato.setEstado(EstadoContratoEmpleado.BORRADOR);
            return contratoRepository.save(contrato);
        }
        ContratoEmpleado existente = buscarObligatorio(contrato.getIdContratoEmpleado());
        contrato.setEmpleado(existente.getEmpleado());
        contrato.setEstado(existente.getEstado());
        contrato.setTerminadoPor(existente.getTerminadoPor());
        contrato.setTerminadoEn(existente.getTerminadoEn());
        contrato.setMotivoTerminacion(existente.getMotivoTerminacion());
        contrato.setAnuladoPor(existente.getAnuladoPor());
        contrato.setAnuladoEn(existente.getAnuladoEn());
        contrato.setMotivoAnulacion(existente.getMotivoAnulacion());
        contrato.setVersion(existente.getVersion());
        return contratoRepository.save(contrato);
    }

    @Override
    @Transactional
    public int sincronizarEstados() {
        LocalDate hoy = LocalDate.now(rrhhClock);
        List<ContratoEmpleado> contratos = contratoRepository.findAllByEstadoIn(List.of(
                EstadoContratoEmpleado.PROGRAMADO,
                EstadoContratoEmpleado.VIGENTE,
                EstadoContratoEmpleado.VENCIDO));
        List<ContratoEmpleado> modificados = new ArrayList<>();
        int actualizados = 0;
        OffsetDateTime ahora = OffsetDateTime.now(rrhhClock);
        for (ContratoEmpleado contrato : contratos) {
            EstadoContratoEmpleado estadoAnterior = contrato.getEstado();
            if (contrato.sincronizarEstado(hoy)) {
                actualizados++;
                modificados.add(contrato);
                registrarCambioEstado(
                        contrato, estadoAnterior, null, ahora, "Sincronización automática por fechas");
            }
        }
        if (actualizados > 0) {
            contratoRepository.saveAll(modificados);
        }
        return actualizados;
    }

    @Override
    @Transactional
    public ContratoEmpleado terminar(Integer idContratoEmpleado, Usuario actor, String motivo) {
        exigirPermiso(actor, "RRHH_CONTRATO_TERMINAR");
        ContratoEmpleado contrato = buscarObligatorio(idContratoEmpleado);
        EstadoContratoEmpleado estadoAnterior = contrato.getEstado();
        contrato.terminar(actor, LocalDate.now(rrhhClock), motivo);
        ContratoEmpleado guardado = contratoRepository.save(contrato);
        registrarCambioEstado(
                guardado, estadoAnterior, actor, OffsetDateTime.now(rrhhClock), "Terminación manual");
        return guardado;
    }

    @Override
    @Transactional
    public ContratoEmpleado anular(Integer idContratoEmpleado, Usuario actor, String motivo) {
        exigirPermiso(actor, "RRHH_CONTRATO_ANULAR");
        ContratoEmpleado contrato = buscarObligatorio(idContratoEmpleado);
        EstadoContratoEmpleado estadoAnterior = contrato.getEstado();
        contrato.anular(actor, LocalDate.now(rrhhClock), motivo);
        ContratoEmpleado guardado = contratoRepository.save(contrato);
        registrarCambioEstado(
                guardado, estadoAnterior, actor, OffsetDateTime.now(rrhhClock), "Anulación administrativa");
        return guardado;
    }

    @Override
    @Transactional
    public ContratoEmpleado renovar(Integer idContratoEmpleado, LocalDate inicio, LocalDate fin,
            BigDecimal salario, Integer idTipoContrato, Integer idCargo, Integer idArea,
            MultipartFile contratoFirmado, Usuario actor) {
        exigirPermiso(actor, "RRHH_GESTIONAR");
        ContratoEmpleado anterior = buscarObligatorio(idContratoEmpleado);
        if (anterior.getEmpleado().getEstado() != EstadoEmpleado.ACTIVO) {
            throw new IllegalStateException("Solo se puede renovar el contrato de un empleado activo");
        }
        if (inicio == null || salario == null || salario.signum() < 0
                || idTipoContrato == null || idCargo == null || idArea == null
                || salario.scale() > 2 || salario.precision() - salario.scale() > 8
                || fin != null && !fin.isAfter(inicio)) {
            throw new IllegalArgumentException(
                    "Selecciona tipo de contrato, cargo y área; revisa las fechas y el salario");
        }
        var tipoContrato = tipoContratoRepository.findByIdAndActivoTrue(idTipoContrato)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un tipo de contrato activo"));
        var cargo = cargoRepository.findByIdAndActivoTrue(idCargo)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un cargo activo"));
        var area = areaRepository.findByIdAndActivoTrue(idArea)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un área activa"));
        if (anterior.getEstado() == EstadoContratoEmpleado.BORRADOR
                || anterior.getEstado() == EstadoContratoEmpleado.ANULADO) {
            throw new IllegalStateException("Solo se puede renovar un contrato formalizado");
        }
        if (anterior.getFechaFin() != null && !inicio.isAfter(anterior.getFechaFin())
                || anterior.getFechaFin() == null
                        && anterior.getEstado() == EstadoContratoEmpleado.VIGENTE) {
            throw new IllegalStateException(
                    "La renovación debe iniciar después de terminar el contrato vigente");
        }
        List<ContratoEmpleado> historial = listarPorEmpleado(anterior.getEmpleado().getIdEmpleado());
        boolean intervaloOcupado = historial.stream()
                .filter(c -> !c.getIdContratoEmpleado().equals(idContratoEmpleado))
                .filter(c -> c.getEstado() == EstadoContratoEmpleado.PROGRAMADO
                        || c.getEstado() == EstadoContratoEmpleado.VIGENTE)
                .anyMatch(c -> !inicio.isAfter(c.getFechaFin() == null
                        ? LocalDate.MAX : c.getFechaFin())
                        && (fin == null || !fin.isBefore(c.getFechaIngreso())));
        if (intervaloOcupado) {
            throw new IllegalStateException("Las fechas se cruzan con otro contrato programado o vigente");
        }
        ContratoEmpleado nuevo = new ContratoEmpleado();
        BeanUtils.copyProperties(anterior, nuevo, "idContratoEmpleado", "estado", "terminadoPor",
                "terminadoEn", "motivoTerminacion", "anuladoPor", "anuladoEn", "motivoAnulacion", "version");
        nuevo.setFechaIngreso(inicio);
        nuevo.setFechaFin(fin);
        nuevo.setSalarioBasico(salario);
        nuevo.setTipoContrato(tipoContrato);
        nuevo.setCargo(cargo);
        nuevo.setArea(area);
        nuevo.setEstado(EstadoContratoEmpleado.calcular(inicio, fin, LocalDate.now(rrhhClock)));
        ContratoEmpleado guardado = contratoRepository.saveAndFlush(nuevo);
        adjuntoService.guardarContratoFirmado(guardado.getEmpleado(), guardado, contratoFirmado);
        registrarCambioEstado(guardado, EstadoContratoEmpleado.BORRADOR, actor, OffsetDateTime.now(rrhhClock),
                "Renovación del contrato #" + idContratoEmpleado);
        return guardado;
    }

    private ContratoEmpleado buscarObligatorio(Integer idContratoEmpleado) {
        return contratoRepository.findById(idContratoEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("No existe el contrato indicado"));
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

    private void registrarCambioEstado(
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
        historialRepository.save(historial);
    }
}
