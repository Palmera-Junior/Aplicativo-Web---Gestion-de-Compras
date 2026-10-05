package com.palmera_junior.gestion_compras.dto;

/**
 * DTO ligero para completar sugerencias y búsquedas de proveedores en formularios y autocompletados.
 * Contiene la información mínima necesaria para mostrar un proveedor sin cargar la entidad completa.
 */
public record ProveedorAutocompleteDTO(
        Integer idProv,
        Long nit,
        String nombre,
        String ciudad,
        String direccion,
        String telefono,
        String correo) {
}