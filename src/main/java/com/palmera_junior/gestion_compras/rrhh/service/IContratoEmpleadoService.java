package com.palmera_junior.gestion_compras.rrhh.service;

import java.util.List;
import java.util.Optional;

import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.entity.Usuario;

public interface IContratoEmpleadoService {

    List<ContratoEmpleado> listarPorEmpleado(Integer idEmpleado);

    Optional<ContratoEmpleado> buscarPorId(Integer idContratoEmpleado);

    ContratoEmpleado guardar(ContratoEmpleado contrato);

    int sincronizarEstados();

    ContratoEmpleado terminar(Integer idContratoEmpleado, Usuario actor, String motivo);

    ContratoEmpleado anular(Integer idContratoEmpleado, Usuario actor, String motivo);
}
