package com.palmera_junior.gestion_compras.rrhh.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoRepository;

@Service
@Transactional(readOnly = true)
public class ContratoEmpleadoService implements IContratoEmpleadoService {

    private final ContratoEmpleadoRepository contratoRepository;

    public ContratoEmpleadoService(ContratoEmpleadoRepository contratoRepository) {
        this.contratoRepository = contratoRepository;
    }

    @Override
    public List<ContratoEmpleado> listarPorEmpleado(Integer idEmpleado) {
        return contratoRepository.findAllByEmpleado_IdEmpleadoOrderByFechaIngresoDesc(idEmpleado);
    }

    @Override
    public Optional<ContratoEmpleado> buscarPorId(Integer idContratoEmpleado) {
        return contratoRepository.findById(idContratoEmpleado);
    }

    @Override
    @Transactional
    public ContratoEmpleado guardar(ContratoEmpleado contrato) {
        return contratoRepository.save(contrato);
    }
}
