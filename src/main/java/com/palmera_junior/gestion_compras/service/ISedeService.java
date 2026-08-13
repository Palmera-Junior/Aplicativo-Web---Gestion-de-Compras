package com.palmera_junior.gestion_compras.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.palmera_junior.gestion_compras.entity.Sede;

public interface ISedeService {
    List<Sede> listarTodos();
    Page<Sede> paginar(int page, int size);
    Sede guardar(Integer idSede, String nombre, String prefijoCiudad, String direccion);
    boolean eliminar(Integer id);
}
