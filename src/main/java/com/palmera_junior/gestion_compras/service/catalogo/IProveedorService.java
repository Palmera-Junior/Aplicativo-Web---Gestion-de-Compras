package com.palmera_junior.gestion_compras.service.catalogo;

import java.util.List;

import org.springframework.data.domain.Page;

import com.palmera_junior.gestion_compras.entity.Proveedor;

/**
 * Contrato de servicio para la administración y consulta de Proveedores.
 */
public interface IProveedorService {

    /**
     * Qué hace:
     * Retorna los proveedores asociados a una sede organizacional ordenados alfabéticamente.
     * 
     * A dónde apunta:
     * - Consulta tabla: proveedor, proveedor_sede (mediante ProveedorRepository)
     * 
     * @param idSede Identificador de la sede.
     * @return Lista de proveedores habilitados para la sede.
     */
    List<Proveedor> listarPorSede(Integer idSede);

    /**
     * Qué hace:
     * Busca proveedores asociados a una sede según un término de coincidencia parcial.
     *
     * A dónde apunta:
     * - Consulta tabla: proveedor, proveedor_sede
     *
     * @param idSede Identificador de la sede donde se limita la búsqueda.
     * @param termino Texto a buscar en nombre o NIT.
     * @return Lista de proveedores coincidentes en la sede.
     */
    List<Proveedor> buscarPorTerminoEnSede(Integer idSede, String termino);

    /**
     * Qué hace:
     * Realiza una búsqueda nacional de proveedores por coincidencia parcial en nombre o NIT,
     * sin limitarla a una sede concreta.
     *
     * A dónde apunta:
     * - Consulta tabla: proveedor
     *
     * @param termino Texto a buscar en nombre o NIT.
     * @return Lista de proveedores coincidentes a nivel nacional.
     */
    List<Proveedor> buscarPorTerminoNacional(String termino);

    /**
     * Qué hace:
     * Retorna el listado completo de proveedores registrados en el sistema.
     * 
     * A dónde apunta:
     * - Consulta tabla: proveedor
     * 
     * @return Lista de todos los proveedores.
     */
    List<Proveedor> listarTodos();

    /**
     * Qué hace:
     * Retorna una página de proveedores según los parámetros de paginación.
     * 
     * A dónde apunta:
     * - Consulta tabla: proveedor (paginación en ProveedorRepository)
     * 
     * @param page Índice de página.
     * @param size Cantidad de registros por página.
     * @return Página de proveedores.
     */
    Page<Proveedor> paginar(int page, int size);

    /**
     * Qué hace:
     * Retorna todos los proveedores (alias para compatibilidad con servicios del dashboard).
     * 
     * A dónde apunta:
     * - Consulta tabla: proveedor
     * 
     * @return Lista de proveedores.
     */
    List<Proveedor> getAllProveedores();

    /**
     * Qué hace:
     * Crea un nuevo proveedor o actualiza uno existente y sincroniza sus sedes asignadas.
     * 
     * A dónde apunta:
     * - Modifica tablas: proveedor, proveedor_sede (mediante ProveedorRepository y SedeRepository)
     * 
     * @param idProv ID del proveedor (null si es nuevo).
     * @param nit Número de Identificación Tributaria.
     * @param nombre Nombre / Razón social.
     * @param ciudad Ciudad del proveedor.
     * @param direccion Dirección física.
     * @param telefono Teléfono de contacto.
     * @param correo Correo electrónico.
     * @param sedeIds Lista de IDs de sedes a vincular.
     * @return Proveedor persistido.
     */
    Proveedor guardar(Integer idProv, Long nit, String nombre, String ciudad, String direccion,
            String telefono, String correo, java.util.List<Integer> sedeIds);

    /**
     * Qué hace:
     * Elimina un proveedor del sistema por su ID si no tiene registros dependientes.
     * 
     * A dónde apunta:
     * - Elimina en tabla: proveedor
     * 
     * @param id Identificador numérico del proveedor.
     * @return true si fue eliminado; false si no existía.
     */
    boolean eliminar(Integer id);
}

