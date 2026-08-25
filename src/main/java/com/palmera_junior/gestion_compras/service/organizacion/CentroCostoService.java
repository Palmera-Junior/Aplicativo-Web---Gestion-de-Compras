package com.palmera_junior.gestion_compras.service.organizacion;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.entity.Sede;
import com.palmera_junior.gestion_compras.repository.CentroCostoRepository;
import com.palmera_junior.gestion_compras.repository.SedeRepository;

/**
 * Implementación del servicio de Centros de Costo.
 * Administra la persistencia en {@link CentroCostoRepository} y la integridad referencial con {@link SedeRepository}.
 */
@Service
public class CentroCostoService implements ICentroCostoService {

   private final CentroCostoRepository centroCostoRepository;
   private final SedeRepository sedeRepository;

   /**
    * Constructor con inyección de repositorios.
    */
   public CentroCostoService(CentroCostoRepository centroCostoRepository, SedeRepository sedeRepository) {
       this.centroCostoRepository = centroCostoRepository;
       this.sedeRepository = sedeRepository;
   }

   /**
    * Qué hace: Retorna todos los centros de costo del sistema.
    * A dónde apunta: {@link CentroCostoRepository#findAll()}
    */
   @Override
   public List<CentroCosto> getAllCentroCostos() {
       return centroCostoRepository.findAll();
   }

   /**
    * Qué hace: Retorna una página de centros de costo.
    * A dónde apunta: {@link CentroCostoRepository#findAll(org.springframework.data.domain.Pageable)}
    */
   @Override
   public Page<CentroCosto> paginar(int page, int size) {
       return centroCostoRepository.findAll(PageRequest.of(page, size));
   }

   /**
    * Qué hace: Retorna los centros de costo asociados a una sede ordenados alfabéticamente.
    * A dónde apunta: {@link CentroCostoRepository#findBySedeIdSedeOrderByNombreAsc(Integer)}
    */
   @Override
   public List<CentroCosto> listarPorSede(Integer idSede) {
       return centroCostoRepository.findBySedeIdSedeOrderByNombreAsc(idSede);
   }

   /**
    * Qué hace: Busca un centro de costo por su ID.
    * A dónde apunta: {@link CentroCostoRepository#findById(Object)}
    */
   @Override
   public CentroCosto buscarPorId(Integer id) {
       return centroCostoRepository.findById(id).orElse(null);
   }

   /**
    * Qué hace: Crea o actualiza un centro de costo asegurando unicidad de nombre en la sede y código global.
    * A dónde apunta: {@link CentroCostoRepository#save(Object)}
    */
   @Override
   @Transactional
   public CentroCosto guardar(Integer idCentroCosto, String nombre, Integer sedeId, String codigo, String direccion) {
       nombre = normalizar(nombre);
       codigo = normalizar(codigo);
       direccion = normalizar(direccion);
       if (!StringUtils.hasText(nombre) || sedeId == null) {
           throw new IllegalArgumentException("El nombre del centro de costo y la sede son obligatorios.");
       }

       Sede sede = sedeRepository.findById(sedeId)
               .orElseThrow(() -> new IllegalArgumentException("La sede seleccionada no es válida."));

       if (idCentroCosto == null) {
           if (centroCostoRepository.existsByNombreIgnoreCaseAndSedeIdSede(nombre, sedeId)) {
               throw new IllegalArgumentException("Ya existe un centro de costo con ese nombre en la sede seleccionada.");
           }
           if (StringUtils.hasText(codigo) && centroCostoRepository.existsByCodigoIgnoreCase(codigo)) {
               throw new IllegalArgumentException("Ya existe un centro de costo con el código '" + codigo + "'.");
           }
           CentroCosto centroCosto = new CentroCosto();
           centroCosto.setNombre(nombre);
           centroCosto.setSede(sede);
           centroCosto.setCodigo(codigo);
           centroCosto.setDireccion(direccion);
           return centroCostoRepository.save(centroCosto);
       }

       CentroCosto existing = centroCostoRepository.findById(idCentroCosto)
               .orElseThrow(() -> new IllegalArgumentException("Centro de costo no encontrado para actualizar."));
       if (centroCostoRepository.existsByNombreIgnoreCaseAndSedeIdSede(nombre, sedeId)
               && !(existing.getNombre().equalsIgnoreCase(nombre) && existing.getSede().getIdSede().equals(sedeId))) {
           throw new IllegalArgumentException("Ya existe otro centro de costo con ese nombre en la sede seleccionada.");
       }
       if (StringUtils.hasText(codigo) && centroCostoRepository.existsByCodigoIgnoreCase(codigo)
               && !(existing.getCodigo() != null && existing.getCodigo().equalsIgnoreCase(codigo))) {
           throw new IllegalArgumentException("Ya existe otro centro de costo con el código '" + codigo + "'.");
       }
       existing.setNombre(nombre);
       existing.setSede(sede);
       existing.setCodigo(codigo);
       existing.setDireccion(direccion);
       return centroCostoRepository.save(existing);
   }

   /**
    * Qué hace: Elimina un centro de costo por ID si existe.
    * A dónde apunta: {@link CentroCostoRepository#deleteById(Object)}
    */
   @Override
   @Transactional
   public boolean eliminar(Integer id) {
       if (!centroCostoRepository.existsById(id)) {
           return false;
       }
       centroCostoRepository.deleteById(id);
       return true;
   }


   private String normalizar(String value) {
       return value == null ? null : value.trim();
   }
}
