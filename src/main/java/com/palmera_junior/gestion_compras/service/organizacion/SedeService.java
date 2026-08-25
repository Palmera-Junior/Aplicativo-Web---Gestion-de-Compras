package com.palmera_junior.gestion_compras.service.organizacion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.repository.SedeRepository;

/**
 * Implementación del servicio de gestión de Sedes geográficas.
 * Controla la persistencia en {@link SedeRepository} y garantiza la unicidad de prefijos de ciudad.
 */
@Service
public class SedeService implements ISedeService {

    private final SedeRepository sedeRepository;

    /**
     * Constructor para inyección del repositorio de sedes.
     */
    public SedeService(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    /**
     * Qué hace: Retorna la lista total de sedes registradas.
     * A dónde apunta: {@link SedeRepository#findAll()} -> tabla sede
     */
    @Override
    public List<Sede> listarTodos() {
        return sedeRepository.findAll();
    }

    /**
     * Qué hace: Retorna una página de sedes.
     * A dónde apunta: {@link SedeRepository#findAll(org.springframework.data.domain.Pageable)} -> tabla sede
     */
    @Override
    public Page<Sede> paginar(int page, int size) {
        return sedeRepository.findAll(PageRequest.of(page, size));
    }

    /**
     * Qué hace: Crea o actualiza una sede validando que el nombre y el prefijo de ciudad sean únicos.
     * A dónde apunta: {@link SedeRepository#save(Object)} -> tabla sede
     */
    @Override
    @Transactional
    public Sede guardar(Integer idSede, String nombre, String prefijoCiudad, String direccion) {
        nombre = normalizar(nombre);
        prefijoCiudad = normalizar(prefijoCiudad);
        if (!StringUtils.hasText(nombre) || !StringUtils.hasText(prefijoCiudad)) {
            throw new IllegalArgumentException("El nombre y el prefijo de la sede son obligatorios.");
        }

        if (idSede == null) {
            if (sedeRepository.existsByNombreIgnoreCase(nombre)) {
                throw new IllegalArgumentException("Ya existe una sede con ese nombre.");
            }
            if (sedeRepository.existsByPrefijoCiudadIgnoreCase(prefijoCiudad)) {
                throw new IllegalArgumentException("Ya existe una sede con ese prefijo de ciudad.");
            }
            Sede sede = new Sede();
            sede.setNombre(nombre);
            sede.setPrefijoCiudad(prefijoCiudad);
            sede.setDireccion(direccion);
            return sedeRepository.save(sede);
        }

        Sede existing = sedeRepository.findById(idSede)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada para actualizar."));
        boolean duplicateName = sedeRepository.existsByNombreIgnoreCase(nombre)
                && !existing.getNombre().equalsIgnoreCase(nombre);
        boolean duplicatePrefijo = sedeRepository.existsByPrefijoCiudadIgnoreCase(prefijoCiudad)
                && !existing.getPrefijoCiudad().equalsIgnoreCase(prefijoCiudad);
        if (duplicateName) {
            throw new IllegalArgumentException("Ya existe una sede con ese nombre.");
        }
        if (duplicatePrefijo) {
            throw new IllegalArgumentException("Ya existe una sede con ese prefijo de ciudad.");
        }
        existing.setNombre(nombre);
        existing.setPrefijoCiudad(prefijoCiudad);
        existing.setDireccion(direccion);
        return sedeRepository.save(existing);
    }

    /**
     * Qué hace: Elimina una sede si existe por su ID.
     * A dónde apunta: {@link SedeRepository#deleteById(Object)} -> tabla sede
     */
    @Override
    @Transactional
    public boolean eliminar(Integer id) {
        if (!sedeRepository.existsById(id)) {
            return false;
        }
        sedeRepository.deleteById(id);
        return true;
    }


    private String normalizar(String value) {
        return value == null ? null : value.trim();
    }
}
