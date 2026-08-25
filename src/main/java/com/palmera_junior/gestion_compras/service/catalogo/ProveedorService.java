package com.palmera_junior.gestion_compras.service.catalogo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.Proveedor;
import com.palmera_junior.gestion_compras.repository.ProveedorRepository;
import com.palmera_junior.gestion_compras.repository.SedeRepository;

/**
 * Implementación del servicio de gestión de Proveedores.
 * Administra la persistencia en {@link ProveedorRepository} y la asignación relacional de {@link SedeRepository}.
 */
@Service
public class ProveedorService implements IProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final SedeRepository sedeRepository;

    /**
     * Constructor para inyección de dependencias de repositorios.
     */
    public ProveedorService(ProveedorRepository proveedorRepository, SedeRepository sedeRepository) {
        this.proveedorRepository = proveedorRepository;
        this.sedeRepository = sedeRepository;
    }

    /**
     * Qué hace: Consulta los proveedores vinculados a una sede ordenados por nombre.
     * A dónde apunta: {@link ProveedorRepository#findBySedesIdSedeOrderByNombreAsc(Integer)}
     */
    @Override
    public List<Proveedor> listarPorSede(Integer idSede) {
        return proveedorRepository.findBySedesIdSedeOrderByNombreAsc(idSede);
    }

    /**
     * Qué hace: Retorna todos los proveedores sin paginación.
     * A dónde apunta: {@link ProveedorRepository#findAll()}
     */
    @Override
    public List<Proveedor> listarTodos() {
        return proveedorRepository.findAll();
    }

    /**
     * Qué hace: Retorna los proveedores paginados.
     * A dónde apunta: {@link ProveedorRepository#findAll(org.springframework.data.domain.Pageable)}
     */
    @Override
    public Page<Proveedor> paginar(int page, int size) {
        return proveedorRepository.findAll(PageRequest.of(page, size));
    }

    /**
     * Qué hace: Alias para listar todos los proveedores.
     * A dónde apunta: {@link ProveedorRepository#findAll()}
     */
    @Override
    public List<Proveedor> getAllProveedores() {
        return proveedorRepository.findAll();
    }

    /**
     * Qué hace: Crea o actualiza un proveedor, validando unicidad de NIT y asignando sedes autorizadas.
     * A dónde apunta: {@link ProveedorRepository#save(Object)} y {@link SedeRepository#findAllById(Iterable)}
     */
    @Override
    @Transactional
    public Proveedor guardar(Integer idProv, String nit, String nombre, String ciudad, String direccion,
            String telefono, String correo, List<Integer> sedeIds) {
        nit = normalizar(nit);
        nombre = normalizar(nombre);
        if (!StringUtils.hasText(nit) || !StringUtils.hasText(nombre)) {
            throw new IllegalArgumentException("El NIT y el nombre del proveedor son obligatorios.");
        }

        if (idProv == null) {
            if (proveedorRepository.findByNitIgnoreCase(nit).isPresent()) {
                throw new IllegalArgumentException("Ya existe un proveedor con ese NIT.");
            }
            Proveedor proveedor = new Proveedor();
            proveedor.setNit(nit);
            proveedor.setNombre(nombre);
            proveedor.setCiudad(ciudad);
            proveedor.setDireccion(direccion);
            proveedor.setTelefono(telefono);
            proveedor.setCorreo(correo);
            if (sedeIds != null) {
                proveedor.getSedes().addAll(sedeRepository.findAllById(sedeIds));
            }
            return proveedorRepository.save(proveedor);
        }

        Proveedor proveedor = proveedorRepository.findById(idProv)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado para actualizar."));
        if (proveedorRepository.findByNitIgnoreCase(nit).filter(p -> !p.getIdProv().equals(idProv)).isPresent()) {
            throw new IllegalArgumentException("Hay otro proveedor con el mismo NIT.");
        }
        proveedor.setNit(nit);
        proveedor.setNombre(nombre);
        proveedor.setCiudad(ciudad);
        proveedor.setDireccion(direccion);
        proveedor.setTelefono(telefono);
        proveedor.setCorreo(correo);
        proveedor.getSedes().clear();
        if (sedeIds != null) {
            proveedor.getSedes().addAll(sedeRepository.findAllById(sedeIds));
        }
        return proveedorRepository.save(proveedor);
    }

    /**
     * Qué hace: Elimina un proveedor por su ID si existe.
     * A dónde apunta: {@link ProveedorRepository#deleteById(Object)}
     */
    @Override
    @Transactional
    public boolean eliminar(Integer id) {
        if (!proveedorRepository.existsById(id)) {
            return false;
        }
        proveedorRepository.deleteById(id);
        return true;
    }


    private String normalizar(String value) {
        return value == null ? null : value.trim();
    }
}

