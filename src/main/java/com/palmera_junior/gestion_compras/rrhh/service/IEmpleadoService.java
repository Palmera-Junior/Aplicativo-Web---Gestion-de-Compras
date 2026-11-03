package com.palmera_junior.gestion_compras.rrhh.service;

import java.util.List;
import java.util.Optional;

import com.palmera_junior.gestion_compras.entity.Empleado;

public interface IEmpleadoService {

    List<Empleado> listar();

    Optional<Empleado> buscarPorId(Integer idEmpleado);

    Empleado guardar(Empleado empleado);
}
