package com.palmera_junior.gestion_compras.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.palmera_junior.gestion_compras.entity.CentroCosto;
import com.palmera_junior.gestion_compras.repository.CentroCostoRepository;

@Service
public class CentroCostoService {

    @Autowired
    private CentroCostoRepository centroCostoRepository;

    public List<CentroCosto> getAllCentroCostos() {
        return centroCostoRepository.findAll();
    }

    public List<CentroCosto> listarPorSede(Integer idSede) {
        return centroCostoRepository.findBySedeIdSedeOrderByNombreAsc(idSede);
    }

    public CentroCosto buscarPorId(Integer id) {
        return centroCostoRepository.findById(id).orElse(null);
    }
}

