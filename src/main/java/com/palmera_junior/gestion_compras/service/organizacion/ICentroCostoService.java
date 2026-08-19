package com.palmera_junior.gestion_compras.service.organizacion;

import java.util.List;

import org.springframework.data.domain.Page;

import com.palmera_junior.gestion_compras.entity.CentroCosto;

public interface ICentroCostoService {
    List<CentroCosto> getAllCentroCostos();
    Page<CentroCosto> paginar(int page, int size);
    List<CentroCosto> listarPorSede(Integer idSede);
    CentroCosto buscarPorId(Integer id);
    CentroCosto guardar(Integer idCentroCosto, String nombre, Integer sedeId, String codigo, String direccion);
    boolean eliminar(Integer id);
}
