package com.palmera_junior.gestion_compras.rrhh.service;

import java.util.List;
import java.util.Optional;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoHistorialEstado;

public interface IEmpleadoService {

    List<Empleado> listar();

    Optional<Empleado> buscarPorId(Integer idEmpleado);

    List<EmpleadoHistorialEstado> listarHistorialEstado(Integer idEmpleado);

    Empleado crearRegistroInicial(Empleado empleado, Usuario actor);

    Empleado editarRegistroInicial(Integer idEmpleado, Empleado cambios, Usuario actor);

    Empleado aprobarRegistroInicial(Integer idEmpleado, Usuario aprobador);

    Empleado completarRegistro(Integer idEmpleado, ContratoEmpleado contrato, Usuario encargado);

    List<String> faltantesRegistroInicial(Integer idEmpleado);

    List<String> faltantesContratacion(Integer idEmpleado, ContratoEmpleado contrato);
}
