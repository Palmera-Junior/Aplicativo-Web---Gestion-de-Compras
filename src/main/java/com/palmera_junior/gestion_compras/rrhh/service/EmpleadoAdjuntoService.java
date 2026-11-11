package com.palmera_junior.gestion_compras.rrhh.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.repository.EmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.GrupoAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.ContratoEmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRequeridoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoAdjuntoEmpleadoRepository;
import com.palmera_junior.gestion_compras.service.poliza.IPolizaArchivoStorage;

@Service
@Transactional(readOnly = true)
public class EmpleadoAdjuntoService implements IEmpleadoAdjuntoService {

    private final EmpleadoAdjuntoRepository adjuntoRepository;
    private final EmpleadoAdjuntoRequeridoRepository requeridoRepository;
    private final TipoAdjuntoEmpleadoRepository tipoAdjuntoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final ContratoEmpleadoRepository contratoRepository;
    private final IPolizaArchivoStorage archivoStorage;

    @Autowired
    public EmpleadoAdjuntoService(
            EmpleadoAdjuntoRepository adjuntoRepository,
            EmpleadoAdjuntoRequeridoRepository requeridoRepository,
            TipoAdjuntoEmpleadoRepository tipoAdjuntoRepository,
            EmpleadoRepository empleadoRepository,
            ContratoEmpleadoRepository contratoRepository,
            IPolizaArchivoStorage archivoStorage) {
        this.adjuntoRepository = adjuntoRepository;
        this.requeridoRepository = requeridoRepository;
        this.tipoAdjuntoRepository = tipoAdjuntoRepository;
        this.empleadoRepository = empleadoRepository;
        this.contratoRepository = contratoRepository;
        this.archivoStorage = archivoStorage;
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

    @Override
    public EmpleadoAdjunto buscarAdjunto(Integer idEmpleado, Long idAdjunto) {
        EmpleadoAdjunto adjunto = adjuntoRepository.findById(idAdjunto)
                .orElseThrow(() -> new IllegalArgumentException("No existe el documento solicitado"));
        if (!adjunto.getEmpleado().getIdEmpleado().equals(idEmpleado)) {
            throw new IllegalArgumentException("No existe el documento solicitado");
        }
        return adjunto;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<EmpleadoAdjunto> buscarContratoFirmado(Integer idEmpleado, Integer idContrato) {
        return adjuntoRepository
                .findFirstByEmpleado_IdEmpleadoAndContrato_IdContratoEmpleadoAndTipoAdjunto_CodigoOrderByFechaCargaDesc(
                        idEmpleado, idContrato, "3.8");
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<DocumentoContratoFirmado> obtenerDocumentoContratoFirmado(
            Integer idEmpleado, Integer idContrato) {
        return adjuntoRepository
                .findFirstByEmpleado_IdEmpleadoAndContrato_IdContratoEmpleadoAndTipoAdjunto_CodigoOrderByFechaCargaDesc(
                        idEmpleado, idContrato, "3.8")
                .map(adjunto -> new DocumentoContratoFirmado(
                        adjunto.getNombreOriginal(), adjunto.getContenido()));
    }

    @Override
    @Transactional
    public EmpleadoAdjunto actualizarPublicacionQr(Integer idEmpleado, Long idAdjunto, boolean publicar) {
        EmpleadoAdjunto adjunto = buscarAdjunto(idEmpleado, idAdjunto);
    if (!"3.1".equals(adjunto.getTipoAdjunto().getCodigo())) {
            throw new IllegalArgumentException("Solo el certificado de ARL se puede publicar en la tarjeta QR");
        }
        if (!"application/pdf".equalsIgnoreCase(adjunto.getTipoMime())) {
            throw new IllegalArgumentException("El certificado ARL debe estar en formato PDF");
        }
        adjunto.setPublicoQr(publicar);
        return adjuntoRepository.save(adjunto);
    }

    @Override
    @Transactional
    public EmpleadoAdjunto agregarDocumento(Integer idEmpleado, Integer idTipoAdjunto,
            Integer idContrato, MultipartFile archivo) {
        Empleado empleado = empleadoRepository.findByIdForUpdate(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("No existe el empleado solicitado"));
        if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            throw new IllegalStateException("Solo se pueden agregar documentos a empleados activos");
        }
        TipoAdjuntoEmpleado tipo = tipoAdjuntoRepository.findById(idTipoAdjunto)
                .filter(TipoAdjuntoEmpleado::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("Selecciona una categoría de documento válida"));
        if ("3.8".equals(tipo.getCodigo())) {
            throw new IllegalArgumentException(
                    "El contrato firmado debe adjuntarse desde el flujo de renovación");
        }
        ContratoEmpleado contrato = buscarContratoEmpleado(idEmpleado, idContrato);
        return guardarPdf(empleado, tipo, contrato, archivo, false);
    }

    @Override
    @Transactional
    public EmpleadoAdjunto reemplazarDocumentoInicial(
            Integer idEmpleado, Long idAdjunto, MultipartFile archivo) {
        Empleado empleado = obtenerEmpleadoEditable(idEmpleado);
        EmpleadoAdjunto anterior = obtenerDocumentoInicialVigente(idEmpleado, idAdjunto);
        byte[] contenido = archivoStorage.almacenar(archivo);
        if (contenido == null) {
            throw new IllegalArgumentException("Adjunta un archivo PDF");
        }

        anterior.setVigente(false);
        anterior.setPublicoQr(false);
        adjuntoRepository.saveAndFlush(anterior);
        return guardarPdf(empleado, anterior.getTipoAdjunto(), anterior.getContrato(),
                archivo, false, contenido);
    }

    @Override
    @Transactional
    public void retirarDocumentoInicial(Integer idEmpleado, Long idAdjunto) {
        obtenerEmpleadoEditable(idEmpleado);
        EmpleadoAdjunto adjunto = obtenerDocumentoInicialVigente(idEmpleado, idAdjunto);
        adjunto.setVigente(false);
        adjunto.setPublicoQr(false);
        adjuntoRepository.save(adjunto);
    }

    @Override
    @Transactional
    public EmpleadoAdjunto guardarContratoFirmado(Empleado empleado, ContratoEmpleado contrato,
            MultipartFile archivo) {
        TipoAdjuntoEmpleado tipo = tipoAdjuntoRepository.findByCodigoAndActivoTrue("3.8")
                .orElseThrow(() -> new IllegalStateException(
                        "No está configurado el tipo de adjunto para contratos de trabajo"));
        return guardarPdf(empleado, tipo, contrato, archivo, true);
    }

    private EmpleadoAdjunto guardarPdf(Empleado empleado, TipoAdjuntoEmpleado tipo,
            ContratoEmpleado contrato, MultipartFile archivo, boolean conservarVersiones) {
        byte[] contenido = archivoStorage.almacenar(archivo);
        return guardarPdf(empleado, tipo, contrato, archivo, conservarVersiones, contenido);
    }

    private EmpleadoAdjunto guardarPdf(Empleado empleado, TipoAdjuntoEmpleado tipo,
            ContratoEmpleado contrato, MultipartFile archivo, boolean conservarVersiones, byte[] contenido) {
        if (contenido == null) {
            throw new IllegalArgumentException("Adjunta un archivo PDF");
        }
        if (!conservarVersiones && !tipo.isPermiteMultiplesVigentes()) {
            List<EmpleadoAdjunto> anteriores = adjuntoRepository
                    .findAllByEmpleado_IdEmpleadoAndTipoAdjunto_IdTipoAdjuntoEmpleadoAndVigenteTrueOrderByFechaCargaDesc(
                            empleado.getIdEmpleado(), tipo.getIdTipoAdjuntoEmpleado());
            anteriores.forEach(adjunto -> adjunto.setVigente(false));
            adjuntoRepository.saveAll(anteriores);
        }
        String nombre = archivo.getOriginalFilename() == null ? "documento.pdf"
                : archivo.getOriginalFilename().replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
        EmpleadoAdjunto adjunto = new EmpleadoAdjunto();
        adjunto.setEmpleado(empleado);
        adjunto.setTipoAdjunto(tipo);
        adjunto.setContrato(contrato);
        adjunto.setNombreOriginal(nombre.isBlank() ? "documento.pdf"
                : nombre.substring(0, Math.min(255, nombre.length())));
        adjunto.setTipoMime("application/pdf");
        adjunto.setTamanoBytes((long) contenido.length);
        adjunto.setContenido(contenido);
        adjunto.setFechaCarga(OffsetDateTime.now());
        adjunto.setVigente(true);
        adjunto.setPublicoQr(false);
        return adjuntoRepository.save(adjunto);
    }

    private Empleado obtenerEmpleadoEditable(Integer idEmpleado) {
        Empleado empleado = empleadoRepository.findByIdForUpdate(idEmpleado)
                .orElseThrow(() -> new IllegalArgumentException("No existe el empleado solicitado"));
        if (empleado.getEstado() == EstadoEmpleado.RETIRADO) {
            throw new IllegalStateException("No se pueden modificar documentos de un empleado retirado");
        }
        return empleado;
    }

    private EmpleadoAdjunto obtenerDocumentoInicialVigente(Integer idEmpleado, Long idAdjunto) {
        EmpleadoAdjunto adjunto = buscarAdjunto(idEmpleado, idAdjunto);
        GrupoAdjuntoEmpleado grupo = adjunto.getTipoAdjunto().getGrupo();
        if (grupo != GrupoAdjuntoEmpleado.INGRESO && grupo != GrupoAdjuntoEmpleado.VEHICULO) {
            throw new IllegalArgumentException(
                    "Solo se pueden modificar documentos del registro inicial");
        }
        if (!adjunto.isVigente()) {
            throw new IllegalStateException("El documento ya no está vigente");
        }
        return adjunto;
    }

    private ContratoEmpleado buscarContratoEmpleado(Integer idEmpleado, Integer idContrato) {
        if (idContrato == null) {
            return null;
        }
        ContratoEmpleado contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new IllegalArgumentException("El contrato seleccionado no existe"));
        if (!contrato.getEmpleado().getIdEmpleado().equals(idEmpleado)) {
            throw new IllegalArgumentException("El contrato seleccionado no corresponde al empleado");
        }
        return contrato;
    }
}
