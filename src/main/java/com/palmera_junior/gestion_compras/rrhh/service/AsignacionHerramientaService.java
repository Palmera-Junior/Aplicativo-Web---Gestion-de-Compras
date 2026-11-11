package com.palmera_junior.gestion_compras.rrhh.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.AsignacionHerramienta;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoAsignacionHerramienta;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.Herramienta;
import com.palmera_junior.gestion_compras.rrhh.repository.AsignacionHerramientaRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.HerramientaRepository;
import com.palmera_junior.gestion_compras.service.poliza.IPolizaArchivoStorage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AsignacionHerramientaService {

    private final AsignacionHerramientaRepository asignacionRepository;
    private final HerramientaRepository herramientaRepository;
    private final IPolizaArchivoStorage archivoStorage;

    @Transactional(readOnly = true)
    public List<Herramienta> listarCatalogoActivo() {
        return herramientaRepository.findAllByActivaTrueOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public List<AsignacionHerramienta> listarPorEmpleado(Integer idEmpleado) {
        return asignacionRepository.findAllByEmpleado_IdEmpleadoOrderByEntregadaEnDesc(idEmpleado);
    }

    @Transactional(readOnly = true)
    public List<Integer> listarIdsPendientes(Integer idEmpleado) {
        return asignacionRepository
                .findAllByEmpleado_IdEmpleadoAndEstadoOrderByHerramienta_NombreAsc(
                        idEmpleado, EstadoAsignacionHerramienta.PENDIENTE)
                .stream()
                .map(asignacion -> asignacion.getHerramienta().getIdHerramienta())
                .toList();
    }

    @Transactional
    public void sincronizarPendientes(Empleado empleado, List<Integer> idsSolicitados) {
        if (empleado.getEstado() != EstadoEmpleado.REGISTRO_INICIAL) {
            throw new IllegalStateException(
                    "Las herramientas solicitadas solo se pueden cambiar durante el registro inicial");
        }

        Set<Integer> ids = new LinkedHashSet<>();
        if (idsSolicitados != null) {
            for (Integer id : idsSolicitados) {
                if (id == null) {
                    throw new IllegalArgumentException("La herramienta seleccionada no es válida");
                }
                ids.add(id);
            }
        }

        Map<Integer, Herramienta> catalogo = new LinkedHashMap<>();
        herramientaRepository.findAllById(ids).forEach(herramienta ->
                catalogo.put(herramienta.getIdHerramienta(), herramienta));
        if (catalogo.size() != ids.size()
                || catalogo.values().stream().anyMatch(herramienta -> !herramienta.isActiva())) {
            throw new IllegalArgumentException(
                    "Una de las herramientas seleccionadas ya no está disponible");
        }

        List<AsignacionHerramienta> pendientes = asignacionRepository
                .findAllByEmpleado_IdEmpleadoAndEstadoOrderByHerramienta_NombreAsc(
                        empleado.getIdEmpleado(), EstadoAsignacionHerramienta.PENDIENTE);
        Map<Integer, AsignacionHerramienta> pendientesPorHerramienta = new LinkedHashMap<>();
        for (AsignacionHerramienta pendiente : pendientes) {
            pendientesPorHerramienta.put(
                    pendiente.getHerramienta().getIdHerramienta(), pendiente);
            if (!ids.contains(pendiente.getHerramienta().getIdHerramienta())) {
                asignacionRepository.delete(pendiente);
            }
        }

        List<AsignacionHerramienta> nuevas = new ArrayList<>();
        for (Integer id : ids) {
            if (pendientesPorHerramienta.containsKey(id)
                    || asignacionRepository
                            .findByEmpleado_IdEmpleadoAndHerramienta_IdHerramientaAndEstado(
                                    empleado.getIdEmpleado(),
                                    id,
                                    EstadoAsignacionHerramienta.ASIGNADA)
                            .isPresent()) {
                continue;
            }
            AsignacionHerramienta pendiente = new AsignacionHerramienta();
            pendiente.setEmpleado(empleado);
            pendiente.setHerramienta(catalogo.get(id));
            pendiente.setEstado(EstadoAsignacionHerramienta.PENDIENTE);
            nuevas.add(pendiente);
        }
        asignacionRepository.saveAll(nuevas);
    }

    @Transactional
    public AsignacionHerramienta asignar(
            Integer idEmpleado, Long idAsignacion, MultipartFile acta, Usuario actor) {
        AsignacionHerramienta asignacion = asignacionRepository.findById(idAsignacion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La solicitud de herramienta ya no existe"));
        if (!asignacion.getEmpleado().getIdEmpleado().equals(idEmpleado)) {
            throw new IllegalArgumentException(
                    "La solicitud de herramienta no pertenece a este empleado");
        }
        if (asignacion.getEmpleado().getEstado() != EstadoEmpleado.ACTIVO) {
            throw new IllegalStateException(
                    "Solo se pueden entregar herramientas a empleados activos");
        }
        if (asignacion.getEstado() != EstadoAsignacionHerramienta.PENDIENTE) {
            throw new IllegalStateException("La herramienta ya no está pendiente de entrega");
        }
        if (!asignacion.getHerramienta().isActiva()) {
            throw new IllegalStateException(
                    "La herramienta ya no está disponible en el catálogo activo");
        }
        if (actor == null) {
            throw new IllegalArgumentException("No se pudo identificar al responsable de entrega");
        }
        if (asignacionRepository.findByHerramienta_IdHerramientaAndEstado(
                asignacion.getHerramienta().getIdHerramienta(),
                EstadoAsignacionHerramienta.ASIGNADA).isPresent()) {
            throw new IllegalStateException(
                    "Esta herramienta ya está asignada. Registra su devolución antes de entregarla de nuevo.");
        }
        if (acta == null || acta.isEmpty()) {
            throw new IllegalArgumentException("Adjunta el acta firmada para registrar la entrega");
        }
        byte[] contenido = archivoStorage.almacenar(acta);
        if (contenido == null || contenido.length < 5
                || contenido[0] != '%' || contenido[1] != 'P' || contenido[2] != 'D'
                || contenido[3] != 'F' || contenido[4] != '-') {
            throw new IllegalArgumentException("Adjunta el acta firmada para registrar la entrega");
        }
        String nombre = nombreSeguro(acta.getOriginalFilename());
        asignacion.setActaNombre(nombre);
        asignacion.setActaMime("application/pdf");
        asignacion.setActaTamanoBytes((long) contenido.length);
        asignacion.setActaContenido(contenido);
        asignacion.setEntregadaEn(OffsetDateTime.now());
        asignacion.setEntregadaPor(actor);
        asignacion.setEstado(EstadoAsignacionHerramienta.ASIGNADA);
        return asignacionRepository.save(asignacion);
    }

    @Transactional
    public AsignacionHerramienta marcarRecibida(
            Integer idEmpleado, Long idAsignacion, Usuario actor) {
        AsignacionHerramienta asignacion = asignacionRepository.findById(idAsignacion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La asignación de herramienta ya no existe"));
        if (!asignacion.getEmpleado().getIdEmpleado().equals(idEmpleado)) {
            throw new IllegalArgumentException(
                    "La asignación de herramienta no pertenece a este empleado");
        }
        if (asignacion.getEstado() != EstadoAsignacionHerramienta.ASIGNADA) {
            throw new IllegalStateException(
                    "Solo se puede recibir una herramienta que esté asignada");
        }
        if (actor == null) {
            throw new IllegalArgumentException("No se pudo identificar a quien recibe la herramienta");
        }
        asignacion.setEstado(EstadoAsignacionHerramienta.RECIBIDO);
        asignacion.setRecibidaEn(OffsetDateTime.now());
        asignacion.setRecibidaPor(actor);
        return asignacionRepository.save(asignacion);
    }

    @Transactional(readOnly = true)
    public ActaHerramienta obtenerActa(Integer idEmpleado, Long idAsignacion) {
        AsignacionHerramienta asignacion = asignacionRepository.findById(idAsignacion)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La asignación de herramienta ya no existe"));
        if (!asignacion.getEmpleado().getIdEmpleado().equals(idEmpleado)) {
            throw new IllegalArgumentException(
                    "La asignación de herramienta no pertenece a este empleado");
        }
        if (asignacion.getActaContenido() == null) {
            throw new IllegalStateException("Esta herramienta aún no tiene un acta de entrega");
        }
        return new ActaHerramienta(asignacion.getActaNombre(), asignacion.getActaContenido());
    }

    private static String nombreSeguro(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new IllegalArgumentException("El acta debe tener un nombre válido");
        }
        String nombre = nombreOriginal.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
        if (nombre.isEmpty() || nombre.length() > 255) {
            throw new IllegalArgumentException("El nombre del acta no es válido");
        }
        return nombre;
    }

    public record ActaHerramienta(String nombre, byte[] contenido) {
    }
}
