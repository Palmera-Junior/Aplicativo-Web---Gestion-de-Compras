package com.palmera_junior.gestion_compras.rrhh.service;

import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoAdjuntoEmpleado;

public interface IEmpleadoAdjuntoService {

    record DocumentoContratoFirmado(String nombreOriginal, byte[] contenido) {
    }

    List<TipoAdjuntoEmpleado> listarTiposActivos();

    List<EmpleadoAdjunto> listarPorEmpleado(Integer idEmpleado);

    List<EmpleadoAdjuntoRequerido> listarRequeridosPorEmpleado(Integer idEmpleado);

    EmpleadoAdjunto guardar(EmpleadoAdjunto adjunto);

    EmpleadoAdjuntoRequerido guardarRequisito(EmpleadoAdjuntoRequerido requisito);

    EmpleadoAdjunto buscarAdjunto(Integer idEmpleado, Long idAdjunto);

    Optional<EmpleadoAdjunto> buscarContratoFirmado(Integer idEmpleado, Integer idContrato);

    Optional<DocumentoContratoFirmado> obtenerDocumentoContratoFirmado(Integer idEmpleado, Integer idContrato);

    EmpleadoAdjunto actualizarPublicacionQr(Integer idEmpleado, Long idAdjunto, boolean publicar);

    EmpleadoAdjunto agregarDocumento(Integer idEmpleado, Integer idTipoAdjunto,
            Integer idContrato, MultipartFile archivo);

    EmpleadoAdjunto reemplazarDocumentoInicial(Integer idEmpleado, Long idAdjunto, MultipartFile archivo);

    void retirarDocumentoInicial(Integer idEmpleado, Long idAdjunto);

    EmpleadoAdjunto guardarContratoFirmado(Empleado empleado, ContratoEmpleado contrato,
            MultipartFile archivo);
}
