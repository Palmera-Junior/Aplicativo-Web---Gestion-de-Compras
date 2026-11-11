package com.palmera_junior.gestion_compras.rrhh.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.EtapaAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRequeridoRepository;

@Service
@Transactional(readOnly = true)
public class EmpleadoCompletitudService {

    private final EmpleadoAdjuntoRepository adjuntoRepository;
    private final EmpleadoAdjuntoRequeridoRepository requeridoRepository;

    public EmpleadoCompletitudService(
            EmpleadoAdjuntoRepository adjuntoRepository,
            EmpleadoAdjuntoRequeridoRepository requeridoRepository) {
        this.adjuntoRepository = adjuntoRepository;
        this.requeridoRepository = requeridoRepository;
    }

    public List<String> faltantesRegistroInicial(Empleado empleado) {
        List<String> faltantes = new ArrayList<>();
        if (empleado.getTipoDocumento() == null) {
            faltantes.add("Tipo de documento");
        }
        agregarSiVacio(faltantes, "Número de documento", empleado.getNumeroDocumento());
        agregarSiVacio(faltantes, "Primer nombre", empleado.getPrimerNombre());
        agregarSiVacio(faltantes, "Primer apellido", empleado.getPrimerApellido());
        if (empleado.getArea() == null) {
            faltantes.add("Área");
        }
        if (empleado.getCargo() == null) {
            faltantes.add("Cargo");
        }
        if (empleado.getCentroCosto() == null) {
            faltantes.add("Centro de costo");
        }
        agregarAdjuntosFaltantes(empleado, EtapaAdjuntoEmpleado.REGISTRO_INICIAL, faltantes);
        return List.copyOf(faltantes);
    }

    public List<String> faltantesContratacion(Empleado empleado, ContratoEmpleado contrato) {
        List<String> faltantes = new ArrayList<>();
        if (contrato == null) {
            faltantes.add("Contrato");
            agregarAdjuntosFaltantes(empleado, EtapaAdjuntoEmpleado.CONTRATACION, faltantes);
            return List.copyOf(faltantes);
        }
        if (contrato.getFechaIngreso() == null) {
            faltantes.add("Fecha de ingreso");
        }
        if (contrato.getFechaFin() != null
                && contrato.getFechaIngreso() != null
                && !contrato.getFechaFin().isAfter(contrato.getFechaIngreso())) {
            faltantes.add("La fecha de finalización debe ser posterior a la fecha de ingreso");
        }
        if (contrato.getTipoContrato() == null) {
            faltantes.add("Tipo de contrato");
        }
        if (contrato.getTipoPago() == null) {
            faltantes.add("Tipo de pago");
        }
        if (contrato.getSalarioBasico() == null || contrato.getSalarioBasico().signum() < 0) {
            faltantes.add("Salario básico válido");
        }
        if (contrato.getEps() == null) {
            faltantes.add("EPS");
        }
        if (contrato.getAfp() == null) {
            faltantes.add("AFP");
        }
        if (contrato.getArl() == null) {
            faltantes.add("ARL");
        }
        if (contrato.getCajaCompensacion() == null) {
            faltantes.add("Caja de compensación");
        }
        agregarAdjuntosFaltantes(empleado, EtapaAdjuntoEmpleado.CONTRATACION, faltantes);
        return List.copyOf(faltantes);
    }

    private void agregarAdjuntosFaltantes(
            Empleado empleado, EtapaAdjuntoEmpleado etapa, List<String> faltantes) {
        List<EmpleadoAdjuntoRequerido> requeridos =
                requeridoRepository.findAllByEmpleado_IdEmpleadoAndEtapaRequerida(
                        empleado.getIdEmpleado(), etapa);
        if (requeridos.isEmpty()) {
            return;
        }

        Set<Integer> adjuntosVigentes = new HashSet<>();
        for (EmpleadoAdjunto adjunto : adjuntoRepository
                .findAllByEmpleado_IdEmpleadoOrderByFechaCargaDesc(empleado.getIdEmpleado())) {
            if (adjunto.isVigente() && adjunto.getTipoAdjunto() != null) {
                adjuntosVigentes.add(adjunto.getTipoAdjunto().getIdTipoAdjuntoEmpleado());
            }
        }

        for (EmpleadoAdjuntoRequerido requerido : requeridos) {
            Integer idTipo = requerido.getTipoAdjunto().getIdTipoAdjuntoEmpleado();
            if (!adjuntosVigentes.contains(idTipo)) {
                faltantes.add("Adjunto requerido: " + requerido.getTipoAdjunto().getDescripcion());
            }
        }
    }

    private static void agregarSiVacio(List<String> faltantes, String etiqueta, String valor) {
        if (valor == null || valor.isBlank()) {
            faltantes.add(etiqueta);
        }
    }
}
