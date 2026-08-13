package com.palmera_junior.gestion_compras.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.palmera_junior.gestion_compras.entity.Proveedor;

public interface IProveedorService {
    List<Proveedor> listarPorSede(Integer idSede);
    List<Proveedor> listarTodos();
    Page<Proveedor> paginar(int page, int size);
    List<Proveedor> getAllProveedores();
    Proveedor guardar(Integer idProv, String nit, String nombre, String ciudad, String direccion,
            String telefono, String correo, java.util.List<Integer> sedeIds);
    boolean eliminar(Integer id);
}
