package com.palmera_junior.gestion_compras.service.organizacion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.repository.SedeRepository;

@Service
public class SedeService implements ISedeService {

    private final SedeRepository sedeRepository;

    public SedeService(SedeRepository sedeRepository) {
        this.sedeRepository = sedeRepository;
    }

    public List<Sede> listarTodos() {
        return sedeRepository.findAll();
    }

    public Page<Sede> paginar(int page, int size) {
        return sedeRepository.findAll(PageRequest.of(page, size));
    }

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
