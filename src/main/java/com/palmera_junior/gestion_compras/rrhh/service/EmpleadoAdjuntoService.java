package com.palmera_junior.gestion_compras.rrhh.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRequeridoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoAdjuntoEmpleadoRepository;

@Service
@Transactional(readOnly = true)
public class EmpleadoAdjuntoService implements IEmpleadoAdjuntoService {

    private final EmpleadoAdjuntoRepository adjuntoRepository;
    private final EmpleadoAdjuntoRequeridoRepository requeridoRepository;
    private final TipoAdjuntoEmpleadoRepository tipoAdjuntoRepository;

    public EmpleadoAdjuntoService(
            EmpleadoAdjuntoRepository adjuntoRepository,
            EmpleadoAdjuntoRequeridoRepository requeridoRepository,
            TipoAdjuntoEmpleadoRepository tipoAdjuntoRepository) {
        this.adjuntoRepository = adjuntoRepository;
        this.requeridoRepository = requeridoRepository;
        this.tipoAdjuntoRepository = tipoAdjuntoRepository;
    }

    @Override
    public List<TipoAdjuntoEmpleado> listarTiposActivos() {
        return tipoAdjuntoRepository.findAllByActivoTrueOrderByOrdenAsc();
    }

    @Override
    public List<EmpleadoAdjunto> listarPorEmpleado(Integer idEmpleado) {
        return adjuntoRepository.findAllByEmpleado_IdEmpleadoOrderByFechaCargaDesc(idEmpleado);
    }

    @Override
    public List<EmpleadoAdjuntoRequerido> listarRequeridosPorEmpleado(Integer idEmpleado) {
        return requeridoRepository.findAllByEmpleado_IdEmpleado(idEmpleado);
    }

    @Override
    @Transactional
    public EmpleadoAdjunto guardar(EmpleadoAdjunto adjunto) {
        if (adjunto.getFechaCarga() == null) {
            adjunto.setFechaCarga(OffsetDateTime.now());
        }
        if (adjunto.getContenido() != null && adjunto.getTamanoBytes() == null) {
            adjunto.setTamanoBytes((long) adjunto.getContenido().length);
        }
        return adjuntoRepository.save(adjunto);
    }

    @Override
    @Transactional
    public EmpleadoAdjuntoRequerido guardarRequisito(EmpleadoAdjuntoRequerido requisito) {
        return requeridoRepository.save(requisito);
    }
}
