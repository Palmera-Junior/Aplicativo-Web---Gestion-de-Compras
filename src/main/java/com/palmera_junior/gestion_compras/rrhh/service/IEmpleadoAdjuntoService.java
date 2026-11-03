package com.palmera_junior.gestion_compras.rrhh.service;

import java.util.List;

import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoAdjuntoEmpleado;

public interface IEmpleadoAdjuntoService {

    List<TipoAdjuntoEmpleado> listarTiposActivos();

    List<EmpleadoAdjunto> listarPorEmpleado(Integer idEmpleado);

    List<EmpleadoAdjuntoRequerido> listarRequeridosPorEmpleado(Integer idEmpleado);

    EmpleadoAdjunto guardar(EmpleadoAdjunto adjunto);

    EmpleadoAdjuntoRequerido guardarRequisito(EmpleadoAdjuntoRequerido requisito);
}
