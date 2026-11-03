package com.palmera_junior.gestion_compras.rrhh.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.repository.EmpleadoRepository;

@Service
@Transactional(readOnly = true)
public class EmpleadoService implements IEmpleadoService {

    private final EmpleadoRepository empleadoRepository;

    public EmpleadoService(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    @Override
    public List<Empleado> listar() {
        return empleadoRepository.findAll();
    }

    @Override
    public Optional<Empleado> buscarPorId(Integer idEmpleado) {
        return empleadoRepository.findById(idEmpleado);
    }

    @Override
    @Transactional
    public Empleado guardar(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }
}
