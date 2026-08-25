package com.palmera_junior.gestion_compras.service.organizacion;

import java.util.List;

import org.springframework.data.domain.Page;

import com.palmera_junior.gestion_compras.entity.Sede;

/**
 * Contrato de servicio para la administración de Sedes geográficas de la organización.
 */
public interface ISedeService {

    /**
     * Qué hace: Retorna todas las sedes registradas en el sistema.
     * A dónde apunta: Consulta tabla `sede`.
     */
    List<Sede> listarTodos();

    /**
     * Qué hace: Retorna una página de sedes según paginación.
     * A dónde apunta: Consulta tabla `sede` con paginación.
     */
    Page<Sede> paginar(int page, int size);

    /**
     * Qué hace: Crea o actualiza una sede verificando unicidad de nombre y de prefijo de ciudad.
     * A dónde apunta: Modifica tabla `sede`.
     */
    Sede guardar(Integer idSede, String nombre, String prefijoCiudad, String direccion);

    /**
     * Qué hace: Elimina una sede si no tiene dependencias en centros de costo o usuarios.
     * A dónde apunta: Modifica tabla `sede`.
     */
    boolean eliminar(Integer id);
}

