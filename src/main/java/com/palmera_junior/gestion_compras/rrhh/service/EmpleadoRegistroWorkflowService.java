package com.palmera_junior.gestion_compras.rrhh.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import com.palmera_junior.gestion_compras.entity.Empleado;
import com.palmera_junior.gestion_compras.entity.Usuario;
import com.palmera_junior.gestion_compras.repository.CentroCostoRepository;
import com.palmera_junior.gestion_compras.repository.EmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjunto;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequerido;
import com.palmera_junior.gestion_compras.rrhh.entity.EmpleadoAdjuntoRequeridoId;
import com.palmera_junior.gestion_compras.rrhh.entity.EtapaAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.ContratoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.CatalogoRrhh;
import com.palmera_junior.gestion_compras.rrhh.entity.TipoAdjuntoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.entity.EstadoEmpleado;
import com.palmera_junior.gestion_compras.rrhh.repository.AfpRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.ArlRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.AreaRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.CajaCompensacionRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.CargoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EntidadBancariaRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EpsRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.EmpleadoAdjuntoRequeridoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.NivelEducativoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoContratoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoAdjuntoEmpleadoRepository;
import com.palmera_junior.gestion_compras.rrhh.repository.TipoPagoRepository;
import com.palmera_junior.gestion_compras.rrhh.web.EmpleadoRegistroForm;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmpleadoRegistroWorkflowService {

    private static final long MAX_TAMANO_ADJUNTO = 10L * 1024 * 1024;
    private static final long MAX_TAMANO_TOTAL_ADJUNTOS = 50L * 1024 * 1024;
    private static final Set<String> GRUPOS_INICIALES = Set.of("INGRESO", "VEHICULO");
    private static final Set<String> GRUPOS_CONTRATACION = Set.of("AFILIACIONES", "EVALUACIONES");

    private final IEmpleadoService empleadoService;
    private final IEmpleadoAdjuntoService adjuntoService;
    private final EmpleadoRepository empleadoRepository;
    private final EmpleadoAdjuntoRepository adjuntoRepository;
    private final EmpleadoAdjuntoRequeridoRepository requeridoRepository;
    private final TipoAdjuntoEmpleadoRepository tipoAdjuntoRepository;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final CentroCostoRepository centroCostoRepository;
    private final NivelEducativoRepository nivelEducativoRepository;
    private final CorreoEmpleadoOutboxService correoOutboxService;
    private final TipoContratoRepository tipoContratoRepository;
    private final TipoPagoRepository tipoPagoRepository;
    private final EntidadBancariaRepository entidadBancariaRepository;
    private final EpsRepository epsRepository;
    private final AfpRepository afpRepository;
    private final ArlRepository arlRepository;
    private final CajaCompensacionRepository cajaCompensacionRepository;

    @Transactional
    public Empleado guardar(
            EmpleadoRegistroForm form,
            MultiValueMap<String, MultipartFile> archivos,
            Usuario actor) {
        if (form == null) {
            throw new IllegalArgumentException("No se recibieron datos del empleado");
        }
        validarTamanoTotalAdjuntos(archivos);
        boolean nuevo = form.getIdEmpleado() == null;
        if (!nuevo) {
            Empleado actual = empleadoRepository.findById(form.getIdEmpleado())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El empleado que intentas editar ya no existe"));
            if (form.getVersion() == null || !form.getVersion().equals(actual.getVersion())) {
                throw new IllegalStateException(
                        "El registro cambió desde que lo abriste. Recarga la página antes de guardar.");
            }
            if (actual.getEstado() == EstadoEmpleado.PROCESO_CONTRATACION) {
                return completarContratacion(form, archivos, actor, actual);
            }
            if (actual.getEstado() != EstadoEmpleado.REGISTRO_INICIAL) {
                throw new IllegalStateException(
                        "Solo se puede editar un registro inicial o completar una contratación pendiente");
            }
        }
        validarFormulario(form);
        Empleado empleado = mapearEmpleado(form);
        if (!nuevo) {
            validarDocumentoNoDuplicado(empleado, form.getIdEmpleado());
            empleado.setIdEmpleado(form.getIdEmpleado());
            empleado = empleadoService.editarRegistroInicial(form.getIdEmpleado(), empleado, actor);
        } else {
            validarDocumentoNoDuplicado(empleado, null);
            empleado = empleadoService.crearRegistroInicial(empleado, actor);
        }
        guardarRequisitos(
                empleado, form.getTiposAdjuntoRequeridos(), EtapaAdjuntoEmpleado.REGISTRO_INICIAL);
        guardarAdjuntos(
                empleado, archivos, form.getTiposAdjuntoRequeridos(),
                EtapaAdjuntoEmpleado.REGISTRO_INICIAL);
        if (nuevo) {
            correoOutboxService.registrarNotificacionesRegistroInicial(empleado);
        }
        return empleado;
    }

    private Empleado completarContratacion(
            EmpleadoRegistroForm form,
            MultiValueMap<String, MultipartFile> archivos,
            Usuario actor,
            Empleado empleado) {
        ContratoEmpleado contrato = mapearContrato(form);
        guardarRequisitos(
                empleado, form.getTiposAdjuntoRequeridos(), EtapaAdjuntoEmpleado.CONTRATACION);
        guardarAdjuntos(
                empleado, archivos, form.getTiposAdjuntoRequeridos(),
                EtapaAdjuntoEmpleado.CONTRATACION);
        return empleadoService.completarRegistro(empleado.getIdEmpleado(), contrato, actor);
    }

    private ContratoEmpleado mapearContrato(EmpleadoRegistroForm form) {
        ContratoEmpleado contrato = new ContratoEmpleado();
        contrato.setFechaIngreso(form.getFechaIngreso());
        contrato.setFechaFin(form.getFechaFin());
        contrato.setSalarioBasico(form.getSalarioBasico());
        contrato.setTipoContrato(buscarCatalogoOpcional(
                form.getTipoContratoId(), tipoContratoRepository, "tipo de contrato"));
        contrato.setTipoPago(buscarCatalogoOpcional(
                form.getTipoPagoId(), tipoPagoRepository, "tipo de pago"));
        contrato.setEntidadBancaria(buscarCatalogoOpcional(
                form.getEntidadBancariaId(), entidadBancariaRepository, "entidad bancaria"));
        contrato.setNumeroCuentaBancaria(normalizar(form.getNumeroCuentaBancaria()));
        contrato.setEps(buscarCatalogoOpcional(form.getEpsId(), epsRepository, "EPS"));
        contrato.setAfp(buscarCatalogoOpcional(form.getAfpId(), afpRepository, "AFP"));
        contrato.setArl(buscarCatalogoOpcional(form.getArlId(), arlRepository, "ARL"));
        contrato.setCajaCompensacion(buscarCatalogoOpcional(
                form.getCajaCompensacionId(), cajaCompensacionRepository, "caja de compensación"));
        contrato.setTarifaArlPorcentaje(form.getTarifaArlPorcentaje());
        contrato.setAuxilioTransporteAplica(form.isAuxilioTransporteAplica());
        contrato.setAuxilioTransporteValor(valorAplicable(
                "auxilio de transporte", form.isAuxilioTransporteAplica(),
                form.getAuxilioTransporteValor()));
        contrato.setAuxilioRodamientoAplica(form.isAuxilioRodamientoAplica());
        contrato.setAuxilioRodamientoValor(valorAplicable(
                "auxilio de rodamiento", form.isAuxilioRodamientoAplica(),
                form.getAuxilioRodamientoValor()));
        contrato.setBonificacionNoSalarialAplica(form.isBonificacionNoSalarialAplica());
        contrato.setBonificacionNoSalarialValor(valorAplicable(
                "bonificación no salarial", form.isBonificacionNoSalarialAplica(),
                form.getBonificacionNoSalarialValor()));
        contrato.setCondicionesEspecialesAplica(form.isCondicionesEspecialesAplica());
        contrato.setCondicionesEspeciales(form.isCondicionesEspecialesAplica()
                ? normalizar(form.getCondicionesEspeciales()) : null);
        if (form.getTarifaArlPorcentaje() != null
                && (form.getTarifaArlPorcentaje().signum() < 0
                        || form.getTarifaArlPorcentaje().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new IllegalArgumentException("La tarifa ARL debe estar entre 0 y 100");
        }
        if (form.isCondicionesEspecialesAplica() && vacio(form.getCondicionesEspeciales())) {
            throw new IllegalArgumentException(
                    "Describe las condiciones especiales del contrato");
        }
        return contrato;
    }

    private Empleado mapearEmpleado(EmpleadoRegistroForm form) {
        Empleado empleado = new Empleado();
        empleado.setTipoDocumento(form.getTipoDocumento());
        empleado.setNumeroDocumento(normalizar(form.getNumeroDocumento()));
        empleado.setFechaExpedicion(form.getFechaExpedicion());
        empleado.setLugarExpedicion(normalizar(form.getLugarExpedicion()));
        empleado.setPrimerNombre(normalizar(form.getPrimerNombre()));
        empleado.setSegundoNombre(normalizar(form.getSegundoNombre()));
        empleado.setPrimerApellido(normalizar(form.getPrimerApellido()));
        empleado.setSegundoApellido(normalizar(form.getSegundoApellido()));
        empleado.setFechaNacimiento(form.getFechaNacimiento());
        empleado.setLugarNacimiento(normalizar(form.getLugarNacimiento()));
        empleado.setTipoSangre(form.getTipoSangre());
        empleado.setRh(form.getRh());
        empleado.setCelular(normalizar(form.getCelular()));
        empleado.setCorreoElectronico(normalizar(form.getCorreoElectronico()));
        empleado.setDireccionResidencia(normalizar(form.getDireccionResidencia()));
        empleado.setBarrio(normalizar(form.getBarrio()));
        empleado.setMunicipio(normalizar(form.getMunicipio()));
        empleado.setDepartamento(normalizar(form.getDepartamento()));
        empleado.setProfesion(normalizar(form.getProfesion()));
        empleado.setSubcentro(normalizar(form.getSubcentro()));
        empleado.setArea(buscarCatalogo(areaRepository.findById(form.getAreaId()), "área"));
        empleado.setCargo(buscarCatalogo(cargoRepository.findById(form.getCargoId()), "cargo"));
        empleado.setCentroCosto(buscarCatalogo(
                centroCostoRepository.findById(form.getCentroCostoId()), "centro de costo"));
        if (form.getNivelEducativoId() != null) {
            empleado.setNivelEducativo(buscarCatalogo(
                    nivelEducativoRepository.findById(form.getNivelEducativoId()), "nivel educativo"));
        }
        return empleado;
    }

    private void guardarRequisitos(
            Empleado empleado, List<Integer> idsTipos, EtapaAdjuntoEmpleado etapa) {
        List<Integer> seleccionados = idsTipos == null ? List.of() : idsTipos.stream().distinct().toList();
        List<TipoAdjuntoEmpleado> tipos = seleccionados.stream()
                .map(id -> tipoAdjuntoRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "El tipo de adjunto seleccionado no existe")))
                .toList();
        Set<String> gruposPermitidos = etapa == EtapaAdjuntoEmpleado.REGISTRO_INICIAL
                ? GRUPOS_INICIALES : GRUPOS_CONTRATACION;
        if (tipos.stream().anyMatch(tipo -> !tipo.isActivo()
                || !gruposPermitidos.contains(tipo.getGrupo().name()))) {
            throw new IllegalArgumentException(
                    etapa == EtapaAdjuntoEmpleado.REGISTRO_INICIAL
                            ? "Solo se pueden configurar como iniciales adjuntos de ingreso o vehículo"
                            : "Solo se pueden configurar para contratación adjuntos de afiliaciones o evaluaciones");
        }

        requeridoRepository.deleteAllByEmpleado_IdEmpleadoAndEtapaRequerida(
                empleado.getIdEmpleado(), etapa);
        for (TipoAdjuntoEmpleado tipo : tipos) {
            EmpleadoAdjuntoRequerido requerido = new EmpleadoAdjuntoRequerido();
            EmpleadoAdjuntoRequeridoId id = new EmpleadoAdjuntoRequeridoId();
            id.setIdEmpleado(empleado.getIdEmpleado());
            id.setIdTipoAdjuntoEmpleado(tipo.getIdTipoAdjuntoEmpleado());
            requerido.setId(id);
            requerido.setEmpleado(empleado);
            requerido.setTipoAdjunto(tipo);
            requerido.setEtapaRequerida(etapa);
            adjuntoService.guardarRequisito(requerido);
        }
    }

    private void guardarAdjuntos(
            Empleado empleado,
            MultiValueMap<String, MultipartFile> archivos,
            List<Integer> tiposRequeridos,
            EtapaAdjuntoEmpleado etapa) {
        if (archivos == null || archivos.isEmpty()) {
            return;
        }
        Set<Integer> idsTiposRequeridos = tiposRequeridos == null
                ? Set.of() : new HashSet<>(tiposRequeridos);
        Set<String> gruposPermitidos = etapa == EtapaAdjuntoEmpleado.REGISTRO_INICIAL
                ? GRUPOS_INICIALES : GRUPOS_CONTRATACION;
        for (Map.Entry<String, List<MultipartFile>> entry : archivos.entrySet()) {
            String nombreCampo = entry.getKey();
            if (!nombreCampo.startsWith("archivo-")) {
                continue;
            }
            Integer idTipo = parsearIdTipo(nombreCampo.substring("archivo-".length()));
            TipoAdjuntoEmpleado tipo = tipoAdjuntoRepository.findById(idTipo)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "El tipo de adjunto seleccionado no existe"));
            if (!tipo.isActivo() || !gruposPermitidos.contains(tipo.getGrupo().name())) {
                throw new IllegalArgumentException(
                        "El tipo de adjunto no corresponde a la etapa actual del registro");
            }
            List<MultipartFile> archivosTipo = entry.getValue().stream()
                    .filter(archivo -> archivo != null && !archivo.isEmpty())
                    .toList();
            if (!archivosTipo.isEmpty() && !idsTiposRequeridos.contains(idTipo)) {
                throw new IllegalArgumentException(
                        "Selecciona el concepto " + tipo.getDescripcion()
                                + " en Configuración antes de adjuntar archivos");
            }
            if (!tipo.isPermiteMultiplesVigentes() && archivosTipo.size() > 1) {
                throw new IllegalArgumentException(
                        "Solo se permite un archivo vigente para " + tipo.getDescripcion());
            }
            if (!tipo.isPermiteMultiplesVigentes() && !archivosTipo.isEmpty()) {
                List<EmpleadoAdjunto> anteriores = adjuntoRepository
                        .findAllByEmpleado_IdEmpleadoAndTipoAdjunto_IdTipoAdjuntoEmpleadoAndVigenteTrueOrderByFechaCargaDesc(
                                empleado.getIdEmpleado(), idTipo);
                anteriores.forEach(adjunto -> adjunto.setVigente(false));
                adjuntoRepository.saveAll(anteriores);
            }
            for (MultipartFile archivo : archivosTipo) {
                guardarAdjunto(empleado, tipo, archivo);
            }
        }
    }

    private void guardarAdjunto(
            Empleado empleado, TipoAdjuntoEmpleado tipo, MultipartFile archivo) {
        if (archivo.getSize() > MAX_TAMANO_ADJUNTO) {
            throw new IllegalArgumentException(
                    "El archivo " + archivo.getOriginalFilename() + " supera el límite de 10 MB");
        }
        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer el archivo adjunto", ex);
        }
        if (contenido.length < 5
                || contenido[0] != '%' || contenido[1] != 'P'
                || contenido[2] != 'D' || contenido[3] != 'F' || contenido[4] != '-') {
            throw new IllegalArgumentException(
                    "El archivo " + archivo.getOriginalFilename() + " no es un PDF válido");
        }
        String nombre = nombreSeguro(archivo.getOriginalFilename());
        EmpleadoAdjunto adjunto = new EmpleadoAdjunto();
        adjunto.setEmpleado(empleado);
        adjunto.setTipoAdjunto(tipo);
        adjunto.setNombreOriginal(nombre);
        adjunto.setTipoMime("application/pdf");
        adjunto.setTamanoBytes((long) contenido.length);
        adjunto.setContenido(contenido);
        adjunto.setFechaCarga(OffsetDateTime.now());
        adjunto.setVigente(true);
        adjunto.setPublicoQr(false);
        adjuntoService.guardar(adjunto);
    }

    private void validarTamanoTotalAdjuntos(MultiValueMap<String, MultipartFile> archivos) {
        if (archivos == null) {
            return;
        }
        long total = archivos.values().stream()
                .flatMap(List::stream)
                .filter(archivo -> archivo != null && !archivo.isEmpty())
                .mapToLong(MultipartFile::getSize)
                .sum();
        if (total > MAX_TAMANO_TOTAL_ADJUNTOS) {
            throw new IllegalArgumentException(
                    "El total de archivos adjuntos no puede superar 50 MB");
        }
    }

    private void validarFormulario(EmpleadoRegistroForm form) {
        if (form == null) {
            throw new IllegalArgumentException("No se recibieron datos del empleado");
        }
        if (form.getTipoDocumento() == null
                || vacio(form.getNumeroDocumento())
                || vacio(form.getPrimerNombre())
                || vacio(form.getPrimerApellido())
                || form.getAreaId() == null
                || form.getCargoId() == null
                || form.getCentroCostoId() == null) {
            throw new IllegalArgumentException(
                    "Completa tipo y número de documento, nombres, apellidos, área, cargo y centro de costo");
        }
    }

    private void validarDocumentoNoDuplicado(Empleado empleado, Integer idActual) {
        Empleado existente = empleadoRepository.findByTipoDocumentoAndNumeroDocumento(
                empleado.getTipoDocumento(), empleado.getNumeroDocumento()).orElse(null);
        if (existente != null && !existente.getIdEmpleado().equals(idActual)) {
            throw new IllegalArgumentException("Ya existe un empleado con ese documento");
        }
    }

    private static Integer parsearIdTipo(String valor) {
        try {
            return Integer.valueOf(valor);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Identificador de adjunto inválido", ex);
        }
    }

    private static String nombreSeguro(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            throw new IllegalArgumentException("El archivo debe tener un nombre válido");
        }
        String nombre = nombreOriginal.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).trim();
        if (nombre.isEmpty() || nombre.length() > 255) {
            throw new IllegalArgumentException("El nombre del archivo no es válido");
        }
        return nombre;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static BigDecimal valorAplicable(
            String concepto, boolean aplica, BigDecimal valor) {
        if (!aplica) {
            return null;
        }
        if (valor == null || valor.signum() < 0) {
            throw new IllegalArgumentException(
                    "Ingresa un valor válido para " + concepto);
        }
        return valor;
    }

    private static <T extends CatalogoRrhh> T buscarCatalogoOpcional(
            Integer id, JpaRepository<T, Integer> repository, String etiqueta) {
        if (id == null) {
            return null;
        }
        T catalogo = repository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("El " + etiqueta + " seleccionado no existe"));
        if (!catalogo.isActivo()) {
            throw new IllegalArgumentException("El " + etiqueta + " seleccionado está inactivo");
        }
        return catalogo;
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static <T> T buscarCatalogo(java.util.Optional<T> resultado, String etiqueta) {
        return resultado.orElseThrow(() ->
                new IllegalArgumentException("El " + etiqueta + " seleccionado no existe"));
    }
}
