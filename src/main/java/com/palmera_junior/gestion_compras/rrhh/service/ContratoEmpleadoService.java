package com.palmera_junior.gestion_compras.rrhh.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Permiso;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleadoHistorialEstado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoHistorialEstadoRepository;

@Service
@Transactional(readOnly = true)
public class ContratoEmpleadoService implements IContratoEmpleadoService {

    private final ContratoEmpleadoRepository contratoRepository;
    private final ContratoEmpleadoHistorialEstadoRepository historialRepository;
    private final Clock rrhhClock;

    public ContratoEmpleadoService(
            ContratoEmpleadoRepository contratoRepository,
            ContratoEmpleadoHistorialEstadoRepository historialRepository,
            Clock rrhhClock) {
        this.contratoRepository = contratoRepository;
        this.historialRepository = historialRepository;
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
