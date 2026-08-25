package com.palmera_junior.gestion_compras.service.organizacion;

import java.util.List;

import org.springframework.data.domain.Page;

import com.palmera_junior.gestion_compras.entity.CentroCosto;

/**
 * Contrato de servicio para la gestión de Centros de Costo de la organización.
 */
public interface ICentroCostoService {

    /**
     * Qué hace: Retorna todos los centros de costo registrados en el sistema.
     * A dónde apunta: Consulta tabla `centro_costo`.
     */
    List<CentroCosto> getAllCentroCostos();

    /**
     * Qué hace: Retorna una página de centros de costo según el número de página y tamaño.
     * A dónde apunta: Consulta tabla `centro_costo` con paginación.
     */
    Page<CentroCosto> paginar(int page, int size);

    /**
     * Qué hace: Lista los centros de costo asociados a una sede específica.
     * A dónde apunta: Consulta tabla `centro_costo` filtrando por `id_sede`.
     */
    List<CentroCosto> listarPorSede(Integer idSede);

    /**
     * Qué hace: Busca un centro de costo por su identificador primario.
     * A dónde apunta: Consulta tabla `centro_costo`.
     */
    CentroCosto buscarPorId(Integer id);

    /**
     * Qué hace: Crea o actualiza un centro de costo validando la unicidad de nombre por sede y código.
     * A dónde apunta: Modifica tabla `centro_costo`.
     */
    CentroCosto guardar(Integer idCentroCosto, String nombre, Integer sedeId, String codigo, String direccion);

    /**
     * Qué hace: Elimina un centro de costo por su ID si no está en uso.
     * A dónde apunta: Modifica tabla `centro_costo`.
     */
    boolean eliminar(Integer id);
}

