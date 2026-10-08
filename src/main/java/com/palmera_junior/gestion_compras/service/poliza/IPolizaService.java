package com.palmera_junior.gestion_compras.service.poliza;

import java.util.List;
import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.palmera_junior.gestion_compras.dto.PolizaDTO;
import com.palmera_junior.gestion_compras.dto.RenovarPolizaDTO;
import com.palmera_junior.gestion_compras.entity.Poliza;
import com.palmera_junior.gestion_compras.entity.PolizaAdjunto;
import com.palmera_junior.gestion_compras.entity.PolizaRenovacion;
import com.palmera_junior.gestion_compras.entity.PolizaSedeAprobacion;
import org.springframework.web.multipart.MultipartFile;

public interface IPolizaService {

    Page<Poliza> polizasPaginadas(Pageable pageable, String search, String fechaDesde,
            String fechaHasta, Integer idSede, boolean esNacional, String estado);

    List<Poliza> listarPolizas();

    Poliza obtenerPorId(Integer idPoliza);

    List<PolizaSedeAprobacion> listarAprobacionesPorSede(Integer idPoliza);

    Poliza guardarDesdeDTO(PolizaDTO dto);

    Poliza actualizarDesdeDTO(Integer idPoliza, PolizaDTO dto);

    Poliza aprobar(Integer idPoliza);

    Poliza aprobarPorSede(Integer idPoliza, Integer idSede);

    Poliza activarVigente(Integer idPoliza, String numeroPoliza, List<MultipartFile> polizasFisicas);

    Poliza renovar(Integer idPoliza, RenovarPolizaDTO dto);

    List<PolizaRenovacion> listarRenovaciones(Integer idPoliza);

    PolizaAdjunto obtenerAdjuntoRenovacion(Integer idPoliza, Integer idRenovacion, Integer idAdjunto);

    byte[] leerAdjuntoRenovacion(PolizaAdjunto adjunto) throws IOException;

    PolizaAdjunto obtenerContratoAdjunto(Integer idPoliza);

    byte[] leerContratoAdjunto(PolizaAdjunto adjunto) throws IOException;

    PolizaAdjunto obtenerAdjuntoFisico(Integer idPoliza, Integer idAdjunto);

    int marcarVencidas();

    Poliza anular(Integer idPoliza);

    Poliza terminar(Integer idPoliza, String motivoTerminacion);
}