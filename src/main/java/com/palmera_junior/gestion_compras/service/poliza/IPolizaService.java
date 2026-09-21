package com.palmera_junior.gestion_compras.service.poliza;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.palmera_junior.gestion_compras.dto.PolizaDTO;
import com.palmera_junior.gestion_compras.entity.Poliza;
import org.springframework.web.multipart.MultipartFile;

public interface IPolizaService {

    Page<Poliza> polizasPaginadas(Pageable pageable, String search, String fechaDesde,
            String fechaHasta, Integer idSede, boolean esNacional, String estado);

    List<Poliza> listarPolizas();

    Poliza obtenerPorId(Integer idPoliza);

    Poliza guardarDesdeDTO(PolizaDTO dto);

    Poliza actualizarDesdeDTO(Integer idPoliza, PolizaDTO dto);

    Poliza aprobar(Integer idPoliza);

    Poliza activarVigente(Integer idPoliza, String fechaVencimiento, MultipartFile polizaFisica);

    int marcarVencidas();

    Poliza anular(Integer idPoliza);
}